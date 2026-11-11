package com.gaebalhagae.consult.domain;

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
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** AI 상담 질문 1건과 종합 결과. 모델별 답은 consultation_answer */
@Getter
@Entity
@Table(name = "consultation", indexes = @Index(columnList = "dog_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Consultation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dog_id", nullable = false)
    private Long dogId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(nullable = false, length = 1000)
    private String question;

    @Column(name = "emergency_flag", nullable = false)
    private boolean emergencyFlag;

    @Column(name = "answered_count", nullable = false)
    private int answeredCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Urgency urgency;

    @Column(name = "visit_vet", nullable = false)
    private boolean visitVet;

    @Column(name = "cross_checked", nullable = false)
    private boolean crossChecked;

    /** 종합 3줄 */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> summary = new ArrayList<>();

    /** 응급 안내·교차검증·진단 아님 문구 */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> notices = new ArrayList<>();

    /** 이번 답변에 함께 보낸 최근 14일 기록 집계 (화면 "사용한 내 기록", 90일 정리 때도 남김) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_lines")
    private List<String> contextLines = new ArrayList<>();

    public Consultation(Long dogId, Long createdBy, String question, boolean emergencyFlag, int answeredCount,
                        Urgency urgency, boolean visitVet, boolean crossChecked, List<String> summary,
                        List<String> notices, List<String> contextLines) {
        this.contextLines = new ArrayList<>(contextLines);
        this.dogId = dogId;
        this.createdBy = createdBy;
        this.question = question;
        this.emergencyFlag = emergencyFlag;
        this.answeredCount = answeredCount;
        this.urgency = urgency;
        this.visitVet = visitVet;
        this.crossChecked = crossChecked;
        this.summary = new ArrayList<>(summary);
        this.notices = new ArrayList<>(notices);
    }
}
