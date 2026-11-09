package com.gaebalhagae.place.review;

import java.util.Optional;

/** 요약 결과 저장소. 지금은 JSON 파일, DB가 생기면 place 테이블에 저장하는 구현체로 바꿉니다. */
public interface ReviewSummaryStore {

    void save(ReviewSummary summary);

    Optional<ReviewSummary> findByPlace(String placeName);
}
