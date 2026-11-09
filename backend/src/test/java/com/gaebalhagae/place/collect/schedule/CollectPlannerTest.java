package com.gaebalhagae.place.collect.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import com.gaebalhagae.place.collect.CollectedPlace;
import com.gaebalhagae.place.review.ReviewSummary;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CollectPlannerTest {

    private static final List<String> AREAS = List.of("평촌", "범계");
    private static final List<String> KEYWORDS = List.of("애견동반 카페", "애견동반 식당", "강아지 공원");

    @Test
    void 지역마다_키워드를_차례로_돌고_다음_위치를_돌려준다() {
        CollectPlanner.Queries q = CollectPlanner.nextQueries(AREAS, KEYWORDS, 0, 4);
        assertThat(q.queries()).containsExactly("평촌 애견동반 카페", "평촌 애견동반 식당", "평촌 강아지 공원", "범계 애견동반 카페");
        assertThat(q.nextCursor()).isEqualTo(4);
        assertThat(q.wrapped()).isFalse();
    }

    @Test
    void 끝까지_가면_처음으로_돌아가고_한바퀴_돌았다고_알린다() {
        CollectPlanner.Queries q = CollectPlanner.nextQueries(AREAS, KEYWORDS, 5, 3);
        assertThat(q.queries()).containsExactly("범계 강아지 공원", "평촌 애견동반 카페", "평촌 애견동반 식당");
        assertThat(q.nextCursor()).isEqualTo(2);
        assertThat(q.wrapped()).isTrue();
    }

    @Test
    void 요약_없는_곳을_먼저_그다음_오래된_요약_순으로_고른다() {
        List<CollectedPlace> places = List.of(place("1", "새카페"), place("2", "옛식당"), place("3", "최근공원"), place("4", "또새곳"));
        Map<String, ReviewSummary> summaries = Map.of(
                "옛식당", summary("옛식당", LocalDateTime.of(2026, 10, 1, 3, 0)),
                "최근공원", summary("최근공원", LocalDateTime.of(2026, 11, 8, 3, 0)));

        List<String> picked = CollectPlanner.pickPlaces(places, n -> Optional.ofNullable(summaries.get(n)), 3);

        assertThat(picked).containsExactly("새카페", "또새곳", "옛식당");
    }

    @Test
    void 이름이_같은_장소는_한번만_고른다() {
        List<CollectedPlace> places = List.of(place("1", "멍카페"), place("2", "멍카페"), place("3", "냥식당"));
        assertThat(CollectPlanner.pickPlaces(places, n -> Optional.empty(), 30)).containsExactly("멍카페", "냥식당");
    }

    private static CollectedPlace place(String id, String name) {
        return new CollectedPlace(id, name, "음식점 > 카페", "CE7", "경기 안양시 동안구", "", 37.39, 126.95, "", "", List.of(), null);
    }

    private static ReviewSummary summary(String name, LocalDateTime at) {
        return new ReviewSummary(name, List.of(), List.of(), Map.of(), List.of(), 1, List.of(), "test", at);
    }
}
