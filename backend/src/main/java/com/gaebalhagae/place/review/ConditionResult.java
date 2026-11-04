package com.gaebalhagae.place.review;

import java.util.List;

/**
 * 조건 하나의 판정 결과. 나중에 place_condition 한 행이 됩니다.
 * evidence에는 근거 문장 원문을 넣어 두어 화면의 "근거 n건"과 출처 확인에 씁니다.
 */
public record ConditionResult(
        String value,             // 허용 값 중 하나, 근거가 없으면 unknown
        Integer weightLimitKg,    // size 조건에서 몸무게 제한을 알면 (예: 10)
        List<String> evidence     // 근거 문장 (evidence_count = 크기)
) {

    static ConditionResult unknown() {
        return new ConditionResult(ConditionKey.UNKNOWN, null, List.of());
    }
}
