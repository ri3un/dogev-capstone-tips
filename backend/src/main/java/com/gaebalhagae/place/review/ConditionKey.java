package com.gaebalhagae.place.review;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * LLM이 문장마다 판정하는 장소 조건 9개 (결정 #8, 기능별_구현방법 5-3). 10번째 분위기(mood_tags)는 자유 태그라 따로 받습니다.
 * 키·값 이름은 설계 문서의 프롬프트와 같고, 나중에 place_condition.condition_key 가 됩니다.
 * 값마다 화면에 보여 줄 고정 키워드를 정해 두고, unknown·NEEDS_CHECK 이면 키워드를 만들지 않습니다(결정 #33).
 */
public enum ConditionKey {

    SIZE_ALLOWED("size_allowed", "입장 가능 크기",
            tags("small_only", "#소형견만", "up_to_medium", "#중형견까지", "up_to_large", "#대형견가능")),
    INDOOR("indoor", "동반 공간", tags("indoor", "#실내동반", "outdoor_only", "#야외만동반")),
    CARRIER_REQUIRED("carrier_required", "이동장·유모차 필요", tags("yes", "#이동장필요", "no", "#이동장없이")),
    LEASH_REQUIRED("leash_required", "목줄 필수", tags("yes", "#목줄필수", "no", "#목줄자율")),
    MANNER_BELT_REQUIRED("manner_belt_required", "매너벨트·기저귀 필요", tags("yes", "#매너벨트필요", "no", "#매너벨트없이")),
    SEAT_ALLOWED("seat_allowed", "반려견 의자·방석 착석", tags("yes", "#의자착석가능", "no", "#의자착석불가")),
    OFF_LEASH_AREA("off_leash_area", "오프리쉬(목줄 풀기) 공간", tags("yes", "#오프리쉬공간", "no", "#오프리쉬없음")),
    WATER_OR_PET_MENU("water_or_pet_menu", "물그릇·펫 메뉴", tags("yes", "#물그릇펫메뉴", "no", "#펫메뉴없음")),
    PARKING("parking", "주차", tags("yes", "#주차가능", "no", "#주차불가"));

    public static final String UNKNOWN = "unknown";
    public static final String NEEDS_CHECK = "NEEDS_CHECK";   // 판정이 엇갈려 다수결 60%를 넘지 못함

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

    public String label() {
        return label;
    }

    /** 고정 키워드(#대형견가능 등) 전체. 분위기 태그와 구분할 때 씁니다 */
    public java.util.Collection<String> tags() {
        return valueToTag.values();
    }

    public static Optional<ConditionKey> fromKey(String key) {
        return Arrays.stream(values()).filter(c -> c.key.equals(key)).findFirst();
    }

    /** unknown 이 아닌 허용 값인지 확인 */
    public boolean isKnownValue(String value) {
        return valueToTag.containsKey(value);
    }

    public Optional<String> tagOf(String value) {
        return Optional.ofNullable(valueToTag.get(value));
    }

    /** 프롬프트에 넣을 한 줄. 예: size_allowed(입장 가능 크기): small_only | up_to_medium | up_to_large | unknown */
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
