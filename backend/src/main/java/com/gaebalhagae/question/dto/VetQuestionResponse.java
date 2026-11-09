package com.gaebalhagae.question.dto;

import com.gaebalhagae.question.domain.QuestionSource;
import com.gaebalhagae.question.domain.VetQuestion;
import java.time.LocalDateTime;

public record VetQuestionResponse(Long id, Long dogId, String content, QuestionSource source, boolean resolved,
                                  Long createdBy, LocalDateTime createdAt) {

    public static VetQuestionResponse from(VetQuestion q) {
        return new VetQuestionResponse(q.getId(), q.getDogId(), q.getContent(), q.getSource(), q.isResolved(),
                q.getCreatedBy(), q.getCreatedAt());
    }
}
