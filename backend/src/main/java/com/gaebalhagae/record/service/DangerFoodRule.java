package com.gaebalhagae.record.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 위험 음식 경고는 LLM 이 아니라 이 서버 규칙으로만 냅니다 (결정 5, 기능별_구현방법).
 * "술"처럼 다른 낱말(수술·기술) 안에 들어가는 짧은 단어는 쓰지 않고 구체적인 이름만 찾습니다.
 */
@Component
public class DangerFoodRule {

    public record Warning(String keyword, String food, String message) {
    }

    private static final Map<String, String> KEYWORDS = new LinkedHashMap<>();

    static {
        put("초콜릿", "초콜릿", "초코");
        put("포도·건포도", "포도", "건포도");
        put("양파·마늘·부추", "양파", "마늘", "부추");
        put("자일리톨", "자일리톨");
        put("마카다미아", "마카다미아");
        put("커피·차·에너지음료", "커피", "에너지음료", "에너지 음료");
        put("술", "소주", "맥주", "와인", "막걸리", "위스키");
        put("아보카도", "아보카도");
        put("익힌 뼈", "닭뼈", "생선뼈", "갈비뼈", "족발뼈");
    }

    private static void put(String food, String... keywords) {
        for (String k : keywords) {
            KEYWORDS.put(k, food);
        }
    }

    public List<Warning> check(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<Warning> warnings = new ArrayList<>();
        KEYWORDS.forEach((keyword, food) -> {
            if (text.contains(keyword) && seen.add(food)) {
                warnings.add(new Warning(keyword, food, food
                        + "은(는) 반려견에게 위험한 음식이에요. 먹은 양과 시간을 기록하고 동물병원에 바로 문의하세요."));
            }
        });
        return warnings;
    }
}
