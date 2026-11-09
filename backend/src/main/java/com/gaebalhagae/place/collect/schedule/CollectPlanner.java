package com.gaebalhagae.place.collect.schedule;

import com.gaebalhagae.place.collect.CollectedPlace;
import com.gaebalhagae.place.review.ReviewSummary;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/** 매일 수집에서 "오늘 무엇을 할지" 정하는 순수 함수 모음 (파일·네트워크 없음) */
public final class CollectPlanner {

    private CollectPlanner() {
    }

    /** wrapped = 이번에 목록 끝을 넘어 처음으로 돌아왔음 (= 검색어를 한 바퀴 다 돌았음) */
    public record Queries(List<String> queries, int nextCursor, boolean wrapped) {
    }

    /** 검색어 = "지역 키워드". 지역마다 모든 키워드를 돈 뒤 다음 지역 */
    public static Queries nextQueries(List<String> areas, List<String> keywords, int cursor, int count) {
        List<String> all = new ArrayList<>();
        for (String area : areas) {
            for (String keyword : keywords) {
                all.add(area + " " + keyword);
            }
        }
        if (all.isEmpty() || count <= 0) {
            return new Queries(List.of(), 0, false);
        }
        int start = Math.floorMod(cursor, all.size());
        List<String> picked = new ArrayList<>();
        for (int i = 0; i < Math.min(count, all.size()); i++) {
            picked.add(all.get((start + i) % all.size()));
        }
        int end = start + picked.size();
        return new Queries(picked, end % all.size(), end >= all.size());
    }

    /** 요약 없는 곳(찾은 순서) 먼저, 그다음 요약이 오래된 순. 블로그·요약은 이름으로 하므로 이름이 같으면 한 번만 */
    public static List<String> pickPlaces(List<CollectedPlace> places,
                                          Function<String, Optional<ReviewSummary>> summaryOf, int limit) {
        Set<String> names = new LinkedHashSet<>();
        places.forEach(p -> names.add(p.name()));

        List<String> fresh = new ArrayList<>();
        List<ReviewSummary> old = new ArrayList<>();
        for (String name : names) {
            Optional<ReviewSummary> s = summaryOf.apply(name);
            if (s.isEmpty()) {
                fresh.add(name);
            } else {
                old.add(s.get());
            }
        }
        old.sort(Comparator.comparing(ReviewSummary::summarizedAt,
                Comparator.nullsFirst(Comparator.<LocalDateTime>naturalOrder())));

        List<String> picked = new ArrayList<>(fresh);
        old.forEach(s -> picked.add(s.placeName()));
        return picked.stream().limit(Math.max(limit, 0)).toList();
    }
}
