package com.gaebalhagae.consult.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaebalhagae.consult.domain.Consultation;
import com.gaebalhagae.consult.domain.ConsultationAnswer;
import com.gaebalhagae.consult.domain.Urgency;
import com.gaebalhagae.consult.dto.ConsultResponses;
import com.gaebalhagae.consult.repository.ConsultationAnswerRepository;
import com.gaebalhagae.consult.repository.ConsultationRepository;
import com.gaebalhagae.dog.domain.Dog;
import com.gaebalhagae.dog.domain.DogDeletedEvent;
import com.gaebalhagae.family.service.FamilyAccessService;
import com.gaebalhagae.global.response.BusinessException;
import com.gaebalhagae.global.response.ErrorCode;
import com.gaebalhagae.llm.LlmFormatException;
import com.gaebalhagae.llm.LlmJson;
import com.gaebalhagae.llm.LlmParallelCaller;
import com.gaebalhagae.llm.LlmParallelCaller.Answer;
import com.gaebalhagae.llm.LlmParallelCaller.Status;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * AI 상담 (결정 2·14·15). LLM 3개 병렬, 모델별 15초. LLM 을 기다리는 동안 DB 트랜잭션을 잡지 않고
 * 답을 다 받은 뒤 짧은 트랜잭션으로 저장합니다. 프롬프트에는 반려견 이름·보호자 정보를 넣지 않습니다.
 */
@Service
@RequiredArgsConstructor
public class ConsultService {

    private static final String SYSTEM = """
            너는 반려견 보호자의 질문에 답하는 건강 상담 도우미야. 진단하지 말고, 보호자가 다음에 할 일을 알려 줘.
            JSON 객체 하나만 답해:
            {"urgency":"LOW|MEDIUM|HIGH|EMERGENCY","visitVet":true|false,
             "answer":"3~5문장, 쉬운 한국어","reasons":["판단 근거 1~3개"],"homeCare":["집에서 할 수 있는 일 0~3개"]}
            - LOW: 지켜봐도 됨, MEDIUM: 며칠 안에 진료 고려, HIGH: 오늘 진료 권장, EMERGENCY: 지금 바로 병원
            - 약 이름·용량은 말하지 마. 모르면 모른다고 하고 진료를 권해.
            """;

    private final FamilyAccessService access;
    private final LlmParallelCaller llm;
    private final ConsultationRepository consultationRepository;
    private final ConsultationAnswerRepository answerRepository;
    private final RecentContextBuilder recentContext;
    private final TransactionTemplate tx;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ConsultResponses.Detail ask(Long memberId, Long dogId, String question) {
        Dog dog = access.checkDog(memberId, dogId);
        String q = question.strip();
        List<String> context = recentContext.build(dogId, LocalDate.now(clock));
        List<Answer<JsonNode>> answers = llm.call(SYSTEM, prompt(dog, q, context), ConsultService::validate);

        List<ConsultAggregator.ModelAnswer> ok = answers.stream().filter(a -> a.status() == Status.OK)
                .map(a -> toModelAnswer(a.value())).toList();
        if (ok.isEmpty()) {
            throw new BusinessException(ErrorCode.LLM_UNAVAILABLE);
        }
        boolean emergency = EmergencyKeywords.matches(q);
        ConsultAggregator.Result result = ConsultAggregator.aggregate(ok, emergency);

        Long id = tx.execute(status -> {
            Consultation c = consultationRepository.save(new Consultation(dogId, memberId, q, emergency, ok.size(),
                    result.urgency(), result.visitVet(), result.crossChecked(), result.summary(), result.notices(),
                    context));
            for (Answer<JsonNode> a : answers) {
                answerRepository.save(new ConsultationAnswer(c.getId(), a.model(), a.status(),
                        a.value() == null ? null : a.value().toString(), a.latencyMs()));
            }
            return c.getId();
        });
        return detail(memberId, id);
    }

    /** 상담 기록 삭제: 그 반려견을 볼 수 있는 가족만, 아니면 있는지도 알려 주지 않고 404 */
    @Transactional
    public void delete(Long memberId, Long consultationId) {
        Consultation c = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONSULTATION_NOT_FOUND));
        try {
            access.checkDog(memberId, c.getDogId());
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.CONSULTATION_NOT_FOUND);
        }
        answerRepository.deleteByConsultationId(c.getId());
        consultationRepository.delete(c);
    }

    @Transactional(readOnly = true)
    public ConsultResponses.Detail detail(Long memberId, Long consultationId) {
        Consultation c = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONSULTATION_NOT_FOUND));
        try {
            access.checkDog(memberId, c.getDogId());
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.CONSULTATION_NOT_FOUND);
        }
        List<ConsultResponses.ModelItem> items = answerRepository.findByConsultationIdOrderByIdAsc(c.getId()).stream()
                .map(this::toItem).toList();
        return new ConsultResponses.Detail(c.getId(), c.getDogId(), c.getQuestion(), c.isEmergencyFlag(),
                c.getUrgency(), c.getUrgency().label(), c.isVisitVet(), c.isCrossChecked(), c.getAnsweredCount(),
                items.size(), List.copyOf(c.getSummary()), List.copyOf(c.getNotices()), items, c.getCreatedBy(),
                c.getCreatedAt(), c.getContextLines() == null ? List.of() : List.copyOf(c.getContextLines()));
    }

    @Transactional(readOnly = true)
    public List<ConsultResponses.Item> list(Long memberId, Long dogId) {
        access.checkDog(memberId, dogId);
        return consultationRepository.findByDogIdOrderByIdDesc(dogId).stream()
                .map(c -> new ConsultResponses.Item(c.getId(), c.getQuestion(), c.getUrgency(), c.getUrgency().label(),
                        c.isEmergencyFlag(), c.isVisitVet(), c.getCreatedAt()))
                .toList();
    }

    @EventListener
    public void onDogDeleted(DogDeletedEvent event) {
        answerRepository.deleteByDogId(event.dogId());
        consultationRepository.deleteByDogId(event.dogId());
    }

    /** 개인 식별 정보 없이 상담에 필요한 정보만. 최근 기록은 숫자 집계만(원문 없음) */
    private String prompt(Dog dog, String question, List<String> context) {
        int months = dog.ageMonths(LocalDate.now(clock));
        String info = "반려견 정보: 견종 %s, 나이 %d살 %d개월, 몸무게 %skg(%s), %s, 중성화 %s, 지병 %s, 알레르기 %s".formatted(
                dog.getBreed(), months / 12, months % 12, dog.getWeightKg().toPlainString(),
                dog.size(), dog.getSex(), dog.isNeutered() ? "함" : "안 함",
                dog.getDiseases().isEmpty() ? "없음" : String.join(", ", dog.getDiseases()),
                dog.getAllergies().isEmpty() ? "없음" : String.join(", ", dog.getAllergies()));
        String recent = context.isEmpty() ? ""
                : "\n최근 14일 기록:\n- " + String.join("\n- ", context)
                + "\n기록은 참고용이고 판단은 질문 기준으로 하세요";
        return info + recent + "\n보호자 질문: " + question;
    }

    static JsonNode validate(String raw) {
        JsonNode n = LlmJson.read(raw);
        try {
            Urgency.valueOf(LlmJson.text(n, "urgency"));
        } catch (RuntimeException e) {
            throw new LlmFormatException("urgency 값이 없음");
        }
        if (LlmJson.text(n, "answer") == null || !n.path("visitVet").isBoolean()) {
            throw new LlmFormatException("answer·visitVet 없음");
        }
        return n;
    }

    private static ConsultAggregator.ModelAnswer toModelAnswer(JsonNode n) {
        return new ConsultAggregator.ModelAnswer(Urgency.valueOf(LlmJson.text(n, "urgency")),
                n.path("visitVet").asBoolean(), LlmJson.text(n, "answer"), texts(n.get("reasons")),
                texts(n.get("homeCare")));
    }

    private ConsultResponses.ModelItem toItem(ConsultationAnswer a) {
        if (a.getStatus() != Status.OK || a.getAnswerJson() == null) {
            return new ConsultResponses.ModelItem(a.getModelName(), a.getStatus(), null, null, null, List.of(),
                    List.of(), a.getLatencyMs());
        }
        try {
            ConsultAggregator.ModelAnswer m = toModelAnswer(objectMapper.readTree(a.getAnswerJson()));
            return new ConsultResponses.ModelItem(a.getModelName(), a.getStatus(), m.urgency(), m.visitVet(),
                    m.answer(), m.reasons(), m.homeCare(), a.getLatencyMs());
        } catch (JsonProcessingException e) {
            return new ConsultResponses.ModelItem(a.getModelName(), Status.INVALID, null, null, null, List.of(),
                    List.of(), a.getLatencyMs());
        }
    }

    private static List<String> texts(JsonNode arr) {
        List<String> out = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            arr.forEach(x -> {
                if (x.isTextual() && !x.asText().isBlank() && out.size() < 5) {
                    out.add(x.asText().strip());
                }
            });
        }
        return out;
    }
}
