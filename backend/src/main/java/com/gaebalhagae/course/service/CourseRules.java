package com.gaebalhagae.course.service;

import com.gaebalhagae.dog.domain.ActivityLevel;
import java.util.ArrayList;
import java.util.List;

/**
 * 기능 연결 B: 건강 정보 → 코스 기본 조건 (결정 5, 기능별_구현방법 추천안 4개). 화면에 이유 한 줄씩 보여 줍니다.
 * 1) 8세 이상: 구간 1km, 총 2km  2) 관절 지병·14일 내 "절뚝": 총 2km, 오프리쉬 제외
 * 3) 3일 내 설사·무른 변 2회 이상: 식당 대신 산책로·카페, 총 1.5km  4) 활동량 높음 + 지병 없음: 오프리쉬 우선
 */
public record CourseRules(Integer maxLegM, Integer maxTotalM, boolean excludeOffLeash, boolean preferOffLeash,
                          boolean replaceRestaurant, List<String> reasons) {

    public record Input(int ageMonths, List<String> diseases, ActivityLevel activity, List<String> recentSymptomMemos,
                        long looseStools3d) {
    }

    public static CourseRules of(Input in) {
        Integer maxLeg = null;
        Integer maxTotal = null;
        boolean excludeOffLeash = false;
        boolean preferOffLeash = false;
        boolean replaceRestaurant = false;
        List<String> reasons = new ArrayList<>();

        if (in.ageMonths() >= 96) {
            maxLeg = 1000;
            maxTotal = 2000;
            reasons.add("노령견이라 2km 이내로 추천했어요");
        }
        boolean jointDisease = in.diseases().stream().anyMatch(d -> d.equalsIgnoreCase("JOINT") || d.contains("관절"));
        boolean limping = in.recentSymptomMemos().stream().anyMatch(m -> m != null && m.contains("절뚝"));
        if (jointDisease || limping) {
            maxTotal = min(maxTotal, 2000);
            excludeOffLeash = true;
            reasons.add(jointDisease ? "관절 지병이 있어 2km 이내로, 뛰는 곳은 빼고 추천했어요"
                    : "최근 관절 메모가 있어 2km 이내로 추천했어요");
        }
        if (in.looseStools3d() >= 2) {
            maxTotal = min(maxTotal, 1500);
            replaceRestaurant = true;
            reasons.add("최근 배변 상태가 좋지 않아 짧은 코스로 추천했어요");
        }
        if (in.activity() == ActivityLevel.HIGH && in.diseases().isEmpty() && !excludeOffLeash) {
            preferOffLeash = true;
            reasons.add("활동량이 많아 뛰어놀 수 있는 곳을 넣었어요");
        }
        return new CourseRules(maxLeg, maxTotal, excludeOffLeash, preferOffLeash, replaceRestaurant, reasons);
    }

    private static Integer min(Integer a, int b) {
        return a == null ? b : Math.min(a, b);
    }
}
