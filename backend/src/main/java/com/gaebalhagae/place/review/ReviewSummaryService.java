package com.gaebalhagae.place.review;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaebalhagae.global.response.BusinessException;
import com.gaebalhagae.global.response.ErrorCode;
import com.gaebalhagae.llm.GeminiClient;
import com.gaebalhagae.llm.GroqClient;
import com.gaebalhagae.llm.LlmClient;
import com.gaebalhagae.place.collect.BlogPost;
import com.gaebalhagae.place.collect.BlogPostStore;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 수집한 블로그 문장으로 3줄 요약, 조건 9개 판정, 키워드를 만듭니다. (결정 8·30·31, 기능별_구현방법 5-3)
 * 장소당 LLM 1회 호출, Groq 실패 시 Gemini로 한 번 더.
 * 키워드는 LLM이 자유롭게 만들지 않고, 판정된 조건 값을 ConditionKey 의 고정 태그로 바꿔 만듭니다.
 * 근거 문장 번호가 없거나 틀린 조건은 unknown, 그런 분위기 태그는 버립니다.
 */
@Slf4j
@Service
public class ReviewSummaryService {

    static final int MAX_SENTENCES = 60;     // Groq 분당 8,000토큰 안에 들도록 문장 수 제한
    static final int MAX_CHARS = 6000;
    static final int MAX_KEYWORDS = 5;

    static final int MAX_MOOD = 3;

    static final String SYSTEM_PROMPT = """
            너는 반려견 동반 장소 정보를 정리하는 도우미다.
            사용자가 주는 문장들은 한 장소에 대한 블로그 글에서 뽑았고, 앞에 [번호]가 붙어 있다.
            문장에 없는 내용은 절대 지어내지 않는다. 근거가 없으면 반드시 unknown 이다.
            조건 목록 (키(설명): 가능한 값):
            %s
            다음 JSON만 출력한다.
            {"summary": [반려견 보호자 입장의 요약 3줄, 원문을 그대로 옮기지 않고 새로 쓴다, 각 줄 60자 이내],
             "conditions": {"size": {"value": "medium", "weight_limit_kg": 15, "evidence": [근거 문장 번호들]}, ... 조건 목록의 모든 키},
             "mood": [{"tag": "#조용함"처럼 분위기를 나타내는 짧은 태그, "evidence": [근거 문장 번호들]}] (최대 3개)}
            weight_limit_kg 는 size 에서 몸무게 제한 숫자가 문장에 있을 때만 넣고 없으면 null.
            값이 unknown 이면 evidence 는 빈 배열.
            문장에 근거가 부족하면 summary 를 1~2줄로 줄인다.
            """.formatted(ConditionKey.promptLines());

    private final BlogPostStore blogPostStore;
    private final ReviewSummaryStore summaryStore;
    private final LlmClient primary;
    private final LlmClient fallback;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    public ReviewSummaryService(BlogPostStore blogPostStore, ReviewSummaryStore summaryStore,
                                GroqClient groq, GeminiClient gemini, Clock clock) {
        this(blogPostStore, summaryStore, (LlmClient) groq, (LlmClient) gemini, clock);   // Groq 메인, Gemini 대안
    }

    // 테스트에서 가짜 LLM을 넣기 위한 생성자
    ReviewSummaryService(BlogPostStore blogPostStore, ReviewSummaryStore summaryStore,
                         LlmClient primary, LlmClient fallback, Clock clock) {
        this.blogPostStore = blogPostStore;
        this.summaryStore = summaryStore;
        this.primary = primary;
        this.fallback = fallback;
        this.clock = clock;
    }

    /** LLM 응답 모양 */
    record LlmResult(List<String> summary, Map<String, Condition> conditions, List<Mood> mood) {
        record Condition(String value, @JsonProperty("weight_limit_kg") Integer weightLimitKg, List<Integer> evidence) {
        }

        record Mood(String tag, List<Integer> evidence) {
        }
    }

    public record SummaryResult(String placeName, String status, int sourceCount, ReviewSummary summary) {
    }

    /** force=false 이면 지난번 요약 이후 새 글이 없을 때 LLM을 부르지 않습니다. */
    public SummaryResult summarize(String placeName, boolean force) {
        List<BlogPost> posts = blogPostStore.findByPlace(placeName).stream()
                .filter(p -> !p.sponsored())                 // 협찬 글 제외
                .filter(p -> !p.petSentences().isEmpty())
                .toList();
        if (posts.isEmpty()) {
            return new SummaryResult(placeName, "NO_SOURCE", 0, null);
        }
        Optional<ReviewSummary> previous = summaryStore.findByPlace(placeName);
        // 조건 판정이 없는 예전 형식 요약은 새 글이 없어도 다시 만듦
        if (!force && previous.isPresent() && previous.get().conditions() != null
                && previous.get().sourceCount() == posts.size()) {
            return new SummaryResult(placeName, "SKIPPED_NO_NEW_POSTS", posts.size(), previous.get());
        }

        List<String> sentences = numberedSentences(posts);
        String userPrompt = "장소: " + placeName + "\n문장:\n" + String.join("\n", sentences);

        Called called = callWithFallback(userPrompt);
        Map<String, ConditionResult> conditions = validConditions(called.result(), sentences);
        List<String> mood = validMood(called.result(), sentences.size());
        ReviewSummary summary = new ReviewSummary(
                placeName,
                called.result().summary() == null ? List.of() : called.result().summary().stream().limit(3).toList(),
                keywords(conditions, mood),
                conditions,
                mood,
                posts.size(),
                called.model(),
                LocalDateTime.now(clock));
        summaryStore.save(summary);
        return new SummaryResult(placeName, "SUMMARIZED", posts.size(), summary);
    }

    public Optional<ReviewSummary> findByPlace(String placeName) {
        return summaryStore.findByPlace(placeName);
    }

    // [1] 문장, [2] 문장 ... 형태로 번호를 붙이고 중복 문장은 한 번만 넣음
    static List<String> numberedSentences(List<BlogPost> posts) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        posts.forEach(p -> unique.addAll(p.petSentences()));
        List<String> numbered = new ArrayList<>();
        int chars = 0;
        for (String s : unique) {
            if (numbered.size() >= MAX_SENTENCES || chars + s.length() > MAX_CHARS) {
                break;
            }
            numbered.add("[" + (numbered.size() + 1) + "] " + s);
            chars += s.length();
        }
        return numbered;
    }

    // 조건 9개를 모두 채움. 목록에 없는 값이거나 근거 번호가 틀리면 unknown (지어낸 판정 방지)
    static Map<String, ConditionResult> validConditions(LlmResult result, List<String> sentences) {
        Map<String, ConditionResult> out = new LinkedHashMap<>();
        for (ConditionKey key : ConditionKey.values()) {
            LlmResult.Condition c = result.conditions() == null ? null : result.conditions().get(key.key());
            if (c == null || c.value() == null || ConditionKey.UNKNOWN.equals(c.value())
                    || !key.accepts(c.value()) || !validEvidence(c.evidence(), sentences.size())) {
                out.put(key.key(), ConditionResult.unknown());
                continue;
            }
            Integer kg = key == ConditionKey.SIZE && c.weightLimitKg() != null
                    && c.weightLimitKg() > 0 && c.weightLimitKg() <= 100 ? c.weightLimitKg() : null;
            List<String> evidence = c.evidence().stream().distinct().map(n -> stripNumber(sentences.get(n - 1))).toList();
            out.put(key.key(), new ConditionResult(c.value(), kg, evidence));
        }
        return out;
    }

    // 분위기 태그: 근거가 있는 것만, # 붙이고 공백 제거, 최대 3개
    static List<String> validMood(LlmResult result, int sentenceCount) {
        if (result.mood() == null) {
            return List.of();
        }
        return result.mood().stream()
                .filter(m -> m.tag() != null && !m.tag().isBlank())
                .filter(m -> validEvidence(m.evidence(), sentenceCount))
                .map(m -> "#" + m.tag().replace("#", "").replace(" ", ""))
                .distinct()
                .limit(MAX_MOOD)
                .toList();
    }

    // 조건 고정 태그(ConditionKey 순서)를 먼저, 남는 자리에 분위기 태그. 최대 5개
    static List<String> keywords(Map<String, ConditionResult> conditions, List<String> mood) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        for (ConditionKey key : ConditionKey.values()) {
            key.tagOf(conditions.get(key.key()).value()).ifPresent(tags::add);
        }
        tags.addAll(mood);
        return tags.stream().limit(MAX_KEYWORDS).toList();
    }

    private static boolean validEvidence(List<Integer> evidence, int sentenceCount) {
        return evidence != null && !evidence.isEmpty()
                && evidence.stream().allMatch(n -> n != null && n >= 1 && n <= sentenceCount);
    }

    // "[3] 문장" → "문장"
    private static String stripNumber(String numbered) {
        return numbered.replaceFirst("^\\[\\d+]\\s*", "");
    }

    private record Called(LlmResult result, String model) {
    }

    // 메인 모델 1회 + JSON 오류 시 1회 재요청, 그래도 실패하면 대안 모델 1회
    private Called callWithFallback(String userPrompt) {
        for (LlmClient client : List.of(primary, primary, fallback)) {
            try {
                String json = client.completeJson(SYSTEM_PROMPT, userPrompt);
                return new Called(objectMapper.readValue(json, LlmResult.class), client.name());
            } catch (Exception e) {
                log.warn("LLM 요약 실패 ({}): {}", client.name(), e.getMessage());
            }
        }
        throw new BusinessException(ErrorCode.LLM_UNAVAILABLE);
    }
}
