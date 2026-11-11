package com.gaebalhagae.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.gaebalhagae.dog.domain.Dog;
import com.gaebalhagae.family.service.FamilyAccessService;
import com.gaebalhagae.global.response.BusinessException;
import com.gaebalhagae.global.response.ErrorCode;
import com.gaebalhagae.llm.LlmFallbackCaller;
import com.gaebalhagae.llm.LlmFormatException;
import com.gaebalhagae.llm.LlmJson;
import com.gaebalhagae.question.dto.VetQuestionResponse;
import com.gaebalhagae.question.service.VetQuestionService;
import com.gaebalhagae.record.domain.RecordCategory;
import com.gaebalhagae.record.domain.StoolStatus;
import com.gaebalhagae.record.dto.RecordResponses;
import com.gaebalhagae.record.service.RecordService;
import com.gaebalhagae.report.dto.ReportData;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 건강 기록 PDF 리포트 (결정 9·10). 기본 30일, 최대 92일.
 * 숫자·목록·이상 신호는 서버가 만들고, LLM 은 ② 기간 요약 3줄만(실패하면 규칙 문장). 이름 등은 LLM 에 보내지 않습니다.
 * LLM 을 기다리는 동안 트랜잭션을 잡지 않도록 이 클래스에는 @Transactional 을 두지 않습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    static final int DEFAULT_DAYS = 30;
    static final int MAX_DAYS = 92;
    static final int SUMMARY_TIMEOUT_SECONDS = 20;

    private static final String SYSTEM = """
            너는 반려견 건강 기록을 동물병원에 보여 줄 요약으로 정리하는 도우미야.
            주어진 숫자와 신호만 사용해서 {"summary":["문장1","문장2","문장3"]} JSON 하나로 답해.
            각 문장은 띄어쓰기를 지킨 자연스러운 존댓말 한 문장(50자 이내)으로 쓰고, 진단이나 처방은 쓰지 마.
            예: "30일 동안 식사 1건, 활동 2건을 기록했어요."
            """;

    private final FamilyAccessService access;
    private final RecordService recordService;
    private final VetQuestionService vetQuestionService;
    private final LlmFallbackCaller llm;
    private final PdfRenderer pdfRenderer;
    private final Clock clock;

    public ReportData preview(Long memberId, Long dogId, LocalDate from, LocalDate to) {
        return build(memberId, dogId, from, to, false).withoutChart();
    }

    public byte[] pdf(Long memberId, Long dogId, LocalDate from, LocalDate to) {
        return pdfRenderer.render(build(memberId, dogId, from, to, true));
    }

    private ReportData build(Long memberId, Long dogId, LocalDate from, LocalDate to, boolean withChart) {
        Dog dog = access.checkDog(memberId, dogId);
        LocalDate end = to == null ? LocalDate.now(clock) : to;
        LocalDate start = from == null ? end.minusDays(DEFAULT_DAYS - 1) : from;
        if (start.isAfter(end) || ChronoUnit.DAYS.between(start, end) >= MAX_DAYS) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "리포트 기간은 1일~92일로 지정해 주세요");
        }

        List<RecordResponses.Record> records = recordService.list(memberId, dogId, start, end, null);
        List<RecordResponses.WeightPoint> weights = recordService.weights(memberId, dogId, start, end);
        List<VetQuestionResponse> questions = vetQuestionService.list(memberId, dogId, false);

        List<RecordResponses.Record> stools = filter(records, Set.of(RecordCategory.STOOL));
        List<RecordResponses.Record> symptoms = filter(records, Set.of(RecordCategory.SYMPTOM));
        List<LocalDate> looseDates = stools.stream()
                .filter(r -> r.stoolStatus() != null && r.stoolStatus().isLoose())
                .map(RecordResponses.Record::recordDate).toList();
        List<String> signals = ReportSignals.of(weights, looseDates, symptoms.size());
        int walk = records.stream().map(RecordResponses.Record::walkMinutes).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).sum();

        Map<RecordCategory, Long> counts = new EnumMap<>(RecordCategory.class);
        records.forEach(r -> counts.merge(r.category(), 1L, Long::sum));
        Summary summary = summarize(counts, signals, weights, walk, ChronoUnit.DAYS.between(start, end) + 1);

        int months = dog.ageMonths(LocalDate.now(clock));
        ReportData.Basic basic = new ReportData.Basic(dog.getName(), dog.getBreed(),
                dog.getSex().name().equals("MALE") ? "수컷" : "암컷", dog.isNeutered(),
                months / 12 + "살 " + months % 12 + "개월", dog.getWeightKg(), sizeLabel(dog),
                List.copyOf(dog.getDiseases()), List.copyOf(dog.getAllergies()));

        return new ReportData(basic, start, end, LocalDateTime.now(clock), summary.lines(), summary.model(), signals,
                rows(filter(records, Set.of(RecordCategory.MEAL, RecordCategory.SNACK))),
                stoolCounts(stools),
                rows(stools.stream().filter(r -> r.stoolStatus() != StoolStatus.NORMAL).toList()),
                rows(symptoms), walk,
                rows(filter(records, Set.of(RecordCategory.ACTIVITY))),
                weights, withChart ? WeightChart.dataUri(weights) : null,
                rows(filter(records, Set.of(RecordCategory.MEDICATION, RecordCategory.HOSPITAL))),
                questions.stream().map(q -> new ReportData.Question(q.content(),
                        q.createdAt() == null ? null : q.createdAt().toLocalDate(), q.resolved())).toList());
    }

    private record Summary(List<String> lines, String model) {
    }

    private Summary summarize(Map<RecordCategory, Long> counts, List<String> signals,
                              List<RecordResponses.WeightPoint> weights, int walk, long days) {
        String facts = "기간 %d일, 기록 수 %s, 산책 총 %d분, 체중 %s, 이상 신호 %s".formatted(days, labelCounts(counts), walk,
                weights.isEmpty() ? "기록 없음" : weights.get(0).weightKg() + "kg → "
                        + weights.get(weights.size() - 1).weightKg() + "kg",
                signals.isEmpty() ? "없음" : String.join(" / ", signals));
        try {
            // 모델 2개 × 재요청까지 합쳐 오래 걸릴 수 있어 전체 20초가 넘으면 규칙 문장으로 대신합니다
            LlmFallbackCaller.Result<List<String>> r = CompletableFuture
                    .supplyAsync(() -> llm.call(SYSTEM, facts, ReportService::parseSummary))
                    .get(SUMMARY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return new Summary(r.value(), r.model());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ruleSummary(counts, signals, weights, walk);
        } catch (ExecutionException | TimeoutException e) {
            log.info("리포트 요약 LLM 실패·시간 초과, 규칙 문장 사용");
            return ruleSummary(counts, signals, weights, walk);
        }
    }

    private static Summary ruleSummary(Map<RecordCategory, Long> counts, List<String> signals,
                                       List<RecordResponses.WeightPoint> weights, int walk) {
        return new Summary(List.of(
                "기간 동안 기록 " + counts.values().stream().mapToLong(Long::longValue).sum() + "건 ("
                        + labelCounts(counts) + ")",
                signals.isEmpty() ? "눈에 띄는 이상 신호는 없었어요" : signals.get(0),
                "산책 총 " + walk + "분" + (weights.isEmpty() ? "" : ", 최근 체중 "
                        + weights.get(weights.size() - 1).weightKg().stripTrailingZeros().toPlainString() + "kg")),
                null);
    }

    static List<String> parseSummary(String raw) {
        JsonNode arr = LlmJson.read(raw).get("summary");
        if (arr == null || !arr.isArray() || arr.size() < 3) {
            throw new LlmFormatException("summary 3줄이 아님");
        }
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String s = arr.get(i).asText("").strip();
            if (s.isEmpty()) {
                throw new LlmFormatException("빈 줄");
            }
            lines.add(s.length() > 80 ? s.substring(0, 80) : s);
        }
        return lines;
    }

    private static String labelCounts(Map<RecordCategory, Long> counts) {
        if (counts.isEmpty()) {
            return "없음";
        }
        List<String> parts = new ArrayList<>();
        counts.forEach((k, v) -> parts.add(k.label() + " " + v));
        return String.join(", ", parts);
    }

    private static Map<String, Long> stoolCounts(List<RecordResponses.Record> stools) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (StoolStatus s : StoolStatus.values()) {
            long n = stools.stream().filter(r -> r.stoolStatus() == s).count();
            if (n > 0) {
                m.put(stoolLabel(s), n);
            }
        }
        return m;
    }

    private static String stoolLabel(StoolStatus s) {
        return switch (s) {
            case NORMAL -> "보통";
            case SOFT -> "무른 변";
            case DIARRHEA -> "설사";
            case HARD -> "딱딱함";
            case BLOODY -> "혈변";
        };
    }

    private static String sizeLabel(Dog dog) {
        return switch (dog.size()) {
            case SMALL -> "소형";
            case MEDIUM -> "중형";
            case LARGE -> "대형";
        };
    }

    private static List<RecordResponses.Record> filter(List<RecordResponses.Record> records, Set<RecordCategory> cats) {
        Predicate<RecordResponses.Record> p = r -> cats.contains(r.category());
        return records.stream().filter(p).toList();
    }

    private static List<ReportData.Row> rows(List<RecordResponses.Record> records) {
        return records.stream().map(r -> new ReportData.Row(r.recordDate(), r.categoryLabel(),
                r.memo() == null ? "" : r.memo(), r.createdByNickname())).toList();
    }
}
