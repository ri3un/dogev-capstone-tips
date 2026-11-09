package com.gaebalhagae.record.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaebalhagae.family.service.FamilyAccessService;
import com.gaebalhagae.llm.LlmFallbackCaller;
import com.gaebalhagae.llm.LlmFormatException;
import com.gaebalhagae.llm.LlmJson;
import com.gaebalhagae.record.domain.RecordCategory;
import com.gaebalhagae.record.domain.StoolStatus;
import com.gaebalhagae.record.dto.RecordInput;
import com.gaebalhagae.record.dto.RecordResponses;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 글 → 기록 후보 (결정 5·9). LLM 은 카테고리별 JSON 만 만들고, 저장은 사용자가 확인한 뒤에 합니다.
 * LLM 을 기다리는 동안 DB 트랜잭션을 잡지 않도록 이 클래스에는 @Transactional 을 두지 않습니다.
 * 반려견 이름 등 개인 식별 정보는 보내지 않습니다.
 */
@Service
@RequiredArgsConstructor
public class RecordParseService {

    static final int MAX_RECORDS = 10;

    private static final String SYSTEM = """
            너는 반려견 보호자가 쓴 글을 건강 기록으로 나누는 도우미야. JSON 객체 하나만 답해.
            형식: {"records":[{"category":"...","date":"YYYY-MM-DD","memo":"한 줄 요약(40자 이내)",
              "weightKg":숫자|null,"stoolStatus":"NORMAL|SOFT|DIARRHEA|HARD|BLOODY"|null,
              "walkMinutes":정수|null,"waterMl":정수|null,"details":{"키":"값"}}]}
            category 는 다음 중 하나: %s
            - MEAL 식사, SNACK 간식, STOOL 배변, ACTIVITY 산책·놀이, SYMPTOM 증상(구토·기침·절뚝임 등),
              WEIGHT 체중, MEDICATION 투약·접종, HOSPITAL 병원 방문, QUESTION 수의사에게 물어볼 질문
            - 날짜는 "기준 날짜"를 오늘로 보고 "어제", "그저께"를 계산해. 말이 없으면 기준 날짜.
            - 글에 없는 내용은 만들지 말고, 진단이나 조언은 쓰지 마.
            - 한 글에 여러 일이 있으면 여러 개로 나눠. 기록할 내용이 없으면 records 를 빈 배열로.
            """.formatted(Arrays.stream(RecordCategory.values()).map(Enum::name).collect(Collectors.joining(", ")));

    private final FamilyAccessService access;
    private final LlmFallbackCaller llm;
    private final DangerFoodRule dangerFoodRule;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public RecordResponses.Parsed parse(Long memberId, Long dogId, String text, LocalDate date) {
        access.checkDog(memberId, dogId);
        LocalDate today = LocalDate.now(clock);
        LocalDate base = date == null || date.isAfter(today) ? today : date;
        LlmFallbackCaller.Result<List<RecordInput>> result = llm.call(SYSTEM,
                "기준 날짜: " + base + "\n글:\n" + text.strip(), raw -> toCandidates(raw, base));
        return new RecordResponses.Parsed(result.value(), dangerFoodRule.check(text), result.model());
    }

    /** LLM JSON 을 검증해 후보로 바꿉니다. 형식이 아예 틀리면 LlmFormatException → 같은 모델에 1회 재요청 */
    List<RecordInput> toCandidates(String raw, LocalDate base) {
        JsonNode records = LlmJson.read(raw).get("records");
        if (records == null || !records.isArray()) {
            throw new LlmFormatException("records 배열이 없음");
        }
        List<RecordInput> out = new ArrayList<>();
        for (JsonNode n : records) {
            RecordCategory category = enumOf(RecordCategory.class, LlmJson.text(n, "category"));
            if (category == null || out.size() >= MAX_RECORDS) {
                continue;
            }
            out.add(new RecordInput(category, dateOf(LlmJson.text(n, "date"), base), cut(LlmJson.text(n, "memo")),
                    details(n.get("details")), decimal(n.get("weightKg")),
                    enumOf(StoolStatus.class, LlmJson.text(n, "stoolStatus")),
                    integer(n.get("walkMinutes"), 1440), integer(n.get("waterMl"), 10000)));
        }
        if (out.isEmpty() && !records.isEmpty()) {
            throw new LlmFormatException("알 수 있는 카테고리가 없음");
        }
        return out;
    }

    private static LocalDate dateOf(String s, LocalDate base) {
        try {
            LocalDate d = s == null ? base : LocalDate.parse(s);
            return d.isAfter(base) || d.isBefore(base.minusYears(1)) ? base : d;
        } catch (RuntimeException e) {
            return base;
        }
    }

    private static <E extends Enum<E>> E enumOf(Class<E> type, String s) {
        try {
            return s == null ? null : Enum.valueOf(type, s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String cut(String s) {
        return s == null ? null : s.length() > 200 ? s.substring(0, 200) : s;
    }

    private static BigDecimal decimal(JsonNode n) {
        if (n == null || !n.isNumber()) {
            return null;
        }
        BigDecimal v = n.decimalValue();
        return v.compareTo(new BigDecimal("0.1")) < 0 || v.compareTo(BigDecimal.valueOf(100)) > 0 ? null : v;
    }

    private static Integer integer(JsonNode n, int max) {
        return n == null || !n.isNumber() || n.asInt() < 0 || n.asInt() > max ? null : n.asInt();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> details(JsonNode n) {
        return n == null || !n.isObject() ? null : objectMapper.convertValue(n, Map.class);
    }
}
