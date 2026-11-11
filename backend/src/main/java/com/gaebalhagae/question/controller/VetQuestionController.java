package com.gaebalhagae.question.controller;

import com.gaebalhagae.global.response.ApiResponse;
import com.gaebalhagae.global.security.LoginMember;
import com.gaebalhagae.question.dto.VetQuestionRequests;
import com.gaebalhagae.question.dto.VetQuestionResponse;
import com.gaebalhagae.question.service.VetQuestionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VetQuestionController {

    private final VetQuestionService service;

    @GetMapping("/api/dogs/{dogId}/vet-questions")
    public ApiResponse<List<VetQuestionResponse>> list(@AuthenticationPrincipal LoginMember me,
                                                       @PathVariable Long dogId,
                                                       @RequestParam(required = false) Boolean resolved) {
        return ApiResponse.ok(service.list(me.id(), dogId, resolved));
    }

    @PostMapping("/api/dogs/{dogId}/vet-questions")
    public ApiResponse<VetQuestionResponse> create(@AuthenticationPrincipal LoginMember me, @PathVariable Long dogId,
                                                   @Valid @RequestBody VetQuestionRequests.Create request) {
        return ApiResponse.ok(service.create(me.id(), dogId, request.content(), request.source()));
    }

    @PatchMapping("/api/vet-questions/{id}")
    public ApiResponse<VetQuestionResponse> edit(@AuthenticationPrincipal LoginMember me, @PathVariable Long id,
                                                 @Valid @RequestBody VetQuestionRequests.Edit request) {
        return ApiResponse.ok(service.edit(me.id(), id, request.content(), request.resolved()));
    }

    @DeleteMapping("/api/vet-questions/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal LoginMember me, @PathVariable Long id) {
        service.delete(me.id(), id);
        return ApiResponse.ok();
    }
}
