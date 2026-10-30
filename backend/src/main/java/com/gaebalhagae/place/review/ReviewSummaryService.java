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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 수집한 블로그 문장으로 3줄 요약과 키워드를 만듭니다. (결정 30·31, 기능별_구현방법 5-3)
 * 장소당 LLM 1회 호출, Groq 실패 시 Gemini로 한 번 더. 근거 문장 번호가 없는 키워드는 버립니다.
 */
@Slf4j
@Service
public class ReviewSummaryService {

    static final int MAX_SENTENCES = 60;     // Groq 분당 8,000토큰 안에 들도록 문장 수 제한
    static final int MAX_CHARS = 6000;
    static final int MAX_KEYWORDS = 5;

    static final String SYSTEM_PROMPT = """
            너는 반려견 동반 장소 정보를 정리하는 도우미다.
            사용자가 주는 문장들은 한 장소에 대한 블로그 글에서 뽑았다. 문장에 없는 내용은 절대 지어내지 않는다.
            다음 JSON만 출력한다.
            {"summary": [반려견 보호자 입장의 요약 3줄, 원문을 그대로 옮기지 않고 새로 쓴다, 각 줄 60자 이내],
             "keywords": [{"tag": "#대형견가능"처럼 #으로 시작하는 짧은 키워드, "evidence": [근거 문장 번호들]}] (최대 5개)}
            문장에 근거가 부족하면 summary를 1~2줄로 줄이고 keywords는 빈 배열로 둔다.
            """;

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
    record LlmResult(List<String> summary, List<Keyword> keywords) {
        record Keyword(String tag, List<Integer> evidence) {
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
        if (!force && previous.isPresent() && previous.get().sourceCount() == posts.size()) {
            return new SummaryResult(placeName, "SKIPPED_NO_NEW_POSTS", posts.size(), previous.get());
        }

        List<String> sentences = numberedSentences(posts);
        String userPrompt = "장소: " + placeName + "\n문장:\n" + String.join("\n", sentences);

        Called called = callWithFallback(userPrompt);
        ReviewSummary summary = new ReviewSummary(
                placeName,
                called.result().summary() == null ? List.of() : called.result().summary().stream().limit(3).toList(),
                validKeywords(called.result(), sentences.size()),
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

    // 근거 번호가 비었거나 없는 문장을 가리키는 키워드는 버림 (지어낸 키워드 방지)
    static List<String> validKeywords(LlmResult result, int sentenceCount) {
        if (result.keywords() == null) {
            return List.of();
        }
        return result.keywords().stream()
                .filter(k -> k.tag() != null && !k.tag().isBlank())
                .filter(k -> k.evidence() != null && !k.evidence().isEmpty())
                .filter(k -> k.evidence().stream().allMatch(n -> n != null && n >= 1 && n <= sentenceCount))
                .map(k -> k.tag().startsWith("#") ? k.tag().replace(" ", "") : "#" + k.tag().replace(" ", ""))
                .distinct()
                .limit(MAX_KEYWORDS)
                .toList();
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
