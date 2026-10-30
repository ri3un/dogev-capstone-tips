package com.gaebalhagae.place.review;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 장소 하나의 LLM 리뷰 요약. 나중에 place 테이블의 review_* 컬럼 4개가 됩니다. (결정 30)
 */
public record ReviewSummary(
        String placeName,
        List<String> summary,       // review_summary: 3줄 요약
        List<String> keywords,      // review_keywords: 근거가 있는 키워드 최대 5개
        int sourceCount,            // review_source_count: 요약에 쓴 글 수
        String model,               // 어떤 모델이 만들었는지
        LocalDateTime summarizedAt  // review_summarized_at
) {
}
