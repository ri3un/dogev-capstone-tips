package com.gaebalhagae.report.dto;

import com.gaebalhagae.record.dto.RecordResponses.WeightPoint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * PDF 리포트 섹션 9개 (결정 9·10). 순서: ①기본 정보 ②기간 요약 ③식사·간식 ④배변 ⑤증상 ⑥활동
 * ⑦체중(그래프 + 표) ⑧투약·접종·병원 ⑨질문 메모. 숫자는 서버가 집계하고 LLM 은 ② 3줄만 씁니다.
 */
public record ReportData(
        Basic basic,
        LocalDate from,
        LocalDate to,
        LocalDateTime generatedAt,
        List<String> summary,
        String summaryModel,
        List<String> signals,
        List<Row> meals,
        Map<String, Long> stoolCounts,
        List<Row> stools,
        List<Row> symptoms,
        int walkMinutesTotal,
        List<Row> activities,
        List<WeightPoint> weights,
        String weightChart,
        List<Row> medical,
        List<Question> questions
) {

    public record Basic(String name, String breed, String sex, boolean neutered, String age, BigDecimal weightKg,
                        String size, List<String> diseases, List<String> allergies) {
    }

    /** 기록 한 줄. by = 기록한 가족 닉네임 */
    public record Row(LocalDate date, String category, String memo, String by) {
    }

    public record Question(String content, LocalDate askedOn, boolean resolved) {
    }

    /** 미리보기 JSON 에는 그래프 이미지를 빼고 보냅니다(프론트가 weights 로 직접 그림) */
    public ReportData withoutChart() {
        return new ReportData(basic, from, to, generatedAt, summary, summaryModel, signals, meals, stoolCounts, stools,
                symptoms, walkMinutesTotal, activities, weights, null, medical, questions);
    }
}
