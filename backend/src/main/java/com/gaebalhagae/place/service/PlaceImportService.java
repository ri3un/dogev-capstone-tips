package com.gaebalhagae.place.service;

import com.gaebalhagae.place.collect.CollectedPlace;
import com.gaebalhagae.place.domain.ConditionSource;
import com.gaebalhagae.place.domain.Place;
import com.gaebalhagae.place.domain.PlaceCategory;
import com.gaebalhagae.place.domain.PlaceCondition;
import com.gaebalhagae.place.domain.PlaceSource;
import com.gaebalhagae.place.dto.PlaceImportRequest;
import com.gaebalhagae.place.repository.PlaceConditionRepository;
import com.gaebalhagae.place.repository.PlaceRepository;
import com.gaebalhagae.place.repository.PlaceSourceRepository;
import com.gaebalhagae.place.review.ConditionKey;
import com.gaebalhagae.place.review.ConditionResult;
import com.gaebalhagae.place.review.ReviewSummary;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수집 JSON → DB 적재 (결정 4·30·32·33). kakao id 로 중복 없이 넣고, 요약은 장소 이름으로 붙입니다.
 * 관리자·수동 입력 조건은 블로그 수집이 덮어쓰지 않습니다 (ADMIN > MANUAL > TOUR > BLOG).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PlaceImportService {

    private final PlaceRepository placeRepository;
    private final PlaceConditionRepository conditionRepository;
    private final PlaceSourceRepository sourceRepository;
    private final Clock clock;

    public PlaceImportRequest.Result importData(PlaceImportRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        int created = 0;
        int updated = 0;
        Map<String, Place> byName = new HashMap<>();
        for (CollectedPlace cp : request.places()) {
            if (cp.kakaoId() == null || cp.name() == null) {
                continue;
            }
            Place place = placeRepository.findByKakaoPlaceId(cp.kakaoId()).orElse(null);
            if (place == null) {
                place = placeRepository.save(new Place(cp.kakaoId(), cp.name()));
                created++;
            } else {
                updated++;
            }
            place.updateBasic(cp.name(), PlaceCategory.of(cp.categoryGroupCode(), cp.name(), cp.category()),
                    cp.category(), cp.address(), cp.roadAddress(), cp.lat(), cp.lng(), cp.phone(), cp.placeUrl(),
                    now);
            byName.put(cp.name(), place);
        }

        int applied = 0;
        List<String> unmatched = new ArrayList<>();
        for (ReviewSummary s : request.summaries() == null ? List.<ReviewSummary>of() : request.summaries()) {
            Place place = byName.containsKey(s.placeName()) ? byName.get(s.placeName())
                    : placeRepository.findFirstByName(s.placeName()).orElse(null);
            if (place == null) {
                unmatched.add(s.placeName());
                continue;
            }
            applySummary(place, s);
            applied++;
        }
        log.info("장소 적재 created={} updated={} summaries={} unmatched={}", created, updated, applied, unmatched);
        return new PlaceImportRequest.Result(created, updated, applied, unmatched);
    }

    private void applySummary(Place place, ReviewSummary s) {
        place.updateReview(s.summary(), s.sourceCount(), s.summarizedAt());

        Map<String, PlaceCondition> existing = conditionRepository.findByPlaceId(place.getId()).stream()
                .collect(Collectors.toMap(PlaceCondition::getConditionKey, c -> c));
        if (s.conditions() != null) {
            s.conditions().forEach((key, r) -> upsertBlog(place.getId(), key, r, existing));
        }

        sourceRepository.deleteByPlaceIdAndSourceType(place.getId(), PlaceSource.BLOG);
        if (s.sourceUrls() != null) {
            s.sourceUrls().stream().distinct().limit(30)
                    .forEach(url -> sourceRepository.save(new PlaceSource(place.getId(), PlaceSource.BLOG, url)));
        }

        Map<String, String> values = existing.values().stream()
                .collect(Collectors.toMap(PlaceCondition::getConditionKey, PlaceCondition::getConditionValue));
        place.changeKeywords(PlaceKeywords.build(values, s.mood()));
    }

    private void upsertBlog(Long placeId, String key, ConditionResult r, Map<String, PlaceCondition> existing) {
        if (r == null || r.value() == null || ConditionKey.fromKey(key).isEmpty()) {
            return;
        }
        PlaceCondition c = existing.get(key);
        if (c != null && !ConditionSource.BLOG.canOverwrite(c.getSource())) {
            return;
        }
        boolean isNew = c == null;
        if (isNew) {
            c = new PlaceCondition(placeId, key);
        }
        c.update(r.value(), r.weightLimitKg(), r.evidenceCount(), r.positiveCount(), ConditionSource.BLOG);
        if (isNew) {
            existing.put(key, conditionRepository.save(c));
        }
    }
}
