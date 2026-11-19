package com.gaebalhagae.course.domain;

import com.gaebalhagae.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 추천 코스. 출발 좌표는 저장하지 않고 출발점 → 첫 장소 거리만 숫자로 남깁니다 (결정 15-①, 31) */
@Getter
@Entity
@Table(name = "course", indexes = @Index(columnList = "dog_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "dog_id", nullable = false)
    private Long dogId;

    @Column(name = "start_to_first_m", nullable = false)
    private int startToFirstM;

    @Column(name = "total_distance_m", nullable = false)
    private int totalDistanceM;

    @Column(name = "total_minutes", nullable = false)
    private int totalMinutes;

    /** 규칙 이유 한 줄씩, 줄바꿈으로 구분 */
    @Column(name = "reason_text", length = 500)
    private String reasonText;

    @Column(name = "visited_at")
    private LocalDateTime visitedAt;

    /** "코스 저장하기"를 누른 시각. 비어 있으면 다시 추천 때 바뀌는 임시 코스 */
    @Column(name = "saved_at")
    private LocalDateTime savedAt;

    public Course(Long createdBy, Long dogId, int startToFirstM, int totalDistanceM, int totalMinutes,
                  String reasonText) {
        this.createdBy = createdBy;
        this.dogId = dogId;
        this.startToFirstM = startToFirstM;
        this.totalDistanceM = totalDistanceM;
        this.totalMinutes = totalMinutes;
        this.reasonText = reasonText;
    }

    public void markVisited(LocalDateTime at) {
        this.visitedAt = at;
        markSaved(at);
    }

    public void markSaved(LocalDateTime at) {
        if (savedAt == null) {
            savedAt = at;
        }
    }

    /** 저장도 안 하고 다녀오지도 않은 코스 */
    public boolean isDraft() {
        return savedAt == null && visitedAt == null;
    }
}
