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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.gaebalhagae.dog.domain.DiseaseGuide;
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
    static final String UNKNOWN_DISEASE = "등록된 안내가 없는 질환이에요. 식단은 수의사와 상의하세요.";
    private static final String OBESITY = "OBESITY";

    private final FamilyAccessService access;
    private final FoodGuideRepository foodGuideRepository;
    private final DiseaseGuideRepository diseaseGuideRepository;
    private final Clock clock;

    public NutritionCardResponse card(Long memberId, Long dogId) {
        Dog dog = access.checkDog(memberId, dogId);
        List<String> keys = dog.getDiseases().stream().map(d -> d.strip().toUpperCase(Locale.ROOT)).toList();
        List<DiseaseGuide> guides = keys.isEmpty() ? List.of() : diseaseGuideRepository.findByDiseaseKeyIn(keys);
        Map<String, String> labels = guides.stream()
                .collect(Collectors.toMap(DiseaseGuide::getDiseaseKey, DiseaseGuide::getLabel));
        boolean obese = labels.containsKey(OBESITY);
        NutritionCalculator.Result calc = NutritionCalculator.calc(dog.getWeightKg().doubleValue(),
                dog.ageMonths(LocalDate.now(clock)), dog.isNeutered(), dog.getFoodKcalPerKg(), obese);

        List<FoodItem> danger = new ArrayList<>();
        List<FoodItem> caution = new ArrayList<>();
        List<FoodItem> safe = new ArrayList<>();
        for (FoodGuide f : foodGuideRepository.findAllByOrderByIdAsc()) {
            FoodItem item = new FoodItem(f.getName(), f.getReason(), f.getHowToFeed(), f.getSourceUrl(), null);
            Optional<String> disease = f.avoidDiseaseKeys().stream().filter(labels::containsKey).findFirst();
            if (f.getType() == FoodType.DANGER) {
                danger.add(item);
            } else if (f.getType() == FoodType.CAUTION) {
                caution.add(item);
            } else if (isAllergen(dog, f.getName())) {
                caution.add(new FoodItem(f.getName(), ALLERGY_REASON, null, f.getSourceUrl(), "ALLERGY"));
            } else if (disease.isPresent()) {
                caution.add(new FoodItem(f.getName(), labels.get(disease.get()) + "이 있어 조금만 주세요",
                        f.getHowToFeed(), f.getSourceUrl(), "DISEASE"));
            } else {
                safe.add(item);
            }
        }

        List<DiseaseItem> diseases = guides.stream()
                .map(g -> new DiseaseItem(g.getDiseaseKey(), g.getLabel(), g.getCautionText(),
                        List.copyOf(g.getAvoidKeywords()), g.getSourceUrl()))
                .toList();
        List<String> unknown = dog.getDiseases().stream().map(String::strip)
                .filter(d -> !d.isEmpty() && !labels.containsKey(d.toUpperCase(Locale.ROOT))).toList();

        List<String> notices = new ArrayList<>();
        if (dog.hasDisease()) {
            notices.add(ASK_VET);
        }
        if (!unknown.isEmpty()) {
            notices.add(UNKNOWN_DISEASE);
        }
        notices.add(NOT_DIAGNOSIS);

        return new NutritionCardResponse(dog.getId(), dog.getWeightKg(), calc.lifeStage(), calc.rerKcal(),
                calc.factor(), calc.derKcal(), dog.getFoodKcalPerKg(), calc.foodGrams(), calc.foodGramsEstimated(),
                obese, calc.waterMinMl(), calc.waterMaxMl(), danger, safe, caution, diseases, unknown, notices);
    }

    private static boolean isAllergen(Dog dog, String foodName) {
        return dog.getAllergies().stream().anyMatch(a -> !a.isBlank() && foodName.contains(a.strip()));
    }
}
