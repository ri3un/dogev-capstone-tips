package com.gaebalhagae.consult.service;

import com.gaebalhagae.record.domain.HealthRecord;
import com.gaebalhagae.record.domain.RecordCategory;
import com.gaebalhagae.record.dto.RecordResponses.WeightPoint;
import com.gaebalhagae.record.repository.HealthRecordRepository;
import com.gaebalhagae.report.service.ReportSignals;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 상담에 함께 보낼 최근 14일 기록 집계 (항목 8). 숫자만 보내고 보호자가 쓴 기록 원문(memo)은 보내지 않음.
 * 빈 목록 = 기록 없음 → 프롬프트·화면 모두 생략.
 */
@Component
@RequiredArgsConstructor
public class RecentContextBuilder {

    static final int DAYS = 14;

    private final HealthRecordRepository recordRepository;

    public List<String> build(Long dogId, LocalDate today) {
        return lines(recordRepository.findByDogIdAndRecordDateBetweenOrderByRecordDateAscIdAsc(dogId,
                today.minusDays(DAYS - 1), today), today);
    }

    public static List<String> lines(List<HealthRecord> records, LocalDate today) {
        if (records.isEmpty()) {
            return List.of();
        }
        Map<RecordCategory, Long> count = new EnumMap<>(RecordCategory.class);
        records.forEach(r -> count.merge(r.getCategory(), 1L, Long::sum));
        List<LocalDate> looseDates = records.stream()
                .filter(r -> r.getStoolStatus() != null && r.getStoolStatus().isLoose())
                .map(HealthRecord::getRecordDate).toList();
        LocalDate lastSymptom = records.stream().filter(r -> r.getCategory() == RecordCategory.SYMPTOM)
                .map(HealthRecord::getRecordDate).max(Comparator.naturalOrder()).orElse(null);
        int walk = records.stream().map(HealthRecord::getWalkMinutes).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).sum();
        List<WeightPoint> weights = records.stream()
                .filter(r -> r.getCategory() == RecordCategory.WEIGHT && r.getWeightKg() != null)
                .sorted(Comparator.comparing(HealthRecord::getRecordDate))
                .map(r -> new WeightPoint(r.getRecordDate(), r.getWeightKg())).toList();

        List<String> parts1 = new ArrayList<>();
        add(parts1, count, RecordCategory.MEAL, "식사 %d회");
        add(parts1, count, RecordCategory.SNACK, "간식 %d회");
        if (count.containsKey(RecordCategory.STOOL)) {
            parts1.add(looseDates.isEmpty() ? "배변 %d회".formatted(count.get(RecordCategory.STOOL))
                    : "배변 %d회(무른 변 %d회)".formatted(count.get(RecordCategory.STOOL), looseDates.size()));
        }
        if (lastSymptom != null) {
            parts1.add("증상 %d회(마지막 %d월 %d일)".formatted(count.get(RecordCategory.SYMPTOM),
                    lastSymptom.getMonthValue(), lastSymptom.getDayOfMonth()));
        }
        List<String> parts2 = new ArrayList<>();
        add(parts2, count, RecordCategory.MEDICATION, "투약·접종 %d회");
        add(parts2, count, RecordCategory.HOSPITAL, "병원 %d회");
        if (walk > 0) {
            parts2.add("산책·활동 총 %d분(하루 평균 %d분)".formatted(walk, Math.round(walk / (double) DAYS)));
        }

        List<String> out = new ArrayList<>();
        if (!parts1.isEmpty()) {
            out.add(String.join(", ", parts1));
        }
        if (!parts2.isEmpty()) {
            out.add(String.join(", ", parts2));
        }
        if (!weights.isEmpty()) {
            String first = weights.get(0).weightKg().stripTrailingZeros().toPlainString();
            String last = weights.get(weights.size() - 1).weightKg().stripTrailingZeros().toPlainString();
            out.add(weights.size() == 1 ? "체중 " + last + "kg" : "체중 " + first + " → " + last + "kg");
        }
        out.addAll(ReportSignals.of(weights, looseDates, count.getOrDefault(RecordCategory.SYMPTOM, 0L).intValue()));
        return out;
    }

    private static void add(List<String> parts, Map<RecordCategory, Long> count, RecordCategory c, String fmt) {
        if (count.containsKey(c)) {
            parts.add(fmt.formatted(count.get(c)));
        }
    }
}
