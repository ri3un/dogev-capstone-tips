package com.gaebalhagae.record.service;

import com.gaebalhagae.dog.domain.Dog;
import com.gaebalhagae.dog.domain.DogDeletedEvent;
import com.gaebalhagae.family.service.FamilyAccessService;
import com.gaebalhagae.global.response.BusinessException;
import com.gaebalhagae.global.response.ErrorCode;
import com.gaebalhagae.member.domain.Member;
import com.gaebalhagae.member.repository.MemberRepository;
import com.gaebalhagae.question.domain.QuestionSource;
import com.gaebalhagae.question.service.VetQuestionService;
import com.gaebalhagae.record.domain.HealthRecord;
import com.gaebalhagae.record.domain.RecordCategory;
import com.gaebalhagae.record.domain.RecordSource;
import com.gaebalhagae.record.dto.RecordInput;
import com.gaebalhagae.record.dto.RecordResponses;
import com.gaebalhagae.record.repository.HealthRecordRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RecordService {

    static final int DEFAULT_DAYS = 30;
    static final int MAX_DAYS = 366;

    private final HealthRecordRepository repository;
    private final FamilyAccessService access;
    private final VetQuestionService vetQuestionService;
    private final MemberRepository memberRepository;
    private final Clock clock;

    /** 확인 카드에서 고른 후보를 한 번에 저장. QUESTION 은 질문 메모로 보냅니다 */
    public List<RecordResponses.Record> saveBatch(Long memberId, Long dogId, List<RecordInput> inputs) {
        Dog dog = access.checkDog(memberId, dogId);
        List<HealthRecord> saved = new ArrayList<>();
        for (RecordInput in : inputs) {
            if (in.category() == RecordCategory.QUESTION) {
                String content = in.memo() == null || in.memo().isBlank() ? "질문 메모" : in.memo();
                vetQuestionService.create(memberId, dogId, content, QuestionSource.MEMO);
            } else {
                saved.add(repository.save(new HealthRecord(dogId, memberId, RecordSource.TEXT, in.toValues())));
            }
        }
        syncWeight(dog);
        return toResponses(saved);
    }

    public RecordResponses.Record create(Long memberId, Long dogId, RecordInput in) {
        Dog dog = access.checkDog(memberId, dogId);
        if (in.category() == RecordCategory.QUESTION) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "질문은 질문 메모로 저장해 주세요");
        }
        HealthRecord r = repository.save(new HealthRecord(dogId, memberId, RecordSource.MANUAL, in.toValues()));
        syncWeight(dog);
        return toResponses(List.of(r)).get(0);
    }

    public RecordResponses.Record update(Long memberId, Long recordId, RecordInput in) {
        HealthRecord r = find(memberId, recordId);
        if (in.category() == RecordCategory.QUESTION) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "질문은 질문 메모로 저장해 주세요");
        }
        r.apply(memberId, in.toValues());
        syncWeight(access.checkDog(memberId, r.getDogId()));
        return toResponses(List.of(r)).get(0);
    }

    public void delete(Long memberId, Long recordId) {
        HealthRecord r = find(memberId, recordId);
        repository.delete(r);
        repository.flush();
        syncWeight(access.checkDog(memberId, r.getDogId()));
    }

    @Transactional(readOnly = true)
    public List<RecordResponses.Record> list(Long memberId, Long dogId, LocalDate from, LocalDate to,
                                             RecordCategory category) {
        access.checkDog(memberId, dogId);
        LocalDate[] range = range(from, to);
        List<HealthRecord> records = category == null
                ? repository.findByDogIdAndRecordDateBetweenOrderByRecordDateAscIdAsc(dogId, range[0], range[1])
                : repository.findByDogIdAndCategoryAndRecordDateBetweenOrderByRecordDateAscIdAsc(
                        dogId, category, range[0], range[1]);
        return toResponses(records);
    }

    @Transactional(readOnly = true)
    public RecordResponses.Summary summary(Long memberId, Long dogId, LocalDate from, LocalDate to) {
        access.checkDog(memberId, dogId);
        LocalDate[] range = range(from, to);
        List<HealthRecord> records =
                repository.findByDogIdAndRecordDateBetweenOrderByRecordDateAscIdAsc(dogId, range[0], range[1]);
        Map<RecordCategory, Long> byCategory = new EnumMap<>(RecordCategory.class);
        Map<LocalDate, List<HealthRecord>> byDate = new TreeMap<>();
        for (HealthRecord r : records) {
            byCategory.merge(r.getCategory(), 1L, Long::sum);
            byDate.computeIfAbsent(r.getRecordDate(), d -> new ArrayList<>()).add(r);
        }
        List<RecordResponses.Day> days = byDate.entrySet().stream()
                .map(e -> new RecordResponses.Day(e.getKey(), e.getValue().size(),
                        e.getValue().stream().map(HealthRecord::getCategory).distinct().toList()))
                .toList();
        int walk = records.stream().map(HealthRecord::getWalkMinutes).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).sum();
        List<Integer> water = records.stream().map(HealthRecord::getWaterMl).filter(Objects::nonNull).toList();
        Integer waterAvg = water.isEmpty() ? null
                : (int) Math.round(water.stream().mapToInt(Integer::intValue).average().orElse(0));
        long loose = records.stream().filter(r -> r.getStoolStatus() != null && r.getStoolStatus().isLoose()).count();
        return new RecordResponses.Summary(range[0], range[1], byCategory, days, walk, waterAvg, loose);
    }

    @Transactional(readOnly = true)
    public List<RecordResponses.WeightPoint> weights(Long memberId, Long dogId, LocalDate from, LocalDate to) {
        access.checkDog(memberId, dogId);
        LocalDate[] range = range(from, to);
        return repository.findByDogIdAndCategoryAndRecordDateBetweenOrderByRecordDateAscIdAsc(
                        dogId, RecordCategory.WEIGHT, range[0], range[1]).stream()
                .filter(r -> r.getWeightKg() != null)
                .map(r -> new RecordResponses.WeightPoint(r.getRecordDate(), r.getWeightKg()))
                .toList();
    }

    /** 코스·장소 "다녀왔어요"가 쓰는 활동 기록. 호출하는 쪽에서 권한을 이미 확인했습니다 */
    public HealthRecord createActivity(Long dogId, Long memberId, LocalDate date, RecordSource source,
                                       Integer walkMinutes, String memo, Map<String, Object> data) {
        return repository.save(new HealthRecord(dogId, memberId, source, new HealthRecord.Values(
                date, RecordCategory.ACTIVITY, memo, data, null, null, walkMinutes, null)));
    }

    @Transactional(readOnly = true)
    public RecordResponses.Record toResponse(HealthRecord record) {
        return toResponses(List.of(record)).get(0);
    }

    @EventListener
    public void onDogDeleted(DogDeletedEvent event) {
        repository.deleteByDogId(event.dogId());
    }

    /** 반려견 몸무게는 가장 최근 날짜의 체중 기록을 따릅니다 */
    private void syncWeight(Dog dog) {
        repository.findTopByDogIdAndCategoryAndWeightKgNotNullOrderByRecordDateDescIdDesc(dog.getId(),
                RecordCategory.WEIGHT).ifPresent(r -> dog.changeWeight(r.getWeightKg()));
    }

    /** 남의 반려견 기록은 존재 여부를 드러내지 않도록 404 */
    private HealthRecord find(Long memberId, Long recordId) {
        HealthRecord r = repository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECORD_NOT_FOUND));
        try {
            access.checkDog(memberId, r.getDogId());
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.RECORD_NOT_FOUND);
        }
        return r;
    }

    private LocalDate[] range(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now(clock) : to;
        LocalDate start = from == null ? end.minusDays(DEFAULT_DAYS - 1) : from;
        if (start.isAfter(end) || ChronoUnit.DAYS.between(start, end) >= MAX_DAYS) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "기간은 1일~366일로 지정해 주세요");
        }
        return new LocalDate[]{start, end};
    }

    private List<RecordResponses.Record> toResponses(Collection<HealthRecord> records) {
        Map<Long, String> names = memberRepository.findAllById(
                        records.stream().map(HealthRecord::getCreatedBy).distinct().toList()).stream()
                .collect(Collectors.toMap(Member::getId, Member::getNickname));
        return records.stream().map(r -> RecordResponses.Record.of(r, names.get(r.getCreatedBy()))).toList();
    }
}
