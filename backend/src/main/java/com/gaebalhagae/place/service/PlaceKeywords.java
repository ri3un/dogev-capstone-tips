package com.gaebalhagae.place.service;

import com.gaebalhagae.place.review.ConditionKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 화면 키워드 = 확정된 조건의 고정 태그(조건 순서) + 분위기 태그 최대 3개, 합쳐서 5개까지 (결정 33) */
public final class PlaceKeywords {

    static final int MAX = 5;
    static final int MAX_MOOD = 3;

    private static final Set<String> FIXED = Arrays.stream(ConditionKey.values())
            .flatMap(k -> k.tags().stream()).collect(Collectors.toSet());

    private PlaceKeywords() {
    }

    public static List<String> build(Map<String, String> conditionValues, List<String> mood) {
        List<String> out = new ArrayList<>();
        for (ConditionKey k : ConditionKey.values()) {
            String value = conditionValues.get(k.key());
            if (value != null && out.size() < MAX) {
                k.tagOf(value).ifPresent(out::add);
            }
        }
        int moodCount = 0;
        for (String m : mood == null ? List.<String>of() : mood) {
            if (out.size() >= MAX || moodCount >= MAX_MOOD) {
                break;
            }
            if (m != null && !m.isBlank() && !FIXED.contains(m) && !out.contains(m)) {
                out.add(m);
                moodCount++;
            }
        }
        return out;
    }

    /** 저장된 키워드에서 분위기 태그만 골라냅니다 */
    public static List<String> moodOf(List<String> keywords) {
        return keywords == null ? List.of() : keywords.stream().filter(k -> !FIXED.contains(k)).toList();
    }
}
