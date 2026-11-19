package com.gaebalhagae.dog;

import static org.assertj.core.api.Assertions.assertThat;

import com.gaebalhagae.dog.service.NutritionCalculator;
import com.gaebalhagae.dog.service.NutritionCalculator.Result;
import org.junit.jupiter.api.Test;

class NutritionCalculatorTest {

    @Test
    void 성견_중성화_10kg() {
        Result r = NutritionCalculator.calc(10, 36, true, 3500, false);

        assertThat(r.rerKcal()).isEqualTo(394);      // 70 × 10^0.75 = 393.6
        assertThat(r.factor()).isEqualTo(1.6);
        assertThat(r.derKcal()).isEqualTo(630);
        assertThat(r.foodGrams()).isEqualTo(180);    // 630 ÷ 3500 × 1000
        assertThat(r.waterMinMl()).isEqualTo(300);
        assertThat(r.waterMaxMl()).isEqualTo(600);
    }

    @Test
    void 생애_단계별_계수() {
        assertThat(NutritionCalculator.calc(3, 3, false, null, false).factor()).isEqualTo(3.0);   // 4개월 미만
        assertThat(NutritionCalculator.calc(3, 8, false, null, false).factor()).isEqualTo(2.0);   // 1살 미만
        assertThat(NutritionCalculator.calc(3, 96, true, null, false).factor()).isEqualTo(1.4);   // 7살 이상
        assertThat(NutritionCalculator.calc(3, 36, false, null, false).factor()).isEqualTo(1.8);  // 중성화 안 함
    }

    @Test
    void 사료_칼로리를_모르면_일반_건사료_3500으로_계산한다() {
        Result r = NutritionCalculator.calc(10, 36, true, null, false);

        assertThat(r.foodGrams()).isEqualTo(180);    // 630 ÷ 3500 × 1000
        assertThat(r.foodGramsEstimated()).isTrue();
        assertThat(NutritionCalculator.calc(10, 36, true, 3500, false).foodGramsEstimated()).isFalse();
    }

    @Test
    void 비만이면_감량_계수_1() {
        Result r = NutritionCalculator.calc(10, 36, true, null, true);

        assertThat(r.factor()).isEqualTo(1.0);
        assertThat(r.derKcal()).isEqualTo(394);
        assertThat(r.foodGrams()).isEqualTo(112);    // 393.6 ÷ 3500 × 1000 = 112.46
    }
}
