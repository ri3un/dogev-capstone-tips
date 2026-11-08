package com.gaebalhagae.course.domain;

import com.gaebalhagae.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 코스의 장소 하나와 직전 지점에서 오는 구간(leg) */
@Getter
@Entity
@Table(name = "course_place", indexes = @Index(columnList = "course_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoursePlace extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(nullable = false)
    private int seq;

    @Column(name = "leg_distance_m", nullable = false)
    private int legDistanceM;

    @Column(name = "leg_minutes", nullable = false)
    private int legMinutes;

    public CoursePlace(Long courseId, Long placeId, int seq, int legDistanceM, int legMinutes) {
        this.courseId = courseId;
        this.placeId = placeId;
        this.seq = seq;
        this.legDistanceM = legDistanceM;
        this.legMinutes = legMinutes;
    }
}
