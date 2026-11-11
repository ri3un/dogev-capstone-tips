package com.gaebalhagae.consult;

import static org.assertj.core.api.Assertions.assertThat;

import com.gaebalhagae.consult.service.RecentContextBuilder;
import com.gaebalhagae.record.domain.HealthRecord;
import com.gaebalhagae.record.domain.RecordCategory;
import com.gaebalhagae.record.domain.RecordSource;
import com.gaebalhagae.record.domain.StoolStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecentContextBuilderTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 11);

    private static HealthRecord rec(int daysAgo, RecordCategory c, String memo, BigDecimal kg, StoolStatus stool,
                                    Integer walk) {
        return new HealthRecord(1L, 1L, RecordSource.MANUAL, new HealthRecord.Values(TODAY.minusDays(daysAgo), c, memo,
                null, kg, stool, walk, null));
    }

    @Test
    void 기록이_없으면_빈_목록() {
        assertThat(RecentContextBuilder.lines(List.of(), TODAY)).isEmpty();
    }

    @Test
    void 숫자_집계만_보내고_기록_원문은_보내지_않는다() {
        List<HealthRecord> records = List.of(
                rec(5, RecordCategory.MEAL, "사료 50g", null, null, null),
                rec(4, RecordCategory.MEAL, null, null, null, null),
                rec(3, RecordCategory.STOOL, null, null, StoolStatus.SOFT, null),
                rec(2, RecordCategory.SYMPTOM, "왼쪽 다리를 절어요", null, null, null),
                rec(1, RecordCategory.ACTIVITY, "공원 산책", null, null, 28),
                rec(9, RecordCategory.WEIGHT, null, new BigDecimal("4.0"), null, null),
                rec(0, RecordCategory.WEIGHT, null, new BigDecimal("4.4"), null, null));

        List<String> lines = RecentContextBuilder.lines(records, TODAY);
        String all = String.join("\n", lines);

        assertThat(all).contains("식사 2회", "배변 1회(무른 변 1회)", "증상 1회(마지막 11월 9일)",
                "산책·활동 총 28분(하루 평균 2분)", "체중 4 → 4.4kg", "10% 늘었어요");
        assertThat(all).doesNotContain("다리를 절어요", "사료 50g", "공원");
    }
}
