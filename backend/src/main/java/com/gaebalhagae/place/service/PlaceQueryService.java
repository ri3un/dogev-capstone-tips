package com.gaebalhagae.place.service;

import com.gaebalhagae.dog.domain.Dog;
import com.gaebalhagae.family.service.FamilyAccessService;
import com.gaebalhagae.global.response.BusinessException;
import com.gaebalhagae.global.response.ErrorCode;
import com.gaebalhagae.global.util.Geo;
import com.gaebalhagae.place.domain.Place;
import com.gaebalhagae.place.domain.PlaceCategory;
import com.gaebalhagae.place.domain.PlaceCondition;
import com.gaebalhagae.place.dto.PlaceResponses;
import com.gaebalhagae.place.repository.PlaceConditionRepository;
import com.gaebalhagae.place.repository.PlaceRepository;
import com.gaebalhagae.place.repository.PlaceSourceRepository;
import com.gaebalhagae.place.review.ConditionKey;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 장소 목록·상세. 시연 지역 장소가 수십 곳이라 전부 읽어 메모리에서 거르고 정렬합니다.
 * shortcut: 장소가 수천 곳이 되면 (lat, lng) 범위 쿼리로 바꿀 것
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceQueryService {

    private final PlaceRepository placeRepository;
    private final PlaceConditionRepository conditionRepository;
    private final PlaceSourceRepository sourceRepository;
    private final FamilyAccessService access;

    public record Filter(PlaceCategory category, String q, Double lat, Double lng, Double radiusKm, Long dogId,
                         boolean fitOnly, List<String> conditions) {
    }

    /** 코스 추천이 쓰는 후보: 장소 + 조건 값 */
    public record Candidate(Place place, Map<String, PlaceVerdict.Cond> conditions) {
    }

    public List<PlaceResponses.Item> list(Long memberId, Filter f) {
        Dog dog = f.dogId() == null ? null : access.checkDog(memberId, f.dogId());
        Map<String, String> required = parseConditions(f.conditions());
        boolean near = f.lat() != null && f.lng() != null;
        return candidates().stream()
                .filter(c -> f.category() == null || c.place().getCategory() == f.category())
                .filter(c -> f.q() == null || f.q().isBlank() || c.place().getName().contains(f.q().strip()))
                .filter(c -> required.entrySet().stream().allMatch(e -> c.conditions().containsKey(e.getKey())
                        && e.getValue().equals(c.conditions().get(e.getKey()).value())))
                .map(c -> toItem(c, dog, near ? Geo.distanceM(f.lat(), f.lng(), c.place().getLat(),
                        c.place().getLng()) : null))
                .filter(i -> !near || f.radiusKm() == null || i.distanceM() <= f.radiusKm() * 1000)
                .filter(i -> !f.fitOnly() || i.verdict() == null || i.verdict().level() != PlaceVerdict.Level.HARD)
                .sorted(near ? Comparator.comparing(PlaceResponses.Item::distanceM)
                        : Comparator.comparing(PlaceResponses.Item::name))
                .toList();
    }

    public PlaceResponses.Detail detail(Long memberId, Long placeId, Long dogId) {
        Place p = placeRepository.findById(placeId).orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
        List<PlaceCondition> conds = conditionRepository.findByPlaceId(placeId);
        PlaceVerdict.Verdict verdict = dogId == null ? null
                : PlaceVerdict.judge(PlaceVerdict.DogInfo.of(access.checkDog(memberId, dogId)), toCondMap(conds));
        List<PlaceResponses.Condition> conditionItems = conds.stream()
                .sorted(Comparator.comparing(c -> ConditionKey.fromKey(c.getConditionKey())
                        .map(Enum::ordinal).orElse(99)))
                .map(PlaceQueryService::toCondition)
                .toList();
        List<PlaceResponses.Source> sources = sourceRepository.findByPlaceIdAndExcludedFalseOrderByIdAsc(placeId)
                .stream().map(s -> new PlaceResponses.Source(s.getUrl(), s.getTitle())).toList();
        return new PlaceResponses.Detail(p.getId(), p.getName(), p.getCategory(), p.getCategory().label(),
                p.getCategoryName(), p.getAddress(), p.getRoadAddress(), p.getLat(), p.getLng(), p.getPhone(),
                p.getPlaceUrl(), p.getImageUrl(), List.copyOf(p.getReviewSummary()), List.copyOf(p.getReviewKeywords()),
                p.getReviewSourceCount(), p.getReviewSummarizedAt(), conditionItems, sources, verdict);
    }

    public Place get(Long placeId) {
        return placeRepository.findById(placeId).orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
    }

    public List<Candidate> candidates() {
        Map<Long, List<PlaceCondition>> byPlace = conditionRepository.findAll().stream()
                .collect(Collectors.groupingBy(PlaceCondition::getPlaceId));
        return placeRepository.findAll().stream()
                .map(p -> new Candidate(p, toCondMap(byPlace.getOrDefault(p.getId(), List.of()))))
                .toList();
    }

    private static PlaceResponses.Item toItem(Candidate c, Dog dog, Integer distance) {
        Place p = c.place();
        PlaceVerdict.Verdict verdict = dog == null ? null
                : PlaceVerdict.judge(PlaceVerdict.DogInfo.of(dog), c.conditions());
        String first = p.getReviewSummary().isEmpty() ? null : p.getReviewSummary().get(0);
        return new PlaceResponses.Item(p.getId(), p.getName(), p.getCategory(), p.getCategory().label(),
                p.getAddress(), p.getLat(), p.getLng(), distance, List.copyOf(p.getReviewKeywords()), first, verdict);
    }

    private static PlaceResponses.Condition toCondition(PlaceCondition c) {
        ConditionKey key = ConditionKey.fromKey(c.getConditionKey()).orElse(null);
        String tag = key == null ? null : key.tagOf(c.getConditionValue()).orElse(null);
        return new PlaceResponses.Condition(c.getConditionKey(), key == null ? c.getConditionKey() : key.label(),
                c.getConditionValue(), tag, c.getWeightLimitKg(), c.getEvidenceCount(), c.getPositiveCount(),
                c.getSource());
    }

    static Map<String, PlaceVerdict.Cond> toCondMap(List<PlaceCondition> conds) {
        return conds.stream().filter(c -> c.getConditionValue() != null).collect(Collectors.toMap(
                PlaceCondition::getConditionKey, c -> new PlaceVerdict.Cond(c.getConditionValue(), c.getWeightLimitKg()),
                (a, b) -> a));
    }

    /** "indoor:indoor,parking:yes" → {indoor=indoor, parking=yes} */
    private static Map<String, String> parseConditions(List<String> raw) {
        if (raw == null) {
            return Map.of();
        }
        return raw.stream().filter(Objects::nonNull).map(String::strip).filter(s -> s.contains(":"))
                .collect(Collectors.toMap(s -> s.substring(0, s.indexOf(':')), s -> s.substring(s.indexOf(':') + 1),
                        (a, b) -> b));
    }
}
