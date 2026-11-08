package com.gaebalhagae.family.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class FamilyRequests {

    private FamilyRequests() {
    }

    public record Rename(@NotBlank @Size(max = 30) String name) {
    }

    public record TransferOwner(@NotNull Long memberId) {
    }
}
