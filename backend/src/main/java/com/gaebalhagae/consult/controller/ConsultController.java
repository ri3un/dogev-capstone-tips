package com.gaebalhagae.consult.controller;

import com.gaebalhagae.consult.dto.ConsultResponses;
import com.gaebalhagae.consult.service.ConsultService;
import com.gaebalhagae.global.response.ApiResponse;
import com.gaebalhagae.global.security.LoginMember;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ConsultController {

    private final ConsultService consultService;

    /** 무료 LLM 3개 교차검증 상담. 최대 약 15초 걸립니다 */
    @PostMapping("/api/dogs/{dogId}/consultations")
    public ApiResponse<ConsultResponses.Detail> ask(@AuthenticationPrincipal LoginMember me, @PathVariable Long dogId,
                                                    @Valid @RequestBody ConsultResponses.Ask request) {
        return ApiResponse.ok(consultService.ask(me.id(), dogId, request.question()));
    }

    @GetMapping("/api/dogs/{dogId}/consultations")
    public ApiResponse<List<ConsultResponses.Item>> list(@AuthenticationPrincipal LoginMember me,
                                                         @PathVariable Long dogId) {
        return ApiResponse.ok(consultService.list(me.id(), dogId));
    }

    @GetMapping("/api/consultations/{id}")
    public ApiResponse<ConsultResponses.Detail> detail(@AuthenticationPrincipal LoginMember me, @PathVariable Long id) {
        return ApiResponse.ok(consultService.detail(me.id(), id));
    }
}
