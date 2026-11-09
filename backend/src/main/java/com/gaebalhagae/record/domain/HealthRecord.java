package com.gaebalhagae.record.domain;

import com.gaebalhagae.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 건강 기록 한 건 (결정 5·9). 세부 내용은 data(JSON), 자주 집계하는 값만 별도 컬럼.
 * memo 는 화면에 보일 한 줄이며 data.memo 에 함께 둡니다.
 */
@Getter
@Entity
@Table(name = "health_record", indexes = @Index(columnList = "dog_id, record_date"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dog_id", nullable = false)
    private Long dogId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecordCategory category;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> data = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RecordSource source;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "stool_status", length = 10)
    private StoolStatus stoolStatus;

    @Column(name = "walk_minutes")
    private Integer walkMinutes;

    @Column(name = "water_ml")
    private Integer waterMl;

    public HealthRecord(Long dogId, Long memberId, RecordSource source, Values values) {
        this.dogId = dogId;
        this.createdBy = memberId;
        this.source = source;
        apply(memberId, values);
    }

    public record Values(LocalDate recordDate, RecordCategory category, String memo, Map<String, Object> data,
                         BigDecimal weightKg, StoolStatus stoolStatus, Integer walkMinutes, Integer waterMl) {
    }

    public void apply(Long memberId, Values v) {
        this.updatedBy = memberId;
        this.recordDate = v.recordDate();
        this.category = v.category();
        Map<String, Object> d = v.data() == null ? new HashMap<>() : new HashMap<>(v.data());
        if (v.memo() != null && !v.memo().isBlank()) {
            d.put("memo", v.memo().strip());
        } else {
            d.remove("memo");
        }
        this.data = d;
        this.weightKg = v.weightKg();
        this.stoolStatus = v.stoolStatus();
        this.walkMinutes = v.walkMinutes();
        this.waterMl = v.waterMl();
    }

    public String memo() {
        Object m = data == null ? null : data.get("memo");
        return m == null ? null : m.toString();
    }
}
