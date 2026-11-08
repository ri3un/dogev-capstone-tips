package com.gaebalhagae.place.domain;

import com.gaebalhagae.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 장소 조건 하나 (결정 8). 화면의 "블로그 5건 중 4건" = evidence_count 5, positive_count 4 */
@Getter
@Entity
@Table(name = "place_condition", uniqueConstraints = @UniqueConstraint(columnNames = {"place_id", "condition_key"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceCondition extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(name = "condition_key", nullable = false, length = 30)
    private String conditionKey;

    @Column(name = "condition_value", nullable = false, length = 20)
    private String conditionValue;

    @Column(name = "weight_limit_kg")
    private Integer weightLimitKg;

    @Column(name = "evidence_count", nullable = false)
    private int evidenceCount;

    @Column(name = "positive_count", nullable = false)
    private int positiveCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ConditionSource source;

    public PlaceCondition(Long placeId, String conditionKey) {
        this.placeId = placeId;
        this.conditionKey = conditionKey;
    }

    public void update(String value, Integer weightLimitKg, int evidenceCount, int positiveCount,
                       ConditionSource source) {
        this.conditionValue = value;
        this.weightLimitKg = weightLimitKg;
        this.evidenceCount = evidenceCount;
        this.positiveCount = positiveCount;
        this.source = source;
    }

    /** 관리자 승인: 값과 출처만 바꾸고 근거 수는 남깁니다 */
    public void overrideByAdmin(String value) {
        this.conditionValue = value;
        this.source = ConditionSource.ADMIN;
    }
}
