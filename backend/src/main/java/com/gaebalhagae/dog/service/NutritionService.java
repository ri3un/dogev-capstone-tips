package com.gaebalhagae.dog.service;

import com.gaebalhagae.dog.domain.Dog;
import com.gaebalhagae.dog.domain.FoodGuide;
import com.gaebalhagae.dog.domain.FoodType;
import com.gaebalhagae.dog.dto.NutritionCardResponse;
import com.gaebalhagae.dog.dto.NutritionCardResponse.DiseaseItem;
import com.gaebalhagae.dog.dto.NutritionCardResponse.FoodItem;
import com.gaebalhagae.dog.repository.DiseaseGuideRepository;
import com.gaebalhagae.dog.repository.FoodGuideRepository;
import com.gaebalhagae.family.service.FamilyAccessService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 맞춤 영양 카드 ①~⑤ (결정 11). LLM 없이 고정 데이터 + 공식. 지병이 있으면 수의사 상의를 가장 먼저 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NutritionService {

    static final String NOT_DIAGNOSIS = "참고 정보이며 진단이 아닙니다. 양은 범위로 보고 반려견 상태에 맞춰 조절하세요.";
    static final String ASK_VET = "지병이 있어 사료량·간식을 바꾸기 전에 수의사와 먼저 상의하세요.";
    private static final String ALLERGY_REASON = "등록한 알레르기와 관련된 음식이라 피하는 것이 좋아요.";

    private final FamilyAccessService access;
    private final FoodGuideRepository foodGuideRepository;
    private final DiseaseGuideRepository diseaseGuideRepository;
    private final Clock clock;

    public NutritionCardResponse card(Long memberId, Long dogId) {
        Dog dog = access.checkDog(memberId, dogId);
        NutritionCalculator.Result calc = NutritionCalculator.calc(dog.getWeightKg().doubleValue(),
                dog.ageMonths(LocalDate.now(clock)), dog.isNeutered(), dog.getFoodKcalPerKg());

        List<FoodItem> danger = new ArrayList<>();
        List<FoodItem> caution = new ArrayList<>();
        List<FoodItem> safe = new ArrayList<>();
        for (FoodGuide f : foodGuideRepository.findAllByOrderByIdAsc()) {
            FoodItem item = new FoodItem(f.getName(), f.getReason(), f.getHowToFeed(), f.getSourceUrl());
            if (f.getType() == FoodType.DANGER) {
                danger.add(item);
            } else if (f.getType() == FoodType.CAUTION) {
                caution.add(item);
            } else if (isAllergen(dog, f.getName())) {
                caution.add(new FoodItem(f.getName(), ALLERGY_REASON, null, f.getSourceUrl()));
            } else {
                safe.add(item);
            }
        }

        List<String> keys = dog.getDiseases().stream().map(d -> d.toUpperCase(Locale.ROOT)).toList();
        List<DiseaseItem> diseases = keys.isEmpty() ? List.of() : diseaseGuideRepository.findByDiseaseKeyIn(keys)
                .stream()
                .map(g -> new DiseaseItem(g.getDiseaseKey(), g.getLabel(), g.getCautionText(),
                        List.copyOf(g.getAvoidKeywords()), g.getSourceUrl()))
                .toList();

        List<String> notices = new ArrayList<>();
        if (dog.hasDisease()) {
            notices.add(ASK_VET);
        }
        notices.add(NOT_DIAGNOSIS);

        return new NutritionCardResponse(dog.getId(), dog.getWeightKg(), calc.lifeStage(), calc.rerKcal(),
                calc.factor(), calc.derKcal(), dog.getFoodKcalPerKg(), calc.foodGrams(), calc.waterMinMl(),
                calc.waterMaxMl(), danger, safe, caution, diseases, notices);
    }

    private static boolean isAllergen(Dog dog, String foodName) {
        return dog.getAllergies().stream().anyMatch(a -> !a.isBlank() && foodName.contains(a.strip()));
    }
}
