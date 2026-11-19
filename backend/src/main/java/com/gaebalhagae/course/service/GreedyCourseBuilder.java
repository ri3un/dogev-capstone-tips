package com.gaebalhagae.course.service;

import com.gaebalhagae.global.util.Geo;
import com.gaebalhagae.place.domain.PlaceCategory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 반경 안 후보를 장소 유형 순서대로, 지금 위치에서 가장 가까운 곳부터 잇습니다 (결정 5).
 * "장소 목록 + 구간 목록" 모양이라 나중에 구간 거리만 TMAP 도보 경로로 바꿀 수 있습니다.
 */
public final class GreedyCourseBuilder {

    private GreedyCourseBuilder() {
    }

    public record Spot(Long placeId, PlaceCategory category, double lat, double lng, boolean offLeash) {
    }

    public record Stop(Spot spot, int legM, int legMinutes) {
    }

    public record Plan(List<Stop> stops, int startToFirstM, int totalM, int totalMinutes) {
    }

    public static Optional<Plan> build(double startLat, double startLng, List<PlaceCategory> order, List<Spot> spots,
                                       CourseRules rules, int radiusM, Collection<Long> excludePlaceIds) {
        double lat = startLat;
        double lng = startLng;
        int total = 0;
        // 다시 추천: 앞서 보여 준 장소는 처음부터 쓴 것으로 칩니다
        Set<Long> used = new HashSet<>(excludePlaceIds);
        List<Stop> stops = new ArrayList<>();
        for (PlaceCategory type : order) {
            List<PlaceCategory> allowed = rules.replaceRestaurant() && type == PlaceCategory.RESTAURANT
                    ? List.of(PlaceCategory.PARK, PlaceCategory.CAFE) : List.of(type);
            Stop next = null;
            for (PlaceCategory t : allowed) {
                next = nearest(lat, lng, startLat, startLng, t, spots, used, rules, radiusM, total);
                if (next != null) {
                    break;
                }
            }
            if (next == null) {
                return Optional.empty();
            }
            stops.add(next);
            used.add(next.spot().placeId());
            total += next.legM();
            lat = next.spot().lat();
            lng = next.spot().lng();
        }
        if (stops.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Plan(stops, stops.get(0).legM(), total, Geo.walkMinutes(total)));
    }

    private static Stop nearest(double lat, double lng, double startLat, double startLng, PlaceCategory type,
                                List<Spot> spots, Set<Long> used, CourseRules rules, int radiusM, int totalSoFar) {
        Comparator<Spot> byDistance = Comparator.comparingInt(s -> Geo.distanceM(lat, lng, s.lat(), s.lng()));
        Comparator<Spot> order = rules.preferOffLeash()
                ? Comparator.comparing((Spot s) -> !s.offLeash()).thenComparing(byDistance) : byDistance;
        return spots.stream()
                .filter(s -> s.category() == type && !used.contains(s.placeId()))
                .filter(s -> !(rules.excludeOffLeash() && s.offLeash()))
                .filter(s -> Geo.distanceM(startLat, startLng, s.lat(), s.lng()) <= radiusM)
                .filter(s -> {
                    int leg = Geo.distanceM(lat, lng, s.lat(), s.lng());
                    return (rules.maxLegM() == null || leg <= rules.maxLegM())
                            && (rules.maxTotalM() == null || totalSoFar + leg <= rules.maxTotalM());
                })
                .min(order)
                .map(s -> {
                    int leg = Geo.distanceM(lat, lng, s.lat(), s.lng());
                    return new Stop(s, leg, Geo.walkMinutes(leg));
                })
                .orElse(null);
    }
}
