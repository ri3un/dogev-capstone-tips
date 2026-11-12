package com.gaebalhagae.dog.dto;

import java.math.BigDecimal;
import java.util.List;

/** 영양 카드: ①위험 음식 ②먹어도 되는 음식 ③적정 사료량 ④적정 음수량 ⑤질병별 주의점 */
public record NutritionCardResponse(
        Long dogId,
        BigDecimal weightKg,
        String lifeStage,
        int rerKcal,
        double factor,
        int derKcal,
        Integer foodKcalPerKg,
        Integer foodGrams,          // 사료 칼로리를 모르면 null
        int waterMinMl,
        int waterMaxMl,
        List<FoodItem> dangerFoods,
        List<FoodItem> safeFoods,
        List<FoodItem> cautionFoods,
        List<DiseaseItem> diseaseCautions,
        List<String> notices
) {

    public record FoodItem(String name, String reason, String howToFeed, String sourceUrl) {
    }

    public record DiseaseItem(String diseaseKey, String label, String cautionText, List<String> avoidKeywords,
                              String sourceUrl) {
    }
}
