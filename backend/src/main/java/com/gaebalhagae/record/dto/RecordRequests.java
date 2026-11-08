package com.gaebalhagae.record.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public final class RecordRequests {

    private RecordRequests() {
    }

    /** 글 → 기록 후보. date 를 주면 "어제" 같은 말을 그 날짜 기준으로 계산 */
    public record Parse(@NotBlank @Size(max = 1000) String text, LocalDate date) {
    }

    public record Batch(@NotEmpty @Size(max = 20) List<@Valid RecordInput> records) {
    }
}
