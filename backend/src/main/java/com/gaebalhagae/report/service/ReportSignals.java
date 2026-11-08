package com.gaebalhagae.report.service;

import com.gaebalhagae.record.dto.RecordResponses.WeightPoint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 리포트 이상 신호 규칙 3개. 숫자는 서버가 계산하고 LLM 은 쓰지 않습니다 (결정 9·10) */
public final class ReportSignals {

    private ReportSignals() {
    }

    public static List<String> of(List<WeightPoint> weights, List<LocalDate> looseStoolDates, int symptomCount) {
        List<String> out = new ArrayList<>();
        if (weights.size() >= 2) {
            BigDecimal first = weights.get(0).weightKg();
            BigDecimal last = weights.get(weights.size() - 1).weightKg();
            // 판정은 반올림 전 값으로(4.75%는 신호 아님), 화면 표시만 정수로 반올림
            BigDecimal pct = last.subtract(first).multiply(BigDecimal.valueOf(100))
                    .divide(first, 4, RoundingMode.HALF_UP);
            if (pct.abs().compareTo(BigDecimal.valueOf(5)) >= 0) {
                out.add("기간 중 체중이 %s%% %s어요 (%s → %skg)".formatted(
                        pct.abs().setScale(0, RoundingMode.HALF_UP), pct.signum() > 0 ? "늘었" : "줄었",
                        first.stripTrailingZeros().toPlainString(), last.stripTrailingZeros().toPlainString()));
            }
        }
        List<LocalDate> sorted = looseStoolDates.stream().sorted().toList();
        for (int i = 0; i + 1 < sorted.size(); i++) {
            if (!sorted.get(i + 1).isAfter(sorted.get(i).plusDays(2))) {
                out.add("3일 안에 설사·무른 변이 2회 이상 있었어요");
                break;
            }
        }
        if (symptomCount >= 3) {
            out.add("증상 기록이 " + symptomCount + "회 있었어요");
        }
        return out;
    }
}
