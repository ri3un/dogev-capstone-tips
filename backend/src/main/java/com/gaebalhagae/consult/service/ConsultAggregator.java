package com.gaebalhagae.consult.service;

import com.gaebalhagae.consult.domain.Urgency;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 여러 모델의 답을 하나로 합칩니다 (결정 14). LLM 없이 규칙으로:
 * 긴급도는 가장 높은 값, 병원 권고는 하나라도 있으면, 종합 3줄, 응답 1개면 "교차검증 안 됨".
 */
public final class ConsultAggregator {

    static final String NOT_DIAGNOSIS = "참고 정보이며 진단이 아닙니다. 걱정되면 동물병원에 문의하세요.";
    static final String EMERGENCY = "응급 증상일 수 있어요. 답변보다 먼저, 즉시 동물병원에 연락하거나 방문하세요.";
    static final String NOT_CROSS_CHECKED = "답한 AI가 1개뿐이라 교차검증이 되지 않았어요.";

    private ConsultAggregator() {
    }

    public record ModelAnswer(Urgency urgency, boolean visitVet, String answer, List<String> reasons,
                              List<String> homeCare) {
    }

    public record Result(Urgency urgency, boolean visitVet, boolean crossChecked, List<String> summary,
                         List<String> notices) {
    }

    public static Result aggregate(List<ModelAnswer> answers, boolean emergency) {
        Urgency max = answers.stream().map(ModelAnswer::urgency).max(Comparator.naturalOrder()).orElse(Urgency.LOW);
        if (emergency) {
            max = Urgency.EMERGENCY;
        }
        long vetCount = answers.stream().filter(a -> a.visitVet() || a.urgency().compareTo(Urgency.HIGH) >= 0)
                .count();
        boolean visitVet = emergency || vetCount > 0;
        boolean crossChecked = answers.size() >= 2;

        // 첫 줄은 긴급도·병원 권고와 어긋나지 않게: 하나라도 진료를 권하면 "지켜봐도 된다"고 쓰지 않음
        List<String> summary = new ArrayList<>();
        if (emergency) {
            summary.add("질문에 응급일 수 있는 표현이 있어 바로 진료를 권해요.");
        } else if (vetCount > 0) {
            summary.add("답한 AI " + answers.size() + "개 중 " + vetCount + "개가 동물병원 진료를 권했어요.");
        } else {
            summary.add(answers.size() == 1 ? "답한 AI는 집에서 지켜봐도 된다고 봤어요."
                    : "답한 AI " + answers.size() + "개 모두 집에서 지켜봐도 된다고 봤어요.");
        }
        summary.add("가장 높은 긴급도는 '" + max.label() + "'이에요.");
        List<String> reasons = answers.stream().flatMap(a -> a.reasons().stream()).filter(Objects::nonNull)
                .map(String::strip).filter(s -> !s.isEmpty()).distinct().limit(2).toList();
        summary.add(reasons.isEmpty() ? "자세한 내용은 모델별 답변을 확인하세요."
                : "주요 근거: " + String.join(", ", reasons));

        List<String> notices = new ArrayList<>();
        if (emergency) {
            notices.add(EMERGENCY);
        }
        if (!crossChecked) {
            notices.add(NOT_CROSS_CHECKED);
        }
        notices.add(NOT_DIAGNOSIS);
        return new Result(max, visitVet, crossChecked, summary, notices);
    }
}
