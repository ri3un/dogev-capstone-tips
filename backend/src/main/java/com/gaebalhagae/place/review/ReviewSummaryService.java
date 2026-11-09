package com.gaebalhagae.place.review;

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
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 수집한 블로그 문장으로 3줄 요약, 조건 9개 판정, 키워드를 만듭니다. (결정 8·30·31·33, 기능별_구현방법 5-3)
 * 장소당 LLM 1회 호출로 문장별 판정(judgements)과 요약을 받고, 서버가 조건마다 다수결로 합칩니다.
 * 키워드는 LLM이 짓지 않고, 다수결로 정해진 조건 값을 ConditionKey 의 고정 태그로 바꿔 만듭니다.
 */
@Slf4j
@Service
public class ReviewSummaryService {

    static final int MAX_SENTENCES = 60;     // Groq 분당 8,000토큰 안에 들도록 문장 수 제한
    static final int MAX_CHARS = 6000;
    static final int MAX_KEYWORDS = 5;
    static final int MAX_MOOD = 3;
    static final double MAJORITY = 0.6;      // 같은 값이 60% 이상이어야 확정, 아니면 NEEDS_CHECK

    static final String SYSTEM_PROMPT = """
            너는 반려견 동반 장소 정보를 정리하는 도우미다.
            사용자가 주는 문장들은 한 장소에 대한 블로그 글에서 뽑았고, 앞에 [번호]가 붙어 있다.
            문장 번호별로 아래 조건을 판정한다. 문장에 근거가 없으면 반드시 unknown 이다. 추측하지 않는다.
            조건 (키(설명): 가능한 값):
            %s
            mood_tags(분위기): "조용함", "넓음"처럼 짧은 태그 최대 3개
            다음 JSON만 출력한다.
            {"judgements": [{"sentence": 문장 번호, "size_allowed": "up_to_medium", "weight_limit_kg": 15, "indoor": "indoor", "mood_tags": ["조용함"]}],
             "summary": [반려견 보호자 입장의 요약 3줄, 원문을 그대로 옮기지 않고 새로 쓴다, 각 줄 60자 이내]}
            judgements 에는 판정할 내용이 하나라도 있는 문장만 넣고, unknown 인 조건 키는 생략한다.
            weight_limit_kg 는 문장에 몸무게 제한 숫자가 있을 때만 넣는다.
            문장에 근거가 부족하면 summary 를 1~2줄로 줄인다.
            """.formatted(ConditionKey.promptLines());

    private final BlogPostStore blogPostStore;
    private final ReviewSummaryStore summaryStore;
    private final LlmClient primary;
    private final LlmClient fallback;
    private final Clock clock;
    private final int minSources;   // 요약에 필요한 최소 글 수 (협찬 아님 + 반려견 문장 있음)
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    public ReviewSummaryService(BlogPostStore blogPostStore, ReviewSummaryStore summaryStore,
                                GroqClient groq, GeminiClient gemini, Clock clock,
                                @Value("${collect.min-sources:10}") int minSources) {
        this(blogPostStore, summaryStore, (LlmClient) groq, (LlmClient) gemini, clock, minSources);   // Groq 메인, Gemini 대안
    }

    // 테스트에서 가짜 LLM을 넣기 위한 생성자
    ReviewSummaryService(BlogPostStore blogPostStore, ReviewSummaryStore summaryStore,
                         LlmClient primary, LlmClient fallback, Clock clock, int minSources) {
        this.blogPostStore = blogPostStore;
        this.summaryStore = summaryStore;
        this.primary = primary;
        this.fallback = fallback;
        this.clock = clock;
        this.minSources = minSources;
    }

    /**
     * 확인된 애견동반 장소: 요약에 쓴 글이 minSources 건 이상이고, 조건 고정 키워드(#목줄필수, #소형견만 …)가 하나 이상.
     * 카카오 검색 결과에는 일반 카페도 섞이므로 이것을 통과한 곳만 DB 에 넣습니다.
     */
    public boolean isConfirmed(ReviewSummary s) {
        if (s == null || s.sourceCount() < minSources || s.conditions() == null) {
            return false;
        }
        return Arrays.stream(ConditionKey.values()).anyMatch(k -> {
            ConditionResult r = s.conditions().get(k.key());
            return r != null && r.value() != null && k.tagOf(r.value()).isPresent();
        });
    }

    /** LLM 응답 모양. 판정 한 건은 조건 키가 문장마다 달라서 Map 으로 받습니다. */
    record LlmResult(List<Map<String, Object>> judgements, List<String> summary) {
    }

    /** 검증을 통과한 문장 한 개의 판정 */
    record Judgement(int sentence, Map<String, String> values, Integer weightLimitKg, List<String> moodTags) {
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
        if (posts.size() < minSources) {   // 글이 적으면 한 줄짜리·키워드 없는 요약이 나와서 LLM 을 부르지 않음
            return new SummaryResult(placeName, "NOT_ENOUGH_SOURCE", posts.size(), null);
        }
        Optional<ReviewSummary> previous = summaryStore.findByPlace(placeName);
        if (!force && previous.isPresent() && isCurrentFormat(previous.get())
                && previous.get().sourceCount() == posts.size()) {
            return new SummaryResult(placeName, "SKIPPED_NO_NEW_POSTS", posts.size(), previous.get());
        }

        List<String> sentences = numberedSentences(posts);
        String userPrompt = "장소: " + placeName + "\n문장:\n" + String.join("\n", sentences);

        Called called = callWithFallback(userPrompt);
        List<Judgement> judgements = validJudgements(called.result(), sentences.size());
        Map<String, ConditionResult> conditions = vote(judgements, sentences);
        List<String> mood = topMood(judgements);
        ReviewSummary summary = new ReviewSummary(
                placeName,
                called.result().summary() == null ? List.of() : called.result().summary().stream().limit(3).toList(),
                keywords(conditions, mood),
                conditions,
                mood,
                posts.size(),
                posts.stream().map(BlogPost::url).toList(),
                called.model(),
                LocalDateTime.now(clock));
        summaryStore.save(summary);
        return new SummaryResult(placeName, "SUMMARIZED", posts.size(), summary);
    }

    public Optional<ReviewSummary> findByPlace(String placeName) {
        return summaryStore.findByPlace(placeName);
    }

    // 조건 판정이 없거나 예전 키 이름으로 만든 요약은 새 글이 없어도 다시 만듦
    private static boolean isCurrentFormat(ReviewSummary s) {
        return s.conditions() != null && s.conditions().containsKey(ConditionKey.SIZE_ALLOWED.key());
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

    // 없는 문장 번호를 가리키는 판정은 버리고, 목록에 없는 값과 unknown 은 빼서 남김 (지어낸 판정 방지)
    static List<Judgement> validJudgements(LlmResult result, int sentenceCount) {
        if (result.judgements() == null) {
            return List.of();
        }
        List<Judgement> out = new ArrayList<>();
        for (Map<String, Object> j : result.judgements()) {
            if (!(j.get("sentence") instanceof Number n) || n.intValue() < 1 || n.intValue() > sentenceCount) {
                continue;
            }
            Map<String, String> values = new LinkedHashMap<>();
            for (ConditionKey key : ConditionKey.values()) {
                if (j.get(key.key()) instanceof String v && key.isKnownValue(v)) {
                    values.put(key.key(), v);
                }
            }
            Integer kg = j.get("weight_limit_kg") instanceof Number w && w.intValue() > 0 && w.intValue() <= 100
                    ? w.intValue() : null;
            List<String> mood = j.get("mood_tags") instanceof List<?> tags
                    ? tags.stream().filter(String.class::isInstance).map(String.class::cast)
                        .map(t -> "#" + t.replace("#", "").replace(" ", "")).filter(t -> t.length() > 1).toList()
                    : List.of();
            out.add(new Judgement(n.intValue(), values, kg, mood));
        }
        return out;
    }

    // 조건마다 unknown 이 아닌 판정만 세서 다수결. 60% 이상이면 그 값, 아니면 NEEDS_CHECK
    static Map<String, ConditionResult> vote(List<Judgement> judgements, List<String> sentences) {
        Map<String, ConditionResult> out = new LinkedHashMap<>();
        for (ConditionKey key : ConditionKey.values()) {
            List<Judgement> voted = judgements.stream().filter(j -> j.values().containsKey(key.key())).toList();
            if (voted.isEmpty()) {
                out.put(key.key(), ConditionResult.unknown());
                continue;
            }
            Map<String, Long> votes = voted.stream()
                    .collect(Collectors.groupingBy(j -> j.values().get(key.key()), LinkedHashMap::new, Collectors.counting()));
            Map.Entry<String, Long> top = votes.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow();
            int total = voted.size();
            int positive = top.getValue().intValue();
            String value = positive >= total * MAJORITY ? top.getKey() : ConditionKey.NEEDS_CHECK;
            List<Judgement> support = voted.stream().filter(j -> j.values().get(key.key()).equals(top.getKey())).toList();
            // 몸무게 제한은 이긴 값의 판정 중 가장 작은 숫자 (보수적으로)
            Integer kg = key == ConditionKey.SIZE_ALLOWED
                    ? support.stream().map(Judgement::weightLimitKg).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null)
                    : null;
            List<String> evidence = support.stream().map(j -> stripNumber(sentences.get(j.sentence() - 1))).distinct().toList();
            out.put(key.key(), new ConditionResult(value, kg, total, positive, evidence));
        }
        return out;
    }

    // 분위기 태그는 여러 문장에서 많이 나온 순서로 최대 3개
    static List<String> topMood(List<Judgement> judgements) {
        Map<String, Long> counts = judgements.stream().flatMap(j -> j.moodTags().stream().distinct())
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(MAX_MOOD)
                .map(Map.Entry::getKey)
                .toList();
    }

    // 확정된 조건의 고정 태그(ConditionKey 순서)를 먼저, 남는 자리에 분위기 태그. 최대 5개
    static List<String> keywords(Map<String, ConditionResult> conditions, List<String> mood) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        for (ConditionKey key : ConditionKey.values()) {
            key.tagOf(conditions.get(key.key()).value()).ifPresent(tags::add);
        }
        tags.addAll(mood);
        return tags.stream().limit(MAX_KEYWORDS).toList();
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
