package com.gaebalhagae.record;

import static org.assertj.core.api.Assertions.assertThat;

import com.gaebalhagae.record.service.DangerFoodRule;
import org.junit.jupiter.api.Test;

class DangerFoodRuleTest {

    private final DangerFoodRule rule = new DangerFoodRule();

    @Test
    void 글에서_위험_음식을_찾는다() {
        assertThat(rule.check("산책하고 와서 포도 두 알을 먹었어요"))
                .extracting(DangerFoodRule.Warning::food).containsExactly("포도·건포도");
        assertThat(rule.check("초콜릿 과자를 조금 핥았어")).hasSize(1);
    }

    @Test
    void 위험_음식이_없으면_경고도_없다() {
        assertThat(rule.check("사료 반 컵 먹고 파란 공으로 놀았어요")).isEmpty();
        assertThat(rule.check(null)).isEmpty();
    }
}
