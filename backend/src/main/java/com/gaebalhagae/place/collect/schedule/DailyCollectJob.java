package com.gaebalhagae.place.collect.schedule;

import com.gaebalhagae.place.collect.BlogCollectService;
import com.gaebalhagae.place.collect.CollectedPlace;
import com.gaebalhagae.place.collect.CollectedPlaceStore;
import com.gaebalhagae.place.collect.PlaceCollectService;
import com.gaebalhagae.place.domain.PlaceCategory;
import com.gaebalhagae.place.dto.PlaceImportRequest;
import com.gaebalhagae.place.review.ConditionKey;
import com.gaebalhagae.place.review.ConditionResult;
import com.gaebalhagae.place.review.ReviewSummary;
import com.gaebalhagae.place.review.ReviewSummaryService;
import com.gaebalhagae.place.service.PlaceImportService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 매일 자동 수집: 카카오 검색어 몇 개 → 장소 목록 → 30곳 블로그 수집·LLM 요약 → DB import → 실행 기록.
 * collect.schedule.enabled=true 일 때만 켜짐 (노트북은 local 프로필이라 import 가 바로 MySQL 로 감).
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(CollectScheduleProperties.class)
@ConditionalOnProperty(name = "collect.schedule.enabled", havingValue = "true")
public class DailyCollectJob {

    private final CollectScheduleProperties props;
    private final PlaceCollectService placeCollect;
    private final CollectedPlaceStore placeStore;
    private final BlogCollectService blogCollect;
    private final ReviewSummaryService summaries;
    private final PlaceImportService importer;
    private final CollectJobStore jobStore;
    private final Clock clock;
    private final AtomicBoolean running = new AtomicBoolean(false);
    static final int RETRY_DAYS = 14;

    public DailyCollectJob(CollectScheduleProperties props, PlaceCollectService placeCollect,
                           CollectedPlaceStore placeStore, BlogCollectService blogCollect,
                           ReviewSummaryService summaries, PlaceImportService importer,
                           CollectJobStore jobStore, Clock clock) {
        this.props = props;
        this.placeCollect = placeCollect;
        this.placeStore = placeStore;
        this.blogCollect = blogCollect;
        this.summaries = summaries;
        this.importer = importer;
        this.jobStore = jobStore;
        this.clock = clock;
    }

    @Scheduled(cron = "${collect.schedule.cron}", zone = "Asia/Seoul")
    public void scheduled() {
        try {
            runOnce();
        } catch (RuntimeException e) {
            log.warn("매일 수집 실패: {}", e.getMessage(), e);
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public CollectJobStore.RunLog runOnce() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("이미 수집 중");
        }
        LocalDateTime startedAt = LocalDateTime.now(clock);
        try {
            // ① 오늘의 검색어로 장소 넓히기
            CollectJobStore.State state = jobStore.loadState();
            CollectPlanner.Queries q = CollectPlanner.nextQueries(props.areas(), props.keywords(),
                    state.queryCursor(), props.queriesPerDay());
            int newPlaces = 0;
            int totalPlaces = 0;
            if (!q.queries().isEmpty()) {
                PlaceCollectService.CollectResult found = placeCollect.collect("", props.addressPrefix(), q.queries());
                newPlaces = found.keywords().stream().mapToInt(PlaceCollectService.KeywordResult::added).sum();
                totalPlaces = found.total();
            }
            int rounds = state.rounds() + (q.wrapped() ? 1 : 0);
            LocalDate today = LocalDate.now(clock);
            Map<String, LocalDate> retryAfter = new HashMap<>(state.retryAfter());
            retryAfter.values().removeIf(d -> !d.isAfter(today));   // 쉬는 기간이 끝난 곳은 다시 후보
            jobStore.saveState(new CollectJobStore.State(q.nextCursor(), rounds, retryAfter));

            // ② 30곳 블로그 수집 → 요약 (한 곳 실패가 나머지를 멈추지 않게)
            List<CollectedPlace> places = placeStore.findAll();
            // 장소 이름 → 분류 (공원이면 카페 조건을 빼고 확인·반영)
            Map<String, PlaceCategory> categoryOf = new HashMap<>();
            places.forEach(p -> categoryOf.putIfAbsent(p.name(),
                    PlaceCategory.of(p.categoryGroupCode(), p.name(), p.category())));
            Function<String, ReviewSummary> summaryFor = name -> ReviewSummaryService.forCategory(
                    summaries.findByPlace(name).orElse(null), categoryOf.get(name));
            List<CollectedPlace> candidates = props.onlyCategory() == null || props.onlyCategory().isBlank() ? places
                    : places.stream().filter(p -> categoryOf.get(p.name()).name()
                            .equals(props.onlyCategory().strip())).toList();
            List<String> picked = CollectPlanner.pickPlaces(candidates, summaries::findByPlace,
                    Set.copyOf(retryAfter.keySet()), props.dailyPlaces());
            int blogCollected = 0;
            int summarized = 0;
            int skipped = 0;
            List<String> failed = new ArrayList<>();
            for (String name : picked) {
                try {
                    blogCollect.collect(name);
                    blogCollected++;
                    boolean calledLlm = "SUMMARIZED".equals(summaries.summarize(name, false).status());
                    if (calledLlm) {
                        summarized++;
                    }
                    // 글 10건 + 조건 키워드가 없으면 애견동반 미확인 → 14일 쉬고 다시 (그사이 새 후기가 쌓임)
                    if (summaries.isConfirmed(summaryFor.apply(name))) {
                        retryAfter.remove(name);
                    } else {
                        skipped++;
                        retryAfter.put(name, today.plusDays(RETRY_DAYS));
                    }
                    if (calledLlm) {
                        pause(); // Groq 분당 토큰 한도 (결정 30)
                    }
                } catch (RuntimeException e) {
                    log.warn("매일 수집: {} 실패 - {}", name, e.getMessage());
                    failed.add(name);
                }
            }

            jobStore.saveState(new CollectJobStore.State(q.nextCursor(), rounds, retryAfter));

            // ③ 애견동반이 확인된 곳만 DB 에 반영 (카카오 결과엔 일반 카페도 섞임. import 는 kakaoId 기준 생성·갱신)
            // 공원·산책로는 목줄만 하면 출입 가능한 공공장소라 블로그 5건 기준 없이 넣음 (2026-11-26)
            List<ReviewSummary> confirmed = places.stream().map(CollectedPlace::name).distinct()
                    .map(name -> {
                        ReviewSummary s = summaryFor.apply(name);
                        if (s != null && summaries.isConfirmed(s)) {
                            return s;
                        }
                        // 블로그 후기가 한 건도 없는 공원은 넣지 않음 (2026-11-26)
                        return categoryOf.get(name) == PlaceCategory.PARK && s != null && s.sourceCount() > 0
                                ? parkDefault(name, s) : null;
                    })
                    .filter(Objects::nonNull).toList();
            Set<String> confirmedNames = confirmed.stream().map(ReviewSummary::placeName).collect(Collectors.toSet());
            List<CollectedPlace> confirmedPlaces = places.stream().filter(p -> confirmedNames.contains(p.name())).toList();
            PlaceImportRequest.Result imported = importer.importData(new PlaceImportRequest(confirmedPlaces, confirmed));

            CollectJobStore.RunLog entry = new CollectJobStore.RunLog(startedAt, LocalDateTime.now(clock),
                    q.queries(), newPlaces, totalPlaces, blogCollected, summarized, skipped, failed.size(),
                    List.copyOf(failed), imported.created() + imported.updated(), null);
            jobStore.appendLog(entry);
            log.info("매일 수집 끝: 새 장소 {}, 요약 {}, 실패 {}, DB 반영 {}", newPlaces, summarized, failed.size(),
                    entry.imported());
            return entry;
        } catch (RuntimeException e) {
            jobStore.appendLog(new CollectJobStore.RunLog(startedAt, LocalDateTime.now(clock), List.of(), 0, 0, 0,
                    0, 0, 0, List.of(), 0, e.getMessage()));
            throw e;
        } finally {
            running.set(false);
        }
    }

    /** 블로그로 확인 못 한 공원: 기존 요약은 살리고 목줄 조건이 비어 있으면 "목줄 필수"로 채움 */
    static ReviewSummary parkDefault(String name, ReviewSummary s) {
        Map<String, ConditionResult> conditions = new LinkedHashMap<>(
                s == null || s.conditions() == null ? Map.of() : s.conditions());
        String leash = ConditionKey.LEASH_REQUIRED.key();
        ConditionResult current = conditions.get(leash);
        if (current == null || !ConditionKey.LEASH_REQUIRED.isKnownValue(current.value())) {
            conditions.put(leash, new ConditionResult("yes", null, 0, 0, List.of()));
        }
        if (s == null) {
            return new ReviewSummary(name, List.of(), List.of(), conditions, List.of(), 0, List.of(), "park-default",
                    null);
        }
        return new ReviewSummary(name, s.summary(), s.keywords(), conditions, s.mood(), s.sourceCount(),
                s.sourceUrls(), s.model(), s.summarizedAt());
    }

    private void pause() {
        if (props.summaryDelayMs() <= 0) {
            return;
        }
        try {
            Thread.sleep(props.summaryDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("수집 중단됨", e);
        }
    }
}
