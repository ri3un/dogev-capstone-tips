package com.gaebalhagae.place.review;

import java.util.List;

/**
 * 조건 하나의 다수결 결과. 나중에 place_condition 한 행이 됩니다.
 * 화면의 "블로그 5건 중 4건"이 evidenceCount=5, positiveCount=4 입니다.
 */
public record ConditionResult(
        String value,             // 다수결로 정한 값, 근거가 없으면 unknown, 엇갈리면 NEEDS_CHECK
        Integer weightLimitKg,    // size_allowed 에서 몸무게 제한을 알면 (예: 10)
        int evidenceCount,        // unknown 이 아닌 판정 수
        int positiveCount,        // 그중 value 와 같은 판정 수
        List<String> evidence     // value 를 뒷받침하는 문장 원문
) {

    static ConditionResult unknown() {
        return new ConditionResult(ConditionKey.UNKNOWN, null, 0, 0, List.of());
    }
}
