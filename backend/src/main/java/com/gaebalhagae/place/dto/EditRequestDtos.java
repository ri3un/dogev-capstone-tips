package com.gaebalhagae.place.dto;

import com.gaebalhagae.place.domain.EditRequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class EditRequestDtos {

    private EditRequestDtos() {
    }

    public record Create(@NotBlank @Size(max = 30) String conditionKey, @NotBlank @Size(max = 20) String requestedValue,
                         @Size(max = 300) String comment) {
    }

    public record Reject(@NotBlank @Size(max = 100) String reason) {
    }

    public record Response(Long id, Long placeId, String placeName, String conditionKey, String conditionLabel,
                           String currentValue, String requestedValue, String comment, EditRequestStatus status,
                           String rejectReason, Long memberId, String nickname, LocalDateTime createdAt) {
    }

    /** 장소 "다녀왔어요" */
    public record Visit(@NotNull Long dogId, LocalDate visitedOn) {
    }
}
