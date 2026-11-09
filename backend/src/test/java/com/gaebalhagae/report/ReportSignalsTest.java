package com.gaebalhagae.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.gaebalhagae.record.dto.RecordResponses.WeightPoint;
import com.gaebalhagae.report.service.ReportSignals;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportSignalsTest {

    private final LocalDate d = LocalDate.of(2026, 11, 1);

    @Test
    void 체중이_5퍼센트_이상_변하면_신호() {
        var weights = List.of(new WeightPoint(d, new BigDecimal("10.0")), new WeightPoint(d.plusDays(20),
                new BigDecimal("9.4")));
        assertThat(ReportSignals.of(weights, List.of(), 0)).singleElement().asString().contains("6%").contains("줄었");
        var small = List.of(new WeightPoint(d, new BigDecimal("10.0")), new WeightPoint(d.plusDays(20),
                new BigDecimal("10.3")));
        assertThat(ReportSignals.of(small, List.of(), 0)).isEmpty();
        var almost = List.of(new WeightPoint(d, new BigDecimal("4.00")), new WeightPoint(d.plusDays(20),
                new BigDecimal("4.19")));   // 4.75%
        assertThat(ReportSignals.of(almost, List.of(), 0)).isEmpty();
    }

    @Test
    void 사흘_안에_무른_변이_두번이면_신호() {
        assertThat(ReportSignals.of(List.of(), List.of(d, d.plusDays(2)), 0)).hasSize(1);
        assertThat(ReportSignals.of(List.of(), List.of(d, d.plusDays(3)), 0)).isEmpty();
    }

    @Test
    void 증상_기록이_세번_이상이면_신호() {
        assertThat(ReportSignals.of(List.of(), List.of(), 3)).singleElement().asString().contains("3회");
        assertThat(ReportSignals.of(List.of(), List.of(), 2)).isEmpty();
    }
}
