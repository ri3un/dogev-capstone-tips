package com.gaebalhagae.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DevLoginRequest(@NotBlank @Size(max = 30) String nickname, boolean admin) {
}
