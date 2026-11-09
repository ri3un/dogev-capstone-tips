# 안양 장소 매일 자동 수집 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **끊기면 새 세션에 그대로 넣기:**
> `docs/superpowers/plans/2026-11-09-daily-place-collect.md 를 읽고 첫 번째 [ ] 부터 이어서 해 줘. 브랜치는 feature/daily-collect.`

**Goal:** 매일 정해진 시각에 카카오로 안양 장소를 넓혀 찾고, 그중 30곳의 네이버 블로그 후기를 모아 LLM으로 요약한 뒤, 실제 DB에 자동으로 import 해서 데이터를 쌓는다.

**Architecture:** 기존 수집 서비스(`PlaceCollectService`·`BlogCollectService`·`ReviewSummaryService`)와 `PlaceImportService` 를 그대로 이어 붙이는 `DailyCollectJob` 하나를 만든다. 순서 계산(검색어 순환·30곳 고르기)은 순수 함수 `CollectPlanner` 로 떼어 테스트하고, 검색어 진행 위치·실행 기록은 `CollectJobStore`(JSON 파일)에 둔다. `collect.schedule.enabled=true` 일 때만 켜지므로 노트북은 `local` 프로필(MySQL)로 돌려 import 가 바로 실제 DB에 들어가고, 나중에 배포 서버는 `prod` 에서 같은 값만 켜면 된다.

**Tech Stack:** Spring Boot 3.5 / Java 17, Spring `@Scheduled`, Jackson, JUnit 5 + Mockito (`./gradlew test`, H2)

**Spec:** 사용자 결정(2026-11-09 대화) — ① 먼저 노트북, 나중에 배포 서버로 이동 ② 수집이 끝나면 import 까지 자동 ③ 범위는 안양 안에서 넓히고, 안양 목록이 다 돌면 다른 시로 확장

## Global Constraints

- 하루 처리 장소 수: 30 (`collect.schedule.daily-places`)
- LLM 요약 사이 쉬는 시간: 기존 `collect.summary-delay-ms` (기본 40000ms, 결정 30)
- 주소 필터: `경기 안양시` 로 시작하는 장소만 (`collect.schedule.address-prefix`)
- 기본은 꺼짐: `COLLECT_SCHEDULE_ENABLED` 환경 변수(기본 false). 테스트·배포에서 실수로 돌지 않게
- 실행 시각: 매일 03:00 Asia/Seoul (`collect.schedule.cron`, 기본 `0 0 3 * * *`)
- 파일 위치: 기존 기본값(`data/place`, `data/blog`, `data/summary`)과 같은 `data/` 아래 `collect-state.json`, `collect-log.jsonl`
- Java 17 (가상 스레드 `Thread.ofVirtual` 쓰지 않음)
- 커밋 메시지: `feat:/fix:/docs:` + 한국어 요약 + `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`
- 브랜치: `feature/daily-collect` (`feature/connect-api` 에서 분기 — PR #10 이 develop 에 머지되기 전이라)

---

## File Structure

| 파일 | 하는 일 |
|---|---|
| Create `backend/src/main/java/com/gaebalhagae/place/collect/schedule/CollectPlanner.java` | 순수 함수: 오늘 돌릴 검색어, 오늘 처리할 30곳 |
| Create `backend/src/main/java/com/gaebalhagae/place/collect/schedule/CollectJobStore.java` | 검색어 진행 위치 읽기·쓰기, 실행 기록 추가·최근 기록 읽기 |
| Create `backend/src/main/java/com/gaebalhagae/place/collect/schedule/CollectScheduleProperties.java` | `collect.schedule.*` 설정 |
| Create `backend/src/main/java/com/gaebalhagae/place/collect/schedule/DailyCollectJob.java` | 하루 작업(검색어 → 장소 → 30곳 블로그·요약 → import → 기록), 예약 실행, 동시 실행 막기 |
| Create `backend/src/main/java/com/gaebalhagae/place/collect/schedule/CollectJobController.java` | 관리자용 지금 실행·최근 기록 (`/api/admin/collect/**`) |
| Modify `backend/src/main/resources/application.yml` | `collect.schedule` 기본값(안양 동 이름·키워드) |
| Modify `.env.example` | `COLLECT_SCHEDULE_ENABLED=` 칸 |
| Test `backend/src/test/java/com/gaebalhagae/place/collect/schedule/CollectPlannerTest.java` | 순환·고르기 규칙 |
| Test `backend/src/test/java/com/gaebalhagae/place/collect/schedule/CollectJobStoreTest.java` | 파일 읽기·쓰기 |
| Test `backend/src/test/java/com/gaebalhagae/place/collect/schedule/DailyCollectJobTest.java` | 단계 순서, 한 곳 실패가 전체를 멈추지 않음, import 호출 |

---

### Task 0: 브랜치

- [x] `git switch -c feature/daily-collect` (현재 `feature/connect-api` 에서) 후 이 계획 파일 커밋 (`docs: 매일 자동 수집 계획`)

### Task 1: CollectPlanner (검색어 순환·30곳 고르기)

**Files:**
- Create: `backend/src/main/java/com/gaebalhagae/place/collect/schedule/CollectPlanner.java`
- Test: `backend/src/test/java/com/gaebalhagae/place/collect/schedule/CollectPlannerTest.java`

**Interfaces:**
- Produces:
  - `record CollectPlanner.Queries(List<String> queries, int nextCursor, boolean wrapped)`
  - `static Queries CollectPlanner.nextQueries(List<String> areas, List<String> keywords, int cursor, int count)` — 검색어 = `area + " " + keyword`, 지역마다 모든 키워드를 돈 뒤 다음 지역. 끝을 넘으면 처음으로, `wrapped=true`
  - `static List<String> CollectPlanner.pickPlaces(List<CollectedPlace> places, Function<String, Optional<ReviewSummary>> summaryOf, int limit)` — 요약 없는 곳(목록 순서) 먼저, 그다음 요약이 오래된 순. 이름이 같으면 한 번만

- [x] **Step 1: 실패하는 테스트 작성**

```java
package com.gaebalhagae.place.collect.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import com.gaebalhagae.place.collect.CollectedPlace;
import com.gaebalhagae.place.review.ReviewSummary;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CollectPlannerTest {

    private static final List<String> AREAS = List.of("평촌", "범계");
    private static final List<String> KEYWORDS = List.of("애견동반 카페", "애견동반 식당", "강아지 공원");

    @Test
    void 지역마다_키워드를_차례로_돌고_다음_위치를_돌려준다() {
        CollectPlanner.Queries q = CollectPlanner.nextQueries(AREAS, KEYWORDS, 0, 4);
        assertThat(q.queries()).containsExactly("평촌 애견동반 카페", "평촌 애견동반 식당", "평촌 강아지 공원", "범계 애견동반 카페");
        assertThat(q.nextCursor()).isEqualTo(4);
        assertThat(q.wrapped()).isFalse();
    }

    @Test
    void 끝까지_가면_처음으로_돌아가고_한바퀴_돌았다고_알린다() {
        CollectPlanner.Queries q = CollectPlanner.nextQueries(AREAS, KEYWORDS, 5, 3);
        assertThat(q.queries()).containsExactly("범계 강아지 공원", "평촌 애견동반 카페", "평촌 애견동반 식당");
        assertThat(q.nextCursor()).isEqualTo(2);
        assertThat(q.wrapped()).isTrue();
    }

    @Test
    void 요약_없는_곳을_먼저_그다음_오래된_요약_순으로_고른다() {
        List<CollectedPlace> places = List.of(place("1", "새카페"), place("2", "옛식당"), place("3", "최근공원"), place("4", "또새곳"));
        Map<String, ReviewSummary> summaries = Map.of(
                "옛식당", summary("옛식당", LocalDateTime.of(2026, 10, 1, 3, 0)),
                "최근공원", summary("최근공원", LocalDateTime.of(2026, 11, 8, 3, 0)));

        List<String> picked = CollectPlanner.pickPlaces(places, n -> Optional.ofNullable(summaries.get(n)), 3);

        assertThat(picked).containsExactly("새카페", "또새곳", "옛식당");
    }

    @Test
    void 이름이_같은_장소는_한번만_고른다() {
        List<CollectedPlace> places = List.of(place("1", "멍카페"), place("2", "멍카페"), place("3", "냥식당"));
        assertThat(CollectPlanner.pickPlaces(places, n -> Optional.empty(), 30)).containsExactly("멍카페", "냥식당");
    }

    private static CollectedPlace place(String id, String name) {
        return new CollectedPlace(id, name, "음식점 > 카페", "CE7", "경기 안양시 동안구", "", 37.39, 126.95, "", "", List.of(), null);
    }

    private static ReviewSummary summary(String name, LocalDateTime at) {
        return new ReviewSummary(name, List.of(), List.of(), Map.of(), List.of(), 1, List.of(), "test", at);
    }
}
```

- [x] **Step 2: 실패 확인** — `cd backend && ./gradlew test --tests "*CollectPlannerTest"` → 컴파일 오류(`CollectPlanner` 없음)

- [x] **Step 3: 구현**

```java
package com.gaebalhagae.place.collect.schedule;

import com.gaebalhagae.place.collect.CollectedPlace;
import com.gaebalhagae.place.review.ReviewSummary;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/** 매일 수집에서 "오늘 무엇을 할지" 정하는 순수 함수 모음 (파일·네트워크 없음) */
public final class CollectPlanner {

    private CollectPlanner() {
    }

    /** wrapped = 이번에 목록 끝을 넘어 처음으로 돌아왔음 (= 검색어를 한 바퀴 다 돌았음) */
    public record Queries(List<String> queries, int nextCursor, boolean wrapped) {
    }

    /** 검색어 = "지역 키워드". 지역마다 모든 키워드를 돈 뒤 다음 지역 */
    public static Queries nextQueries(List<String> areas, List<String> keywords, int cursor, int count) {
        List<String> all = new ArrayList<>();
        for (String area : areas) {
            for (String keyword : keywords) {
                all.add(area + " " + keyword);
            }
        }
        if (all.isEmpty() || count <= 0) {
            return new Queries(List.of(), 0, false);
        }
        int start = Math.floorMod(cursor, all.size());
        List<String> picked = new ArrayList<>();
        for (int i = 0; i < Math.min(count, all.size()); i++) {
            picked.add(all.get((start + i) % all.size()));
        }
        int end = start + picked.size();
        return new Queries(picked, end % all.size(), end >= all.size());
    }

    /** 요약 없는 곳(찾은 순서) 먼저, 그다음 요약이 오래된 순. 블로그·요약은 이름으로 하므로 이름이 같으면 한 번만 */
    public static List<String> pickPlaces(List<CollectedPlace> places,
                                          Function<String, Optional<ReviewSummary>> summaryOf, int limit) {
        Set<String> names = new LinkedHashSet<>();
        places.forEach(p -> names.add(p.name()));

        List<String> fresh = new ArrayList<>();
        List<ReviewSummary> old = new ArrayList<>();
        for (String name : names) {
            Optional<ReviewSummary> s = summaryOf.apply(name);
            if (s.isEmpty()) {
                fresh.add(name);
            } else {
                old.add(s.get());
            }
        }
        old.sort(Comparator.comparing(ReviewSummary::summarizedAt,
                Comparator.nullsFirst(Comparator.<LocalDateTime>naturalOrder())));

        List<String> picked = new ArrayList<>(fresh);
        old.forEach(s -> picked.add(s.placeName()));
        return picked.stream().limit(Math.max(limit, 0)).toList();
    }
}
```

- [x] **Step 4: 통과 확인** — 같은 명령, 4 tests PASS
- [x] **Step 5: 커밋** — `feat: 매일 수집의 검색어 순환·30곳 고르기 규칙 추가`

---

### Task 2: CollectJobStore (진행 위치·실행 기록 파일)

**Files:**
- Create: `backend/src/main/java/com/gaebalhagae/place/collect/schedule/CollectJobStore.java`
- Test: `backend/src/test/java/com/gaebalhagae/place/collect/schedule/CollectJobStoreTest.java`

**Interfaces:**
- Produces:
  - `record CollectJobStore.State(int queryCursor, int rounds)` — rounds = 검색어를 한 바퀴 다 돈 횟수
  - `record CollectJobStore.RunLog(LocalDateTime startedAt, LocalDateTime finishedAt, List<String> queries, int newPlaces, int totalPlaces, int blogCollected, int summarized, int skipped, int failed, List<String> failedPlaces, int imported, String error)`
  - `CollectJobStore(ObjectMapper om, @Value("${collect.schedule.dir:data}") String dir)`
  - `State loadState()` (파일 없으면 `State(0, 0)`), `void saveState(State)`, `void appendLog(RunLog)`, `List<RunLog> recentLogs(int n)` (최신이 앞)

- [x] **Step 1: 실패하는 테스트 작성**

```java
package com.gaebalhagae.place.collect.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CollectJobStoreTest {

    @TempDir
    Path dir;

    private CollectJobStore store() {
        return new CollectJobStore(new ObjectMapper().registerModule(new JavaTimeModule()), dir.toString());
    }

    @Test
    void 처음에는_위치_0이고_저장한_위치를_다시_읽는다() {
        assertThat(store().loadState()).isEqualTo(new CollectJobStore.State(0, 0));
        store().saveState(new CollectJobStore.State(7, 1));
        assertThat(store().loadState()).isEqualTo(new CollectJobStore.State(7, 1));
    }

    @Test
    void 실행_기록을_쌓고_최신부터_돌려준다() {
        CollectJobStore s = store();
        s.appendLog(log(1));
        s.appendLog(log(2));
        s.appendLog(log(3));
        assertThat(s.recentLogs(2)).extracting(CollectJobStore.RunLog::newPlaces).containsExactly(3, 2);
    }

    private static CollectJobStore.RunLog log(int n) {
        LocalDateTime t = LocalDateTime.of(2026, 11, 9, 3, 0);
        return new CollectJobStore.RunLog(t, t, List.of("평촌 애견동반 카페"), n, 10, 1, 1, 0, 0, List.of(), 10, null);
    }
}
```

- [x] **Step 2: 실패 확인** — `cd backend && ./gradlew test --tests "*CollectJobStoreTest"` → 컴파일 오류

- [x] **Step 3: 구현**

```java
package com.gaebalhagae.place.collect.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 매일 수집의 검색어 진행 위치(data/collect-state.json)와 실행 기록(data/collect-log.jsonl, 한 번에 한 줄) */
@Component
public class CollectJobStore {

    public record State(int queryCursor, int rounds) {
    }

    public record RunLog(LocalDateTime startedAt, LocalDateTime finishedAt, List<String> queries, int newPlaces,
                         int totalPlaces, int blogCollected, int summarized, int skipped, int failed,
                         List<String> failedPlaces, int imported, String error) {
    }

    private final ObjectMapper om;
    private final Path statePath;
    private final Path logPath;

    public CollectJobStore(ObjectMapper om, @Value("${collect.schedule.dir:data}") String dir) {
        this.om = om;
        this.statePath = Path.of(dir, "collect-state.json");
        this.logPath = Path.of(dir, "collect-log.jsonl");
    }

    public synchronized State loadState() {
        try {
            return Files.exists(statePath) ? om.readValue(statePath.toFile(), State.class) : new State(0, 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public synchronized void saveState(State state) {
        try {
            Files.createDirectories(statePath.getParent());
            om.writeValue(statePath.toFile(), state);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public synchronized void appendLog(RunLog log) {
        try {
            Files.createDirectories(logPath.getParent());
            Files.writeString(logPath, om.writeValueAsString(log) + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** 최신 기록부터 n개 */
    public synchronized List<RunLog> recentLogs(int n) {
        try {
            if (!Files.exists(logPath)) {
                return List.of();
            }
            List<RunLog> logs = new ArrayList<>();
            for (String line : Files.readAllLines(logPath, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    logs.add(om.readValue(line, RunLog.class));
                }
            }
            Collections.reverse(logs);
            return logs.stream().limit(n).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
```

- [x] **Step 4: 통과 확인** — 2 tests PASS
- [x] **Step 5: 커밋** — `feat: 매일 수집의 진행 위치·실행 기록 파일 저장소 추가`

---

### Task 3: DailyCollectJob + 설정 (하루 작업, 예약 실행)

**Files:**
- Create: `backend/src/main/java/com/gaebalhagae/place/collect/schedule/CollectScheduleProperties.java`
- Create: `backend/src/main/java/com/gaebalhagae/place/collect/schedule/DailyCollectJob.java`
- Modify: `backend/src/main/resources/application.yml` (공통 구역)
- Modify: `.env.example`
- Test: `backend/src/test/java/com/gaebalhagae/place/collect/schedule/DailyCollectJobTest.java`

**Interfaces:**
- Consumes: Task 1·2, 기존 `PlaceCollectService.collect(String area, String addressPrefix, List<String> keywords)` → `CollectResult(List<KeywordResult> keywords, int total)` / `KeywordResult(String query, int fetched, int inArea, int added)` (검색어는 `(area + " " + keyword).trim()` 이라 area 에 `""` 를 주면 검색어 그대로), `CollectedPlaceStore.findAll()`, `BlogCollectService.collect(String)`, `ReviewSummaryService.summarize(String, boolean)` → `SummaryResult(placeName, status, sourceCount, summary)` (status `SUMMARIZED`·`SKIPPED_NO_NEW_POSTS`·`NO_SOURCE`), `ReviewSummaryService.findByPlace(String)`, `PlaceImportService.importData(PlaceImportRequest)` → `Result(created, updated, summariesApplied, unmatchedSummaries)`
- Produces:
  - `record CollectScheduleProperties(boolean enabled, String cron, int dailyPlaces, int queriesPerDay, String addressPrefix, List<String> areas, List<String> keywords, long summaryDelayMs)` — `@ConfigurationProperties("collect.schedule")`
  - `CollectJobStore.RunLog DailyCollectJob.runOnce()` — 동기 실행. 이미 도는 중이면 `IllegalStateException`
  - `boolean DailyCollectJob.isRunning()`, `void DailyCollectJob.scheduled()` (`@Scheduled(cron = "${collect.schedule.cron}", zone = "Asia/Seoul")`, 예외는 로그만)

- [x] **Step 1: 설정** — `application.yml` 공통 구역에:

```yaml
collect:
  schedule:
    # 매일 자동 수집을 켤 때만 true (.env 의 COLLECT_SCHEDULE_ENABLED)
    enabled: ${COLLECT_SCHEDULE_ENABLED:false}
    cron: "0 0 3 * * *"          # 매일 03:00 (Asia/Seoul)
    daily-places: 30             # 하루에 블로그 수집·요약할 장소 수
    queries-per-day: 6           # 하루에 새로 돌릴 카카오 검색어 수 (검색 1번에 최대 45곳)
    address-prefix: 경기 안양시
    summary-delay-ms: ${collect.summary-delay-ms:40000}
    # 안양 안에서 넓혀 가는 순서. 다 돌면 처음으로 돌아가고 로그의 rounds 가 1 늘어남 → 그때 다른 시 추가
    areas: [평촌, 범계, 인덕원, 안양역, 안양1번가, 명학역, 비산동, 관양동, 부흥동, 귀인동, 호계동, 평안동, 신촌동, 달안동, 부림동, 갈산동, 석수동, 박달동, 안양동, 만안구, 동안구]
    keywords: [애견동반 카페, 애견동반 식당, 반려견 동반, 애견카페, 강아지 공원, 반려견 놀이터, 펫프렌들리]
```

`.env.example` 의 `# 장소 데이터·지도` 구역 끝에:

```
# 매일 03:00 자동 수집(카카오 → 블로그 → LLM 요약 → DB import). 켤 때만 true
COLLECT_SCHEDULE_ENABLED=
```

- [x] **Step 2: 실패하는 테스트 작성**

```java
package com.gaebalhagae.place.collect.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gaebalhagae.place.collect.BlogCollectService;
import com.gaebalhagae.place.collect.CollectedPlace;
import com.gaebalhagae.place.collect.CollectedPlaceStore;
import com.gaebalhagae.place.collect.PlaceCollectService;
import com.gaebalhagae.place.dto.PlaceImportRequest;
import com.gaebalhagae.place.review.ReviewSummaryService;
import com.gaebalhagae.place.service.PlaceImportService;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;

class DailyCollectJobTest {

    @TempDir
    Path dir;

    private final PlaceCollectService placeCollect = mock(PlaceCollectService.class);
    private final CollectedPlaceStore placeStore = mock(CollectedPlaceStore.class);
    private final BlogCollectService blogCollect = mock(BlogCollectService.class);
    private final ReviewSummaryService summaries = mock(ReviewSummaryService.class);
    private final PlaceImportService importer = mock(PlaceImportService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-09T18:00:00Z"), ZoneId.of("Asia/Seoul"));
    private CollectJobStore jobStore;
    private DailyCollectJob job;

    @BeforeEach
    void setUp() {
        jobStore = new CollectJobStore(new ObjectMapper().registerModule(new JavaTimeModule()), dir.toString());
        CollectScheduleProperties props = new CollectScheduleProperties(true, "0 0 3 * * *", 2, 2, "경기 안양시",
                List.of("평촌", "범계"), List.of("애견동반 카페"), 0);
        job = new DailyCollectJob(props, placeCollect, placeStore, blogCollect, summaries, importer, jobStore, clock);

        when(placeCollect.collect(eq(""), eq("경기 안양시"), any())).thenReturn(new PlaceCollectService.CollectResult(
                List.of(new PlaceCollectService.KeywordResult("평촌 애견동반 카페", 10, 5, 2),
                        new PlaceCollectService.KeywordResult("범계 애견동반 카페", 10, 3, 1)), 3));
        when(placeStore.findAll()).thenReturn(List.of(place("1", "멍카페"), place("2", "냥식당"), place("3", "공원")));
        when(summaries.findByPlace(anyString())).thenReturn(Optional.empty());
        when(blogCollect.collect(anyString())).thenAnswer(a -> new BlogCollectService.CollectResult(a.getArgument(0), 5, 3, 3, 0, 3));
        when(summaries.summarize(anyString(), eq(false))).thenAnswer(
                a -> new ReviewSummaryService.SummaryResult(a.getArgument(0), "SUMMARIZED", 3, null));
        when(importer.importData(any())).thenReturn(new PlaceImportRequest.Result(3, 0, 0, List.of()));
    }

    @Test
    void 검색어를_돌리고_두곳을_요약한_뒤_import_하고_기록한다() {
        CollectJobStore.RunLog log = job.runOnce();

        InOrder order = inOrder(placeCollect, blogCollect, summaries, importer);
        order.verify(placeCollect).collect("", "경기 안양시", List.of("평촌 애견동반 카페", "범계 애견동반 카페"));
        order.verify(blogCollect).collect("멍카페");
        order.verify(summaries).summarize("멍카페", false);
        order.verify(blogCollect).collect("냥식당");
        order.verify(summaries).summarize("냥식당", false);
        order.verify(importer).importData(any(PlaceImportRequest.class));

        assertThat(log.newPlaces()).isEqualTo(3);
        assertThat(log.summarized()).isEqualTo(2);
        assertThat(log.imported()).isEqualTo(3);
        assertThat(jobStore.loadState()).isEqualTo(new CollectJobStore.State(0, 1)); // 검색어 2개를 다 돌아 한 바퀴
        assertThat(jobStore.recentLogs(1)).hasSize(1);
    }

    @Test
    void 한곳이_실패해도_나머지와_import_는_계속한다() {
        when(blogCollect.collect("멍카페")).thenThrow(new IllegalStateException("네이버 응답 없음"));

        CollectJobStore.RunLog log = job.runOnce();

        assertThat(log.failed()).isEqualTo(1);
        assertThat(log.failedPlaces()).containsExactly("멍카페");
        assertThat(log.summarized()).isEqualTo(1);
        assertThat(log.imported()).isEqualTo(3);
    }

    private static CollectedPlace place(String id, String name) {
        return new CollectedPlace(id, name, "음식점 > 카페", "CE7", "경기 안양시 동안구", "", 37.39, 126.95, "", "", List.of(), null);
    }
}
```

- [x] **Step 3: 실패 확인** — `cd backend && ./gradlew test --tests "*DailyCollectJobTest"` → 컴파일 오류

- [x] **Step 4: 구현**

`CollectScheduleProperties.java`:

```java
package com.gaebalhagae.place.collect.schedule;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml 의 collect.schedule.* */
@ConfigurationProperties("collect.schedule")
public record CollectScheduleProperties(boolean enabled, String cron, int dailyPlaces, int queriesPerDay,
                                        String addressPrefix, List<String> areas, List<String> keywords,
                                        long summaryDelayMs) {

    public CollectScheduleProperties {
        areas = areas == null ? List.of() : List.copyOf(areas);
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
    }
}
```

`DailyCollectJob.java`:

```java
package com.gaebalhagae.place.collect.schedule;

import com.gaebalhagae.place.collect.BlogCollectService;
import com.gaebalhagae.place.collect.CollectedPlace;
import com.gaebalhagae.place.collect.CollectedPlaceStore;
import com.gaebalhagae.place.collect.PlaceCollectService;
import com.gaebalhagae.place.dto.PlaceImportRequest;
import com.gaebalhagae.place.review.ReviewSummary;
import com.gaebalhagae.place.review.ReviewSummaryService;
import com.gaebalhagae.place.service.PlaceImportService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
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
            jobStore.saveState(new CollectJobStore.State(q.nextCursor(), state.rounds() + (q.wrapped() ? 1 : 0)));

            // ② 30곳 블로그 수집 → 요약 (한 곳 실패가 나머지를 멈추지 않게)
            List<CollectedPlace> places = placeStore.findAll();
            List<String> picked = CollectPlanner.pickPlaces(places, summaries::findByPlace, props.dailyPlaces());
            int blogCollected = 0;
            int summarized = 0;
            int skipped = 0;
            List<String> failed = new ArrayList<>();
            for (String name : picked) {
                try {
                    blogCollect.collect(name);
                    blogCollected++;
                    if ("SUMMARIZED".equals(summaries.summarize(name, false).status())) {
                        summarized++;
                        pause(); // Groq 분당 토큰 한도 (결정 30)
                    } else {
                        skipped++;
                    }
                } catch (RuntimeException e) {
                    log.warn("매일 수집: {} 실패 - {}", name, e.getMessage());
                    failed.add(name);
                }
            }

            // ③ 전체 장소 + 있는 요약을 DB 에 반영 (import 는 kakaoId 기준으로 생성·갱신)
            List<ReviewSummary> allSummaries = places.stream().map(CollectedPlace::name).distinct()
                    .map(summaries::findByPlace).flatMap(Optional::stream).toList();
            PlaceImportRequest.Result imported = importer.importData(new PlaceImportRequest(places, allSummaries));

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
```

- [x] **Step 5: 통과 확인** — 2 tests PASS
- [x] **Step 6: 전체 테스트** — `cd backend && ./gradlew test` → PASS (기존 111 + 새 8, 기본 enabled=false 라 다른 테스트 영향 없음)
- [x] **Step 7: 커밋** — `feat: 안양 장소 매일 자동 수집(검색어 순환→30곳 블로그·요약→DB import) 예약 작업 추가`

---

### Task 4: 관리자용 지금 실행·기록 보기

**Files:**
- Create: `backend/src/main/java/com/gaebalhagae/place/collect/schedule/CollectJobController.java`

**Interfaces:**
- Consumes: `DailyCollectJob.scheduled()`, `isRunning()` (Task 3), `CollectJobStore.recentLogs(int)` (Task 2)
- Produces: `POST /api/admin/collect/run` → `{"started": true|false}` (뒤에서 실행, 이미 돌면 false), `GET /api/admin/collect/logs?n=10` → `List<CollectJobStore.RunLog>`. `/api/admin/**` 는 기존 SecurityConfig 가 ADMIN 만 허용

- [x] **Step 1: 구현**

```java
package com.gaebalhagae.place.collect.schedule;

import com.gaebalhagae.global.response.ApiResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 매일 수집을 기다리지 않고 지금 한 번 돌리기 + 최근 실행 기록 (관리자만) */
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "collect.schedule.enabled", havingValue = "true")
public class CollectJobController {

    private final DailyCollectJob job;
    private final CollectJobStore jobStore;

    /** 30곳 요약에 20분쯤 걸려서 바로 돌려주고 뒤에서 돌림 */
    @PostMapping("/api/admin/collect/run")
    public ApiResponse<Map<String, Boolean>> run() {
        if (job.isRunning()) {
            return ApiResponse.ok(Map.of("started", false));
        }
        new Thread(job::scheduled, "collect-now").start();
        return ApiResponse.ok(Map.of("started", true));
    }

    @GetMapping("/api/admin/collect/logs")
    public ApiResponse<List<CollectJobStore.RunLog>> logs(@RequestParam(defaultValue = "10") int n) {
        return ApiResponse.ok(jobStore.recentLogs(Math.min(Math.max(n, 1), 100)));
    }
}
```

- [x] **Step 2: 전체 테스트** — `cd backend && ./gradlew test` → PASS
- [x] **Step 3: 커밋** — `feat: 관리자용 매일 수집 지금 실행·실행 기록 API 추가`

---

### Task 5: 노트북에서 실제로 한 번 돌려 확인

- [x] **Step 1:** 루트 `.env` 에 `COLLECT_SCHEDULE_ENABLED=true` 추가 (값 출력 금지)
- [x] **Step 2:** `cd backend && ./gradlew bootRun --args='--collect.schedule.daily-places=2 --collect.schedule.queries-per-day=1'` → `Started BackendApplication` (확인 동안만 2곳·검색어 1개)
- [x] **Step 3:** 개발용 로그인(관리자) 토큰으로 `POST /api/admin/collect/run` → `{"started":true}`, 몇 분 뒤 `GET /api/admin/collect/logs?n=1` → `newPlaces`·`summarized`·`imported` 확인, `error` 없음
- [x] **Step 4:** `GET /api/places` 에 새 장소가 늘었는지, 요약된 곳의 상세에 3줄 요약이 있는지 확인
- [x] **Step 5:** 백엔드 끄기, 이 계획 체크 갱신 후 커밋 (`docs: 매일 수집 노트북 확인`)

### Task 6: 노트북에서 매일 돌리기 안내 + 마무리

- [x] **Step 1:** 이 문서 끝 "운영 메모"에 적기: ① `.env` 에 `COLLECT_SCHEDULE_ENABLED=true` ② 03:00 에 노트북이 켜져 있고 `./gradlew bootRun` 이 떠 있어야 함 ③ 진행은 `backend/data/collect-log.jsonl` 또는 `GET /api/admin/collect/logs` ④ 로그·`collect-state.json` 의 `rounds` 가 1 이 되면 안양 검색어 한 바퀴 완료 → 다른 시 추가(`areas`, `address-prefix`) ⑤ 배포 서버로 옮길 때: 서버 `.env` 에 네이버 검색·카카오·LLM 키 + `COLLECT_SCHEDULE_ENABLED=true`, `data/` 를 Docker 볼륨으로
- [x] **Step 2:** `./gradlew test` 통과 확인 → 커밋 → 사용자에게 push·PR 여부 묻기

---

## Self-Review 메모

- 사용자 결정 ①(노트북 먼저·나중에 서버): Task 3 `enabled` + Task 6 ✔ ②(import 자동): Task 3 ③단계 ✔ ③(안양 안에서 넓히고 다 돌면 다른 시): Task 1 순환 + `rounds` + Task 6 ✔
- 하루 30곳 `daily-places: 30` ✔, 실행 기록 Task 2·3 ✔
- 타입 이름: `CollectPlanner.Queries`, `CollectJobStore.State/RunLog`, `CollectScheduleProperties`, `DailyCollectJob.runOnce(): CollectJobStore.RunLog` — Task 1~4 같은 이름 ✔

---

## 확인 결과 (2026-11-09, 노트북 local 프로필)

`--collect.schedule.daily-places=2 --collect.schedule.queries-per-day=1` 로 띄우고 `POST /api/admin/collect/run`:
검색어 `평촌 애견동반 카페` → 새 장소 38곳(모두 `경기 안양시`) → 블로그 2곳 수집 → 요약 1곳(킹박스커피), 후기 없음 1곳 → DB import 38곳(장소 2 → 40), 실패 0, 약 1분. `collect-state.json` = `{"queryCursor":1,"rounds":0}`

## 운영 메모: 노트북에서 매일 돌리기

1. 루트 `.env` 에 `COLLECT_SCHEDULE_ENABLED=true` (2026-11-09 넣어 둠). 끄려면 `false` 또는 줄 삭제
2. 수집 시각: 노트북(local)은 매일 **15:00**, 배포 서버(prod)는 매일 **03:00** (2026-11-09 사용자 결정). 그 시각에 노트북이 깨어 있고 `cd backend && ./gradlew bootRun` 이 떠 있어야 돔. 놓친 날은 다음 실행에서 이어서(검색어 위치는 파일에 남음). 시각은 `.env` 의 `COLLECT_CRON` 으로 바꿈 — 데이터가 충분히 쌓이면 한 달에 한 번 `0 0 3 1 * *`(매월 1일 03:00)
3. 기다리지 않고 지금 돌리기: 관리자 로그인 후 `POST /api/admin/collect/run` (30곳이면 요약 사이 40초씩 쉬어 약 20~25분)
4. 진행 보기: `GET /api/admin/collect/logs?n=7` 또는 `backend/data/collect-log.jsonl` (한 줄 = 하루). 서버 콘솔 로그는 Windows 인코딩이라 한글이 깨져 보일 수 있음 — 파일·API 는 정상
5. 안양 다 돌았는지: `backend/data/collect-state.json` 의 `rounds` 가 1 이 되면 검색어(21개 지역 × 7개 키워드 = 147개, 하루 6개면 약 25일) 한 바퀴 완료 → `application.yml` 의 `collect.schedule.areas` 에 다른 시 지역을 더하고 `address-prefix` 를 바꿈(여러 시를 함께 받으려면 접두어를 목록으로 바꾸는 작은 수정 필요)
6. 배포 서버로 옮길 때: 서버 `.env` 에 `NAVER_SEARCH_ID/SECRET`, `KAKAO_REST_KEY`, `GROQ_API_KEY`, `GEMINI_API_KEY`, `COLLECT_SCHEDULE_ENABLED=true` 를 넣고, `docker-compose.yml` 의 backend 에 `data/` 볼륨을 붙여 컨테이너를 다시 만들어도 진행 위치·후기가 남게 함. 노트북 쪽은 그때 `false` 로 꺼서 두 곳이 같이 돌지 않게
7. 한도: 카카오 키워드 검색 일 10만, 네이버 검색 일 2만 5천, 하루 사용은 각각 수십 번 수준. LLM 은 Groq 무료 한도 때문에 요약 사이 40초(결정 30)

---

## 2단계: 애견동반 확인된 곳만 · 글 10건 이상 · 조건 키워드 필수 · LLM 모델 교체 (2026-11-09 사용자 요청)

> 끊기면: `docs/superpowers/plans/2026-11-09-daily-place-collect.md 의 "2단계" 첫 [ ] 부터 이어서 해 줘. 브랜치 feature/daily-collect.`

사용자 요청: ① Gemini·OpenRouter 응답 없음 고치기 ② 장소마다 블로그 글 10건 이상 모아서 요약 ③ 조건 키워드(목줄 필수, 소형견 입장 등) 꼭 뽑기 ④ 일반 카페가 아니라 애견동반 되는 곳만 ⑤ 점검 카페·점검 공원 삭제

원인(①): 모델이 내려감 — Gemini `gemini-2.5-flash-lite` 404 "no longer available to new users", OpenRouter `deepseek/deepseek-chat-v3-0324:free` 404 "unavailable for free". 직접 호출로 확인한 대체: `gemini-3.5-flash-lite`(1.0초), OpenRouter `nvidia/nemotron-3.5-super-vl:free`(1.9초) — 둘 다 한국어 조건 판정 JSON 정답

규칙: "확인된 애견동반 장소" = 요약에 쓴 글(협찬 아님 + 반려견 문장 있음) **10건 이상** + 조건 고정 키워드 **1개 이상**. DB 에는 이것만 import. 미확인 장소는 14일 뒤 다시 시도(매일 같은 곳만 고르지 않게)

- [x] 2-1 `application.yml`: `llm.gemini.model: gemini-3.5-flash-lite`, `llm.openrouter.model` 기본값 `nvidia/nemotron-3.5-super-vl:free` → 커밋
- [x] 2-2 `BlogCollectService`: 검색어 `{이름} 애견동반`, `{이름} 강아지`, `{이름} 반려견` 세 번 검색해 URL 로 합침 (테스트 갱신) → 커밋
- [x] 2-3 `ReviewSummaryService`: 쓸 수 있는 글 < 10 이면 LLM 안 부르고 `NOT_ENOUGH_SOURCE` + `static boolean isConfirmed(ReviewSummary)` (sourceCount ≥ 10 && 조건 고정 태그 ≥ 1) (테스트) → 커밋
- [x] 2-4 `DailyCollectJob`/`CollectPlanner`/`CollectJobStore`: 확인된 곳만 import, 미확인 장소 14일 쉬기(`State.retryAfter` 맵), 기록에 `confirmed` (테스트) → 커밋
- [x] 2-5 DB 정리: 사용자 요청("데이터베이스를 지우고 … 30곳을 수집해봐")으로 장소 관련 6개 테이블(place·place_condition·place_source·place_edit_request·course·course_place) 비움, 회원·반려견은 유지. 백업: DB 는 mysqldump(작업용 임시 폴더), 수집 파일은 `backend/data-backup-20261109/`(git 제외)
- [x] 2-6 다른 포트로 백엔드 띄워 AI 상담 3개 모델 응답·수집 한 번 확인 → 결과 기록·커밋

### 2단계 첫 실제 수집 (2026-11-09 12:50~12:57, DB 비운 뒤 30곳)

평촌 검색어 6개 → 후보 42곳 → 블로그 30곳 → 글 10건 이상 9곳 요약 → 조건 키워드 있는 2곳만 DB(에그룸, 올웨이즈 선데이). 실패 0. AI 상담은 3개 모델 모두 응답(Gemini 1.3초, Groq 1.2초, OpenRouter 11.9초)
원인 발견: 글이 많은 7곳(아카루이 29건, 아띠 26건 …)이 키워드 0개 → Groq 이 `"sentence":"1"` 처럼 문장 번호를 문자로 줄 때 판정을 통째로 버리던 버그. 수정 커밋 b31f22f (테스트 추가)

- [x] 2-7 새 코드로 백엔드 다시 띄우고, 키워드 0개였던 7곳의 요약 파일을 지우고 쉬는 목록에서 빼서 다시 요약 → DB 반영 확인

- [x] 2-8 (재수집 끝난 뒤) 수집 시각: 배포 전 노트북은 매일 15:00, 배포 서버(prod 프로필)는 매일 03:00. `.env` 의 `COLLECT_CRON` 으로 바꿀 수 있게(데이터가 충분히 쌓이면 한 달에 한 번, 예 `0 0 3 1 * *`). `.env.example`·운영 메모 갱신 → 테스트 → 커밋 → 백엔드 다시 띄우기 (사용자 결정 2026-11-09)

### 재수집 결과 (2026-11-09 13:03~13:16, 버그 수정 후)

검색어 `평촌 펫프렌들리`, `범계 …` 5개 → 새 장소 24곳(후보 66) → 블로그 30곳 → 요약 17곳 → **DB 18곳**(키워드 0개였던 7곳 모두 키워드 나옴), 미확인 14곳은 14일 쉼, 실패 0. 2-6 의 AI 상담 3개 모델 응답도 확인(Gemini 1.3초, Groq 1.2초, OpenRouter 11.9초)
열린 질문: 주차 키워드(#주차가능/#주차불가)만으로 확인된 곳이 5곳 — 반려견 조건(크기·목줄·실내 등)만 확인 조건으로 칠지 사용자 결정 대기

### 규칙 조정 (2026-11-09 사용자 결정)

- 주차(#주차가능/#주차불가)는 반려견 동반과 상관없어서 **확인 조건에서 뺌** — 반려견 조건 8개 중 하나 이상이 있어야 확인. 키워드로는 계속 보여 줌
- 최소 글 수 10 → **5** (`collect.min-sources`). 근거: 블로그 글 있는 36곳 중 10건 이상 19곳, 5건 이상 26곳. 품질은 반려견 조건 키워드 필수 + 다수결(60%)이 지킴
- DB 에 이미 들어간 "주차만" 5곳(비브라·아에이오우·아띠·막스로스터스·아카루이)은 삭제, 글 5~9건 장소는 쉬는 목록에서 빼서 다음 수집 때 요약

### 새 규칙(주차 제외, 글 5건) 수집 결과 (2026-11-09 13:23~13:36)

주차만 확인된 5곳을 DB 에서 지우고(백업 SQL 은 작업용 임시 폴더), 글 5건 이상 8곳을 쉬는 목록에서 뺀 뒤 수집: 범계·인덕원 검색어 6개 → 새 장소 7(후보 73) → 블로그 30 → 요약 19 → **DB 24곳**, 미확인 17, 실패 0

## 3단계: 화면 손보기 (2026-11-09 사용자 요청)

> 끊기면: `docs/superpowers/plans/2026-11-09-daily-place-collect.md 의 "3단계" 첫 [ ] 부터 이어서 해 줘. 브랜치 feature/daily-collect, 디자인(className)은 그대로.`

- [x] 3-1 지도: "우리 강아지 기준" 끄면 목록·상세의 가능/확인 필요/어려움 배지 숨김 (`MapPage`, `PlaceDrawer` 에 `showVerdict`)
- [x] 3-2 코스 화면 산책 팁 섹션 삭제 (`CoursePage`, `constants/care.ts` 의 `WALK_TIP`)
- [x] 3-3 삭제 X 버튼: 건강 기록(이미 `DELETE /api/records/{id}` 있음 → `RecordsPage` 일별 목록), AI 상담 기록(백엔드 `DELETE /api/consultations/{id}` 새로 + 테스트 → `ConsultPage` 지난 상담 목록)
- [x] 3-4 장소 이미지: 지도 API 는 사진을 공식으로 안 줌 → 사용자 결정 "가져올 수 없으면 이미지 칸을 없애줘" → `PlaceDrawer` 사진 칸 삭제, 닫기 버튼·동반 배지는 머리 줄로
- [ ] 3-5 lint·build·test → 브라우저 확인 → 커밋
