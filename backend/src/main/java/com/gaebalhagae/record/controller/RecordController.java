package com.gaebalhagae.record.controller;

import com.gaebalhagae.global.response.ApiResponse;
import com.gaebalhagae.global.security.LoginMember;
import com.gaebalhagae.record.domain.RecordCategory;
import com.gaebalhagae.record.dto.RecordInput;
import com.gaebalhagae.record.dto.RecordRequests;
import com.gaebalhagae.record.dto.RecordResponses;
import com.gaebalhagae.record.service.RecordParseService;
import com.gaebalhagae.record.service.RecordService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;
    private final RecordParseService parseService;

    /** 글 → 기록 후보 (저장 안 함). 프론트는 확인 카드를 보여 준 뒤 /records/batch 로 저장 */
    @PostMapping("/api/dogs/{dogId}/records/parse")
    public ApiResponse<RecordResponses.Parsed> parse(@AuthenticationPrincipal LoginMember me, @PathVariable Long dogId,
                                                     @Valid @RequestBody RecordRequests.Parse request) {
        return ApiResponse.ok(parseService.parse(me.id(), dogId, request.text(), request.date()));
    }

    @PostMapping("/api/dogs/{dogId}/records/batch")
    public ApiResponse<List<RecordResponses.Record>> batch(@AuthenticationPrincipal LoginMember me,
                                                           @PathVariable Long dogId,
                                                           @Valid @RequestBody RecordRequests.Batch request) {
        return ApiResponse.ok(recordService.saveBatch(me.id(), dogId, request.records()));
    }

    @PostMapping("/api/dogs/{dogId}/records")
    public ApiResponse<RecordResponses.Record> create(@AuthenticationPrincipal LoginMember me, @PathVariable Long dogId,
                                                      @Valid @RequestBody RecordInput request) {
        return ApiResponse.ok(recordService.create(me.id(), dogId, request));
    }

    @GetMapping("/api/dogs/{dogId}/records")
    public ApiResponse<List<RecordResponses.Record>> list(
            @AuthenticationPrincipal LoginMember me, @PathVariable Long dogId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) RecordCategory category) {
        return ApiResponse.ok(recordService.list(me.id(), dogId, from, to, category));
    }

    @GetMapping("/api/dogs/{dogId}/records/summary")
    public ApiResponse<RecordResponses.Summary> summary(
            @AuthenticationPrincipal LoginMember me, @PathVariable Long dogId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(recordService.summary(me.id(), dogId, from, to));
    }

    @GetMapping("/api/dogs/{dogId}/records/weights")
    public ApiResponse<List<RecordResponses.WeightPoint>> weights(
            @AuthenticationPrincipal LoginMember me, @PathVariable Long dogId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(recordService.weights(me.id(), dogId, from, to));
    }

    @PutMapping("/api/records/{recordId}")
    public ApiResponse<RecordResponses.Record> update(@AuthenticationPrincipal LoginMember me,
                                                      @PathVariable Long recordId,
                                                      @Valid @RequestBody RecordInput request) {
        return ApiResponse.ok(recordService.update(me.id(), recordId, request));
    }

    @DeleteMapping("/api/records/{recordId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal LoginMember me, @PathVariable Long recordId) {
        recordService.delete(me.id(), recordId);
        return ApiResponse.ok();
    }
}
