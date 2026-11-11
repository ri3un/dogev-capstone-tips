package com.gaebalhagae.record.dto;

import com.gaebalhagae.record.domain.HealthRecord;
import com.gaebalhagae.record.domain.RecordCategory;
import com.gaebalhagae.record.domain.RecordSource;
import com.gaebalhagae.record.domain.StoolStatus;
import com.gaebalhagae.record.service.DangerFoodRule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class RecordResponses {

    private RecordResponses() {
    }

    public record Record(Long id, Long dogId, LocalDate recordDate, RecordCategory category, String categoryLabel,
                         String memo, Map<String, Object> data, BigDecimal weightKg, StoolStatus stoolStatus,
                         Integer walkMinutes, Integer waterMl, RecordSource source, Long createdBy,
                         String createdByNickname, LocalDateTime createdAt) {

        public static Record of(HealthRecord r, String nickname) {
            return new Record(r.getId(), r.getDogId(), r.getRecordDate(), r.getCategory(), r.getCategory().label(),
                    r.memo(), r.getData(), r.getWeightKg(), r.getStoolStatus(), r.getWalkMinutes(), r.getWaterMl(),
                    r.getSource(), r.getCreatedBy(), nickname, r.getCreatedAt());
        }
    }

    /** 확인 카드에 보여 줄 후보 + 서버 규칙 위험 음식 경고 */
    public record Parsed(List<RecordInput> candidates, List<DangerFoodRule.Warning> warnings, String model) {
    }

    public record Summary(LocalDate from, LocalDate to, Map<RecordCategory, Long> byCategory, List<Day> byDate,
                          int walkMinutesTotal, Integer waterAvgMl, long looseStoolCount) {
    }

    public record Day(LocalDate date, int count, List<RecordCategory> categories) {
    }

    public record WeightPoint(LocalDate date, BigDecimal weightKg) {
    }
}
