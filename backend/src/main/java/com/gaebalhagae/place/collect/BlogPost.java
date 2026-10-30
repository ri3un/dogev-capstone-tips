package com.gaebalhagae.place.collect;

import java.time.LocalDate;
import java.util.List;

/**
 * 블로그 검색 결과 한 건. 나중에 place_source 테이블의 한 행이 됩니다.
 * description은 검색 API가 주는 2~3문장 요약문이고, 본문 전체는 받지 않습니다(결정 30).
 */
public record BlogPost(
        String title,
        String description,
        String url,
        String bloggerName,
        LocalDate postDate,
        boolean sponsored,          // 협찬·체험단 표현이 있으면 true (LLM 분석에서 제외)
        List<String> petSentences   // 반려견 키워드가 들어간 문장만 (LLM에 넘길 재료)
) {
}
