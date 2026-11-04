package com.gaebalhagae.place.review;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * LLM이 판정하는 장소 조건 9개 (멘토피드백_반영 2-2, 결정 #8). 10번째 분위기는 자유 태그라 따로 받습니다.
 * 값마다 화면에 보여 줄 고정 키워드를 정해 두고, 값이 unknown이면 키워드를 만들지 않습니다.
 * 나중에 place_condition.condition_key 가 됩니다.
 */
public enum ConditionKey {

    SIZE("size", "입장 가능 크기", tags("small_only", "#소형견만", "medium", "#중형견까지", "large", "#대형견가능")),
    SPACE("space", "동반 공간", tags("indoor", "#실내동반", "outdoor_only", "#야외만동반")),
    CARRIER("carrier", "이동장·유모차 필요", tags("required", "#이동장필요", "not_required", "#이동장없이")),
    LEASH("leash", "목줄 필수", tags("required", "#목줄필수", "not_required", "#목줄자율")),
    MANNER_BELT("manner_belt", "매너벨트·기저귀 필요", tags("required", "#매너벨트필요", "not_required", "#매너벨트없이")),
    SEAT("seat", "반려견 의자·방석 착석", tags("allowed", "#의자착석가능", "not_allowed", "#의자착석불가")),
    OFF_LEASH("off_leash", "오프리쉬 공간", tags("yes", "#오프리쉬공간", "no", "#오프리쉬없음")),
    PET_MENU("pet_menu", "물그릇·펫 메뉴", tags("yes", "#펫메뉴", "no", "#펫메뉴없음")),
    PARKING("parking", "주차", tags("allowed", "#주차가능", "not_allowed", "#주차불가"));

    public static final String UNKNOWN = "unknown";

    private final String key;
    private final String label;
    private final Map<String, String> valueToTag;   // 값 → 고정 키워드 (순서 유지)

    ConditionKey(String key, String label, Map<String, String> valueToTag) {
        this.key = key;
        this.label = label;
        this.valueToTag = valueToTag;
    }

    public String key() {
        return key;
    }

    /** 허용 값인지 확인 (unknown 포함) */
    public boolean accepts(String value) {
        return UNKNOWN.equals(value) || valueToTag.containsKey(value);
    }

    public Optional<String> tagOf(String value) {
        return Optional.ofNullable(valueToTag.get(value));
    }

    /** 프롬프트에 넣을 한 줄. 예: size(입장 가능 크기): small_only | medium | large | unknown */
    String promptLine() {
        return key + "(" + label + "): " + String.join(" | ", valueToTag.keySet()) + " | " + UNKNOWN;
    }

    static String promptLines() {
        return String.join("\n", Arrays.stream(values()).map(ConditionKey::promptLine).toList());
    }

    private static Map<String, String> tags(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }
}
