package com.gaebalhagae.dog.service;

/**
 * 적정 사료량·음수량 공식 (결정 11).
 * RER = 70 × 몸무게^0.75, 하루 필요 열량(DER) = RER × 생애 단계 계수, 음수량은 kg당 30~60ml.
 */
public final class NutritionCalculator {

    private NutritionCalculator() {
    }

    /** 사료 봉투 칼로리를 모를 때 쓰는 일반 건사료 평균 */
    public static final int DEFAULT_FOOD_KCAL_PER_KG = 3500;

    public record Result(int rerKcal, double factor, String lifeStage, int derKcal, int foodGrams,
                         boolean foodGramsEstimated, int waterMinMl, int waterMaxMl) {
    }

    /** obese 면 감량 기준 계수 1.0 (일반 수의 가이드 값) */
    public static Result calc(double weightKg, int ageMonths, boolean neutered, Integer foodKcalPerKg,
                              boolean obese) {
        double rer = 70 * Math.pow(weightKg, 0.75);
        double factor;
        String stage;
        if (obese) {
            factor = 1.0;
            stage = "비만 감량 기준";
        } else if (ageMonths < 4) {
            factor = 3.0;
            stage = "4개월 미만 강아지";
        } else if (ageMonths < 12) {
            factor = 2.0;
            stage = "1살 미만 강아지";
        } else if (ageMonths >= 84) {
            factor = 1.4;
            stage = "7살 이상 노령견";
        } else if (neutered) {
            factor = 1.6;
            stage = "중성화한 성견";
        } else {
            factor = 1.8;
            stage = "중성화하지 않은 성견";
        }
        double der = rer * factor;
        boolean estimated = foodKcalPerKg == null || foodKcalPerKg <= 0;
        int kcalPerKg = estimated ? DEFAULT_FOOD_KCAL_PER_KG : foodKcalPerKg;
        int food = (int) Math.round(der / kcalPerKg * 1000);
        return new Result((int) Math.round(rer), factor, stage, (int) Math.round(der), food, estimated,
                (int) Math.round(weightKg * 30), (int) Math.round(weightKg * 60));
    }
}
