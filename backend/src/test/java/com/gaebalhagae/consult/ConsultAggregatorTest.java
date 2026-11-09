package com.gaebalhagae.consult;

import static org.assertj.core.api.Assertions.assertThat;

import com.gaebalhagae.consult.domain.Urgency;
import com.gaebalhagae.consult.service.ConsultAggregator;
import com.gaebalhagae.consult.service.ConsultAggregator.ModelAnswer;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsultAggregatorTest {

    private static ModelAnswer a(Urgency u, boolean vet, String reason) {
        return new ModelAnswer(u, vet, "답변", List.of(reason), List.of());
    }

    @Test
    void 긴급도는_가장_높은_값_병원_권고는_하나라도_있으면() {
        var r = ConsultAggregator.aggregate(List.of(a(Urgency.LOW, false, "식욕 정상"),
                a(Urgency.HIGH, true, "반복 구토"), a(Urgency.MEDIUM, false, "탈수 주의")), false);

        assertThat(r.urgency()).isEqualTo(Urgency.HIGH);
        assertThat(r.visitVet()).isTrue();
        assertThat(r.crossChecked()).isTrue();
        assertThat(r.summary()).hasSize(3);
        assertThat(r.notices()).last().asString().contains("진단이 아닙니다");
    }

    @Test
    void 하나만_답하면_교차검증_안_됨() {
        var r = ConsultAggregator.aggregate(List.of(a(Urgency.LOW, false, "괜찮아요")), false);

        assertThat(r.crossChecked()).isFalse();
        assertThat(r.notices()).anyMatch(n -> n.contains("교차검증"));
    }

    @Test
    void 응급_표현이면_병원_안내가_맨_앞() {
        var r = ConsultAggregator.aggregate(List.of(a(Urgency.LOW, false, "x"), a(Urgency.LOW, false, "y")), true);

        assertThat(r.urgency()).isEqualTo(Urgency.EMERGENCY);
        assertThat(r.visitVet()).isTrue();
        assertThat(r.notices().get(0)).contains("즉시 동물병원");
        assertThat(r.summary().get(0)).doesNotContain("지켜봐도");
    }

    @Test
    void 긴급도가_높으면_병원_권고가_없어도_지켜보라고_하지_않는다() {
        var r = ConsultAggregator.aggregate(List.of(a(Urgency.HIGH, false, "x")), false);

        assertThat(r.visitVet()).isTrue();
        assertThat(r.summary().get(0)).doesNotContain("지켜봐도");
    }
}
