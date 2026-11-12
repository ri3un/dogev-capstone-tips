package com.gaebalhagae.dog.service;

import static com.gaebalhagae.dog.domain.FoodType.CAUTION;
import static com.gaebalhagae.dog.domain.FoodType.DANGER;
import static com.gaebalhagae.dog.domain.FoodType.SAFE;

import com.gaebalhagae.dog.domain.DiseaseGuide;
import com.gaebalhagae.dog.domain.FoodGuide;
import com.gaebalhagae.dog.repository.DiseaseGuideRepository;
import com.gaebalhagae.dog.repository.FoodGuideRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 영양 카드 고정 데이터를 테이블이 비어 있을 때 한 번 넣습니다.
 * 음식 목록은 ASPCA "People Foods to Avoid Feeding Your Pets" 기준. 질병별 주의점은 일반 관리 수칙이며 수의사 상담이 우선입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuideDataLoader implements ApplicationRunner {

    static final String ASPCA = "https://www.aspca.org/pet-care/animal-poison-control/people-foods-avoid-feeding-your-pets";

    private final FoodGuideRepository foodGuideRepository;
    private final DiseaseGuideRepository diseaseGuideRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (foodGuideRepository.count() == 0) {
            foodGuideRepository.saveAll(foods());
            log.info("food_guide 고정 데이터 {}건 적재", foodGuideRepository.count());
        }
        if (diseaseGuideRepository.count() == 0) {
            diseaseGuideRepository.saveAll(diseases());
        }
    }

    private static List<FoodGuide> foods() {
        return List.of(
                new FoodGuide("초콜릿", DANGER, "테오브로민·카페인 때문에 구토, 떨림, 경련이 올 수 있어요.", null, ASPCA),
                new FoodGuide("포도·건포도", DANGER, "적은 양으로도 급성 신장 손상이 생길 수 있어요.", null, ASPCA),
                new FoodGuide("양파·마늘·부추", DANGER, "적혈구를 손상시켜 빈혈을 일으킬 수 있어요. 익혀도 위험해요.", null, ASPCA),
                new FoodGuide("자일리톨", DANGER, "껌·사탕·일부 땅콩버터에 들어 있어요. 저혈당과 간 손상 위험이 있어요.", null, ASPCA),
                new FoodGuide("마카다미아", DANGER, "무기력, 구토, 떨림, 고열이 생길 수 있어요.", null, ASPCA),
                new FoodGuide("커피·차·에너지음료", DANGER, "카페인이 흥분, 빠른 심박, 떨림을 일으킬 수 있어요.", null, ASPCA),
                new FoodGuide("술", DANGER, "구토, 호흡 저하, 의식 저하가 생길 수 있어요.", null, ASPCA),
                new FoodGuide("아보카도", DANGER, "구토·설사를 일으킬 수 있고 씨는 장을 막을 수 있어요.", null, ASPCA),
                new FoodGuide("익힌 뼈", DANGER, "쪼개진 조각이 입·식도·장을 다치게 할 수 있어요.", null, ASPCA),
                new FoodGuide("우유·유제품", CAUTION, "유당을 잘 소화하지 못해 설사를 할 수 있어요.", null, ASPCA),
                new FoodGuide("땅콩버터", CAUTION, "자일리톨이 없는 제품만, 아주 조금만 주세요.", null, ASPCA),
                new FoodGuide("견과류", CAUTION, "지방이 많아 배탈이나 췌장염 위험이 있어요.", null, ASPCA),
                new FoodGuide("짠 음식", CAUTION, "나트륨이 많아 구토·설사, 심하면 중독이 생길 수 있어요.", null, ASPCA),
                new FoodGuide("생고기·날달걀", CAUTION, "살모넬라·대장균 같은 세균 위험이 있어요.", null, ASPCA),
                new FoodGuide("튀김·기름진 음식", CAUTION, "지방이 많아 배탈이나 췌장염 위험이 있어요.", null, null),
                new FoodGuide("당근", SAFE, "열량이 낮고 씹는 재미가 있어요.", "잘게 썰어 생으로 또는 익혀서. 간식은 하루 열량의 10% 이내", null),
                new FoodGuide("사과", SAFE, "비타민과 식이섬유가 있어요.", "씨와 심은 반드시 빼고 작게 잘라서", null),
                new FoodGuide("블루베리", SAFE, "열량이 낮은 간식이에요.", "작은 강아지는 반으로 잘라서 몇 알만", null),
                new FoodGuide("고구마", SAFE, "식이섬유가 많아요.", "껍질을 벗기고 익혀서 조금만. 많이 주면 살이 쪄요", null),
                new FoodGuide("닭가슴살", SAFE, "기름기가 적은 단백질이에요.", "양념 없이 완전히 익히고 뼈를 빼서", null),
                new FoodGuide("오이", SAFE, "수분이 많고 열량이 아주 낮아요.", "얇게 썰어서", null),
                new FoodGuide("수박", SAFE, "수분이 많은 여름 간식이에요.", "씨와 껍질을 빼고 과육만 조금", null),
                new FoodGuide("단호박", SAFE, "식이섬유가 있어 장에 도움이 될 수 있어요.", "양념 없이 익혀서 조금만", null),
                new FoodGuide("바나나", SAFE, "칼륨이 있지만 당분이 많아요.", "가끔 한두 조각만", null));
    }

    private static List<DiseaseGuide> diseases() {
        return List.of(
                new DiseaseGuide("KIDNEY", "신장", "인·나트륨이 많은 음식을 줄이고 물을 충분히 마시게 해 주세요. 처방식 여부는 수의사와 상의하세요.",
                        List.of("치즈", "가공육", "육포", "짠 간식"), null),
                new DiseaseGuide("PANCREATITIS", "췌장염", "기름진 음식이 재발 원인이 될 수 있어요. 지방이 적은 식단을 유지하세요.",
                        List.of("튀김", "삼겹살", "버터", "견과류", "땅콩버터"), null),
                new DiseaseGuide("OBESITY", "비만", "간식은 하루 열량의 10% 이내로, 사료는 계량해서 주세요. 감량 속도는 수의사와 정하세요.",
                        List.of("빵", "튀김", "치즈", "고구마 과다"), null),
                new DiseaseGuide("JOINT", "관절", "체중을 관리하고 점프·계단을 줄여 주세요. 산책은 짧게 여러 번이 좋아요.",
                        List.of("고열량 간식"), null),
                new DiseaseGuide("SKIN", "피부", "새 간식은 한 번에 하나씩 시험하고, 가려움이 생기면 그 음식을 기록해 두세요.",
                        List.of("새 단백질 간식"), null));
    }
}
