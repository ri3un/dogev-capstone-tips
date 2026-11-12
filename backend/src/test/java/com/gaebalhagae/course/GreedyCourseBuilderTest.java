package com.gaebalhagae.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.gaebalhagae.course.service.CourseRules;
import com.gaebalhagae.course.service.GreedyCourseBuilder;
import com.gaebalhagae.course.service.GreedyCourseBuilder.Spot;
import com.gaebalhagae.dog.domain.ActivityLevel;
import com.gaebalhagae.place.domain.PlaceCategory;
import java.util.List;
import org.junit.jupiter.api.Test;

class GreedyCourseBuilderTest {

    // 위도 0.001도 ≈ 111m
    private final Spot cafeNear = new Spot(1L, PlaceCategory.CAFE, 37.001, 127.0, false);
    private final Spot cafeFar = new Spot(2L, PlaceCategory.CAFE, 37.010, 127.0, false);
    private final Spot park = new Spot(3L, PlaceCategory.PARK, 37.002, 127.0, false);
    private final Spot offLeashPark = new Spot(4L, PlaceCategory.PARK, 37.004, 127.0, true);
    private final CourseRules none = CourseRules.of(new CourseRules.Input(36, List.of(), null, List.of(), 0));

    @Test
    void 순서대로_가장_가까운_곳을_잇는다() {
        var plan = GreedyCourseBuilder.build(37.0, 127.0, List.of(PlaceCategory.CAFE, PlaceCategory.PARK),
                List.of(cafeFar, cafeNear, park, offLeashPark), none, 3000).orElseThrow();

        assertThat(plan.stops()).extracting(s -> s.spot().placeId()).containsExactly(1L, 3L);
        assertThat(plan.startToFirstM()).isCloseTo(111, org.assertj.core.data.Offset.offset(2));
        assertThat(plan.totalM()).isCloseTo(222, org.assertj.core.data.Offset.offset(3));
    }

    @Test
    void 반경_밖이거나_후보가_없으면_실패() {
        assertThat(GreedyCourseBuilder.build(37.0, 127.0, List.of(PlaceCategory.RESTAURANT),
                List.of(cafeNear), none, 3000)).isEmpty();
        assertThat(GreedyCourseBuilder.build(37.0, 127.0, List.of(PlaceCategory.CAFE),
                List.of(cafeFar), none, 500)).isEmpty();
    }

    @Test
    void 오프리쉬_우선과_제외() {
        CourseRules prefer = CourseRules.of(new CourseRules.Input(36, List.of(), ActivityLevel.HIGH, List.of(), 0));
        assertThat(GreedyCourseBuilder.build(37.0, 127.0, List.of(PlaceCategory.PARK), List.of(park, offLeashPark),
                prefer, 3000).orElseThrow().stops().get(0).spot().placeId()).isEqualTo(4L);

        CourseRules exclude = CourseRules.of(new CourseRules.Input(36, List.of("JOINT"), null, List.of(), 0));
        assertThat(GreedyCourseBuilder.build(37.0, 127.0, List.of(PlaceCategory.PARK), List.of(offLeashPark),
                exclude, 3000)).isEmpty();
    }

    @Test
    void 설사_규칙이면_식당_대신_산책로() {
        CourseRules loose = CourseRules.of(new CourseRules.Input(36, List.of(), null, List.of(), 3));
        var plan = GreedyCourseBuilder.build(37.0, 127.0, List.of(PlaceCategory.RESTAURANT), List.of(park), loose, 3000);
        assertThat(plan).isPresent();
    }
}
