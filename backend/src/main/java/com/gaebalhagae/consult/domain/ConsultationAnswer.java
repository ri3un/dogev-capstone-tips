package com.gaebalhagae.consult.domain;

import com.gaebalhagae.global.domain.BaseTimeEntity;
import com.gaebalhagae.llm.LlmParallelCaller;
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

/** 모델 하나의 답. status 가 OK 가 아니면 answer_json 은 비어 있습니다 */
@Getter
@Entity
@Table(name = "consultation_answer", indexes = @Index(columnList = "consultation_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsultationAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consultation_id", nullable = false)
    private Long consultationId;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LlmParallelCaller.Status status;

    @Column(name = "answer_json", columnDefinition = "text")
    private String answerJson;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    public ConsultationAnswer(Long consultationId, String modelName, LlmParallelCaller.Status status,
                              String answerJson, long latencyMs) {
        this.consultationId = consultationId;
        this.modelName = modelName;
        this.status = status;
        this.answerJson = answerJson;
        this.latencyMs = latencyMs;
    }
}
