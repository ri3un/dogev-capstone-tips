package com.gaebalhagae.consult.dto;

import com.gaebalhagae.consult.domain.Urgency;
import com.gaebalhagae.llm.LlmParallelCaller;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class ConsultResponses {

    private ConsultResponses() {
    }

    public record Ask(@NotBlank @Size(max = 1000) String question) {
    }

    public record Detail(Long id, Long dogId, String question, boolean emergency, Urgency urgency, String urgencyLabel,
                         boolean visitVet, boolean crossChecked, int answeredCount, int totalModels,
                         List<String> summary, List<String> notices, List<ModelItem> answers, Long createdBy,
                         LocalDateTime createdAt, List<String> contextLines) {
    }

    public record ModelItem(String model, LlmParallelCaller.Status status, Urgency urgency, Boolean visitVet,
                            String answer, List<String> reasons, List<String> homeCare, long latencyMs) {
    }

    public record Item(Long id, String question, Urgency urgency, String urgencyLabel, boolean emergency,
                       boolean visitVet, LocalDateTime createdAt) {
    }
}
