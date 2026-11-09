package com.gaebalhagae.question.dto;

import com.gaebalhagae.question.domain.QuestionSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class VetQuestionRequests {

    private VetQuestionRequests() {
    }

    public record Create(@NotBlank @Size(max = 500) String content, QuestionSource source) {
    }

    public record Edit(@Size(max = 500) String content, Boolean resolved) {
    }
}
