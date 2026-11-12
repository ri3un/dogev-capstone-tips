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
        int foodGrams,
        boolean foodGramsEstimated, // 사료 칼로리를 몰라 일반 건사료 3,500kcal/kg 로 계산
        boolean weightLoss,         // 비만이라 감량 기준 사료량
        int waterMinMl,
        int waterMaxMl,
        List<FoodItem> dangerFoods,
        List<FoodItem> safeFoods,
        List<FoodItem> cautionFoods,
        List<DiseaseItem> diseaseCautions,
        List<String> unknownDiseases, // 등록된 안내가 없는 자유 입력 지병
        List<String> notices
) {

    /** reasonTag: 원래 먹어도 되는 음식인데 ALLERGY·DISEASE 때문에 주의로 옮겼으면 그 이유, 아니면 null */
    public record FoodItem(String name, String reason, String howToFeed, String sourceUrl, String reasonTag) {
    }

    public record DiseaseItem(String diseaseKey, String label, String cautionText, List<String> avoidKeywords,
                              String sourceUrl) {
    }
}
