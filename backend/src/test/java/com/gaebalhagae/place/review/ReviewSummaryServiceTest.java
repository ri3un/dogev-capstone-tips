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

    private static final String GOOD = """
            {"summary":["중형견까지 실내 동반이 가능해요.","목줄은 꼭 필요해요.","주말엔 붐벼요."],
             "keywords":[{"tag":"#실내동반","evidence":[1]},{"tag":"주차편함","evidence":[2]},
                         {"tag":"#지어냄","evidence":[]},{"tag":"#없는번호","evidence":[99]}]}
            """;

    @BeforeEach
    void setUp() {
        ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());
        blogStore = new JsonFileBlogPostStore(om, tempDir.resolve("blog").toString());
        summaryStore = new JsonFileReviewSummaryStore(om, tempDir.resolve("summary").toString());
        blogStore.saveAll("공원A", List.of(
                post("u1", false, "중형견 실내 동반 가능해요."),
                post("u2", false, "주차장이 넓어 강아지랑 가기 편해요."),
                post("u3", true, "협찬 글: 대형견도 됩니다.")));
    }

    private static BlogPost post(String url, boolean sponsored, String sentence) {
        return new BlogPost("t", sentence, url, "b", LocalDate.of(2026, 10, 1), sponsored, List.of(sentence));
    }

    private ReviewSummaryService service(LlmClient primary, LlmClient fallback) {
        return new ReviewSummaryService(blogStore, summaryStore, primary, fallback, clock);
    }

    @Test
    void 근거_없는_키워드는_버리고_협찬_글은_빼고_요약한다() {
        var result = service(new FakeLlm("groq", GOOD), new FakeLlm("gemini")).summarize("공원A", false);

        assertThat(result.status()).isEqualTo("SUMMARIZED");
        assertThat(result.sourceCount()).isEqualTo(2);
        assertThat(result.summary().summary()).hasSize(3);
        assertThat(result.summary().keywords()).containsExactly("#실내동반", "#주차편함");
        assertThat(summaryStore.findByPlace("공원A")).isPresent();
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
