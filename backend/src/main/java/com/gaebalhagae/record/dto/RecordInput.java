package com.gaebalhagae.record.dto;

import com.gaebalhagae.record.domain.HealthRecord;
import com.gaebalhagae.record.domain.RecordCategory;
import com.gaebalhagae.record.domain.StoolStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** 기록 한 건 입력. 글 변환 후보(확인 카드)도 같은 모양이라 그대로 저장 요청에 씁니다 */
public record RecordInput(
        @NotNull RecordCategory category,
        @NotNull @PastOrPresent LocalDate recordDate,
        @Size(max = 200) String memo,
        @Size(max = 20) Map<String, Object> data,
        @DecimalMin("0.1") @DecimalMax("100") BigDecimal weightKg,
        StoolStatus stoolStatus,
        @Min(0) @Max(1440) Integer walkMinutes,
        @Min(0) @Max(10000) Integer waterMl
) {

    public HealthRecord.Values toValues() {
        return new HealthRecord.Values(recordDate, category, memo, data, weightKg, stoolStatus, walkMinutes, waterMl);
    }
}
