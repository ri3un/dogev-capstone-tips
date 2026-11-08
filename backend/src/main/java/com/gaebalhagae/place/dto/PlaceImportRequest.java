package com.gaebalhagae.place.dto;

import com.gaebalhagae.place.collect.CollectedPlace;
import com.gaebalhagae.place.review.ReviewSummary;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 노트북 수집 서버(collect)가 만든 JSON: data/place 의 장소 + data/summary 의 요약 */
public record PlaceImportRequest(@NotNull @Size(max = 2000) List<CollectedPlace> places,
                                 @Size(max = 2000) List<ReviewSummary> summaries) {

    public record Result(int created, int updated, int summariesApplied, List<String> unmatchedSummaries) {
    }
}
