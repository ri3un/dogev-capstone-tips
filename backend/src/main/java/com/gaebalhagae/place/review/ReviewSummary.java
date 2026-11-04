package com.gaebalhagae.place.review;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 장소 하나의 LLM 리뷰 요약과 조건 판정. (결정 8·30·33)
 * summary·keywords·sourceCount·summarizedAt 은 place 테이블의 review_* 컬럼,
 * conditions 는 place_condition 테이블(condition_key 별 한 행)이 됩니다.
 */
public record ReviewSummary(
        String placeName,
        List<String> summary,                    // review_summary: 3줄 요약
        List<String> keywords,                   // review_keywords: 조건 고정 키워드 + 분위기, 최대 5개
        Map<String, ConditionResult> conditions, // 조건 9개 (키: size_allowed, indoor ...)
        List<String> mood,                       // 분위기 자유 태그 최대 3개 (예: #조용함)
        int sourceCount,                         // review_source_count: 요약에 쓴 글 수
        List<String> sourceUrls,                 // 화면의 출처 링크 (협찬 글 제외)
        String model,                            // 어떤 모델이 만들었는지
        LocalDateTime summarizedAt               // review_summarized_at
) {
}
