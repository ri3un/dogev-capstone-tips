package com.gaebalhagae.place.domain;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자가 보낸 장소 조건 수정 요청. 관리자가 승인하면 반영, 반려하면 사유만 남김 (결정 14-③) */
@Getter
@Entity
@Table(name = "place_edit_request", indexes = {@Index(columnList = "status"), @Index(columnList = "member_id")})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceEditRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "condition_key", nullable = false, length = 30)
    private String conditionKey;

    @Column(name = "requested_value", nullable = false, length = 20)
    private String requestedValue;

    @Column(length = 300)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EditRequestStatus status;

    @Column(name = "reject_reason", length = 100)
    private String rejectReason;

    public PlaceEditRequest(Long placeId, Long memberId, String conditionKey, String requestedValue, String comment) {
        this.placeId = placeId;
        this.memberId = memberId;
        this.conditionKey = conditionKey;
        this.requestedValue = requestedValue;
        this.comment = comment;
        this.status = EditRequestStatus.PENDING;
    }

    public boolean isPending() {
        return status == EditRequestStatus.PENDING;
    }

    public void approve() {
        this.status = EditRequestStatus.APPROVED;
    }

    public void reject(String reason) {
        this.status = EditRequestStatus.REJECTED;
        this.rejectReason = reason;
    }
}
