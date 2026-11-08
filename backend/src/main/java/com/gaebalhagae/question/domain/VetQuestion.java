package com.gaebalhagae.question.domain;

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

/** 병원에서 물어볼 질문 메모. PDF ⑨ 섹션 (결정 9) */
@Getter
@Entity
@Table(name = "vet_question", indexes = @Index(columnList = "dog_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VetQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dog_id", nullable = false)
    private Long dogId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(nullable = false, length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private QuestionSource source;

    @Column(nullable = false)
    private boolean resolved;

    public VetQuestion(Long dogId, Long createdBy, String content, QuestionSource source) {
        this.dogId = dogId;
        this.createdBy = createdBy;
        this.content = content.strip();
        this.source = source;
    }

    public void edit(String content, Boolean resolved) {
        if (content != null && !content.isBlank()) {
            this.content = content.strip();
        }
        if (resolved != null) {
            this.resolved = resolved;
        }
    }
}
