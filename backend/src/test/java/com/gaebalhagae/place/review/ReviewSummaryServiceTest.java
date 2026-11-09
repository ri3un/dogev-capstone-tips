package com.gaebalhagae.place.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gaebalhagae.global.response.BusinessException;
import com.gaebalhagae.llm.LlmClient;
import com.gaebalhagae.place.collect.BlogPost;
import com.gaebalhagae.place.collect.BlogPostStore;
import com.gaebalhagae.place.collect.JsonFileBlogPostStore;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReviewSummaryServiceTest {

    @TempDir
    Path tempDir;

    private BlogPostStore blogStore;
    private ReviewSummaryStore summaryStore;
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-30T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    /** 미리 정한 응답을 차례로 돌려주는 가짜 LLM. "ERROR"면 예외를 던짐 */
    static class FakeLlm implements LlmClient {
        final Deque<String> answers = new ArrayDeque<>();
        final String name;
        int calls;

        FakeLlm(String name, String... answers) {
            this.name = name;
            this.answers.addAll(List.of(answers));
        }

        @Override
        public String completeJson(String system, String user) {
            calls++;
            String a = answers.isEmpty() ? "ERROR" : answers.poll();
            if (a.equals("ERROR")) {
                throw new IllegalStateException("429 Too Many Requests");
            }
            return a;
        }

        @Override
        public String name() {
            return name;
        }
    }

    // 문장 [1] 중형견 실내, [2] 주차장 넓음, [3] 대형견 안 됨(중형까지), [4] 주차 불가
    private static final String GOOD = """
            {"judgements":[
               {"sentence":1,"size_allowed":"up_to_medium","weight_limit_kg":15,"indoor":"indoor","mood_tags":["조용함"]},
               {"sentence":2,"parking":"yes","mood_tags":["넓음","#조용함"]},
               {"sentence":3,"size_allowed":"up_to_medium","weight_limit_kg":10,"leash_required":"지어낸값"},
               {"sentence":4,"parking":"no"},
               {"sentence":99,"seat_allowed":"yes"}],
             "summary":["중형견까지 실내 동반이 가능해요.","주차는 후기가 엇갈려요.","조용한 편이에요."]}
            """;

    @BeforeEach
    void setUp() {
        ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());
        blogStore = new JsonFileBlogPostStore(om, tempDir.resolve("blog").toString());
        summaryStore = new JsonFileReviewSummaryStore(om, tempDir.resolve("summary").toString());
        blogStore.saveAll("공원A", List.of(
                post("u1", false, "중형견 실내 동반 가능해요."),
                post("u2", false, "주차장이 넓어 강아지랑 가기 편해요."),
                post("u4", false, "대형견은 안 되고 10kg 이하 중형견까지 돼요."),
                post("u5", false, "강아지 데리고 갔는데 주차는 안 돼요."),
                post("u3", true, "협찬 글: 대형견도 됩니다.")));
    }

    private static BlogPost post(String url, boolean sponsored, String sentence) {
        return new BlogPost("t", sentence, url, "b", LocalDate.of(2026, 10, 1), sponsored, List.of(sentence));
    }

    // 기존 판정 테스트는 글 4건으로 보므로 최소 글 수 1, 10건 규칙은 아래 테스트에서 따로
    private ReviewSummaryService service(LlmClient primary, LlmClient fallback) {
        return new ReviewSummaryService(blogStore, summaryStore, primary, fallback, clock, 1);
    }

    @Test
    void 쓸_수_있는_글이_10건_미만이면_LLM을_부르지_않는다() {
        FakeLlm groq = new FakeLlm("groq", GOOD);
        var result = new ReviewSummaryService(blogStore, summaryStore, groq, new FakeLlm("gemini"), clock, 10)
                .summarize("공원A", false);

        assertThat(result.status()).isEqualTo("NOT_ENOUGH_SOURCE");
        assertThat(result.sourceCount()).isEqualTo(4);
        assertThat(groq.calls).isZero();
        assertThat(summaryStore.findByPlace("공원A")).isEmpty();
    }

    @Test
    void 글_10건_이상이고_조건_키워드가_있어야_확인된_애견동반_장소다() {
        var svc = new ReviewSummaryService(blogStore, summaryStore, new FakeLlm("groq"), new FakeLlm("gemini"), clock, 10);
        var known = Map.of("leash_required", new ConditionResult("yes", null, 3, 3, List.of()));
        var allUnknown = Map.of("leash_required", ConditionResult.unknown());

        assertThat(svc.isConfirmed(summary(10, known))).isTrue();
        assertThat(svc.isConfirmed(summary(9, known))).isFalse();        // 글 부족
        assertThat(svc.isConfirmed(summary(12, allUnknown))).isFalse();  // 조건 키워드 없음
        assertThat(svc.isConfirmed(null)).isFalse();
        // 주차는 반려견 동반과 상관없어서 주차 키워드만으로는 확인 안 됨 (2026-11-09 사용자 결정)
        var parkingOnly = Map.of("parking", new ConditionResult("yes", null, 3, 3, List.of()),
                "leash_required", ConditionResult.unknown());
        assertThat(svc.isConfirmed(summary(15, parkingOnly))).isFalse();
    }

    private static ReviewSummary summary(int sources, Map<String, ConditionResult> conditions) {
        return new ReviewSummary("공원A", List.of("요약"), List.of(), conditions, List.of(), sources, List.of(), "groq",
                java.time.LocalDateTime.of(2026, 11, 9, 3, 0));
    }

    @Test
    void 문장별_판정을_다수결로_합치고_키워드는_고정_태그로_만든다() {
        var result = service(new FakeLlm("groq", GOOD), new FakeLlm("gemini")).summarize("공원A", false);

        assertThat(result.status()).isEqualTo("SUMMARIZED");
        assertThat(result.sourceCount()).isEqualTo(4);   // 협찬 글 제외
        assertThat(result.summary().sourceUrls()).containsExactly("u1", "u2", "u4", "u5");
        var c = result.summary().conditions();
        assertThat(c).hasSize(ConditionKey.values().length);

        var size = c.get("size_allowed");
        assertThat(size.value()).isEqualTo("up_to_medium");
        assertThat(size.evidenceCount()).isEqualTo(2);
        assertThat(size.positiveCount()).isEqualTo(2);
        assertThat(size.weightLimitKg()).isEqualTo(10);   // 더 작은 제한
        assertThat(size.evidence()).hasSize(2);

        assertThat(c.get("parking").value()).isEqualTo("NEEDS_CHECK");   // 1:1 로 엇갈림
        assertThat(c.get("parking").evidenceCount()).isEqualTo(2);
        assertThat(c.get("leash_required").value()).isEqualTo("unknown"); // 목록에 없는 값
        assertThat(c.get("seat_allowed").value()).isEqualTo("unknown");   // 없는 문장 번호

        assertThat(result.summary().mood()).containsExactly("#조용함", "#넓음");
        assertThat(result.summary().keywords()).containsExactly("#중형견까지", "#실내동반", "#조용함", "#넓음");
    }

    @Test
    void 문장_번호가_문자로_와도_판정을_버리지_않는다() {
        // Groq 이 "sentence":"1" 처럼 문자로 줄 때가 있음 (2026-11-09 실제 응답) → 예전엔 판정이 통째로 버려져 키워드 0개
        String stringNumbers = """
                {"judgements":[{"sentence":"1","indoor":"indoor"},{"sentence":"2","parking":"yes","mood_tags":["넓음"]}],
                 "summary":["실내 동반이 돼요."]}
                """;
        var result = service(new FakeLlm("groq", stringNumbers), new FakeLlm("gemini")).summarize("공원A", false);

        assertThat(result.summary().conditions().get("indoor").value()).isEqualTo("indoor");
        assertThat(result.summary().conditions().get("parking").value()).isEqualTo("yes");
        assertThat(result.summary().keywords()).containsExactly("#실내동반", "#주차가능", "#넓음");
    }

    @Test
    void 새_글이_없으면_LLM을_다시_부르지_않는다() {
        FakeLlm groq = new FakeLlm("groq", GOOD, GOOD);
        var svc = service(groq, new FakeLlm("gemini"));
        svc.summarize("공원A", false);

        var second = svc.summarize("공원A", false);

        assertThat(second.status()).isEqualTo("SKIPPED_NO_NEW_POSTS");
        assertThat(groq.calls).isEqualTo(1);
    }

    @Test
    void Groq가_두_번_실패하면_Gemini로_요약한다() {
        var result = service(new FakeLlm("groq", "ERROR", "{깨진 JSON"), new FakeLlm("gemini", GOOD))
                .summarize("공원A", false);

        assertThat(result.summary().model()).isEqualTo("gemini");
    }

    @Test
    void 모두_실패하면_LLM_UNAVAILABLE() {
        assertThatThrownBy(() -> service(new FakeLlm("groq"), new FakeLlm("gemini")).summarize("공원A", false))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 수집한_글이_없으면_NO_SOURCE() {
        var result = service(new FakeLlm("groq"), new FakeLlm("gemini")).summarize("없는장소", false);

        assertThat(result.status()).isEqualTo("NO_SOURCE");
    }
}
