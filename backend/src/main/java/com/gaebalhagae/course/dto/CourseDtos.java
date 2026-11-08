package com.gaebalhagae.course.dto;

import com.gaebalhagae.place.domain.PlaceCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class CourseDtos {

    private CourseDtos() {
    }

    /** 출발 위치는 계산에만 쓰고 저장하지 않습니다. placeTypes 는 들를 순서 (예: CAFE, PARK) */
    public record Recommend(@NotNull Long dogId,
                            @NotNull @DecimalMin("33") @DecimalMax("39") Double lat,
                            @NotNull @DecimalMin("124") @DecimalMax("132") Double lng,
                            @NotEmpty @Size(max = 5) List<@NotNull PlaceCategory> placeTypes,
                            @DecimalMin("0.3") @DecimalMax("10") Double radiusKm) {
    }

    public record Response(Long id, Long dogId, List<Stop> stops, List<Leg> legs, int startToFirstM,
                           int totalDistanceM, int totalMinutes, List<String> reasons, LocalDateTime visitedAt,
                           Long createdBy, LocalDateTime createdAt) {
    }

    public record Stop(int seq, Long placeId, String name, PlaceCategory category, String categoryLabel, double lat,
                       double lng, List<String> keywords, int legDistanceM, int legMinutes) {
    }

    /** from 0 = 출발점 */
    public record Leg(int from, int to, int distanceM, int minutes) {
    }
}
