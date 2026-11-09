package com.gaebalhagae.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.gaebalhagae.course.service.CourseRules;
import com.gaebalhagae.course.service.CourseRules.Input;
import com.gaebalhagae.dog.domain.ActivityLevel;
import java.util.List;
import org.junit.jupiter.api.Test;

class CourseRulesTest {

    @Test
    void 규칙이_없으면_제한도_없다() {
        CourseRules r = CourseRules.of(new Input(36, List.of(), ActivityLevel.NORMAL, List.of(), 0));
        assertThat(r.maxTotalM()).isNull();
        assertThat(r.reasons()).isEmpty();
    }

    @Test
    void 노령견은_구간_1km_총_2km() {
        CourseRules r = CourseRules.of(new Input(100, List.of(), null, List.of(), 0));
        assertThat(r.maxLegM()).isEqualTo(1000);
        assertThat(r.maxTotalM()).isEqualTo(2000);
        assertThat(r.reasons()).containsExactly("노령견이라 2km 이내로 추천했어요");
    }

    @Test
    void 관절_지병이나_절뚝임_메모는_오프리쉬_제외() {
        assertThat(CourseRules.of(new Input(36, List.of("JOINT"), null, List.of(), 0)).excludeOffLeash()).isTrue();
        CourseRules r = CourseRules.of(new Input(36, List.of(), null, List.of("산책 후 뒷다리를 절뚝였어요"), 0));
        assertThat(r.excludeOffLeash()).isTrue();
        assertThat(r.maxTotalM()).isEqualTo(2000);
    }

    @Test
    void 최근_설사가_잦으면_식당_대신_짧게() {
        CourseRules r = CourseRules.of(new Input(36, List.of(), null, List.of(), 2));
        assertThat(r.replaceRestaurant()).isTrue();
        assertThat(r.maxTotalM()).isEqualTo(1500);
    }

    @Test
    void 활동량이_많고_지병이_없으면_오프리쉬_우선() {
        assertThat(CourseRules.of(new Input(36, List.of(), ActivityLevel.HIGH, List.of(), 0)).preferOffLeash()).isTrue();
        assertThat(CourseRules.of(new Input(36, List.of("SKIN"), ActivityLevel.HIGH, List.of(), 0)).preferOffLeash())
                .isFalse();
    }
}
