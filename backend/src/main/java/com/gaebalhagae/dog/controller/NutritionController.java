package com.gaebalhagae.dog.controller;

import com.gaebalhagae.dog.dto.NutritionCardResponse;
import com.gaebalhagae.dog.service.NutritionService;
import com.gaebalhagae.global.response.ApiResponse;
import com.gaebalhagae.global.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NutritionController {

    private final NutritionService nutritionService;

    @GetMapping("/api/dogs/{dogId}/nutrition-card")
    public ApiResponse<NutritionCardResponse> card(@AuthenticationPrincipal LoginMember me, @PathVariable Long dogId) {
        return ApiResponse.ok(nutritionService.card(me.id(), dogId));
    }
}
