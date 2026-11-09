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

- [ ] **Step 1: 구현**

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

- [ ] **Step 2: 전체 테스트** — `cd backend && ./gradlew test` → PASS
- [ ] **Step 3: 커밋** — `feat: 관리자용 매일 수집 지금 실행·실행 기록 API 추가`

---

### Task 5: 노트북에서 실제로 한 번 돌려 확인

- [ ] **Step 1:** 루트 `.env` 에 `COLLECT_SCHEDULE_ENABLED=true` 추가 (값 출력 금지)
- [ ] **Step 2:** `cd backend && ./gradlew bootRun --args='--collect.schedule.daily-places=2 --collect.schedule.queries-per-day=1'` → `Started BackendApplication` (확인 동안만 2곳·검색어 1개)
- [ ] **Step 3:** 개발용 로그인(관리자) 토큰으로 `POST /api/admin/collect/run` → `{"started":true}`, 몇 분 뒤 `GET /api/admin/collect/logs?n=1` → `newPlaces`·`summarized`·`imported` 확인, `error` 없음
- [ ] **Step 4:** `GET /api/places` 에 새 장소가 늘었는지, 요약된 곳의 상세에 3줄 요약이 있는지 확인
- [ ] **Step 5:** 백엔드 끄기, 이 계획 체크 갱신 후 커밋 (`docs: 매일 수집 노트북 확인`)

### Task 6: 노트북에서 매일 돌리기 안내 + 마무리

- [ ] **Step 1:** 이 문서 끝 "운영 메모"에 적기: ① `.env` 에 `COLLECT_SCHEDULE_ENABLED=true` ② 03:00 에 노트북이 켜져 있고 `./gradlew bootRun` 이 떠 있어야 함 ③ 진행은 `backend/data/collect-log.jsonl` 또는 `GET /api/admin/collect/logs` ④ 로그·`collect-state.json` 의 `rounds` 가 1 이 되면 안양 검색어 한 바퀴 완료 → 다른 시 추가(`areas`, `address-prefix`) ⑤ 배포 서버로 옮길 때: 서버 `.env` 에 네이버 검색·카카오·LLM 키 + `COLLECT_SCHEDULE_ENABLED=true`, `data/` 를 Docker 볼륨으로
- [ ] **Step 2:** `./gradlew test` 통과 확인 → 커밋 → 사용자에게 push·PR 여부 묻기

---

## Self-Review 메모

- 사용자 결정 ①(노트북 먼저·나중에 서버): Task 3 `enabled` + Task 6 ✔ ②(import 자동): Task 3 ③단계 ✔ ③(안양 안에서 넓히고 다 돌면 다른 시): Task 1 순환 + `rounds` + Task 6 ✔
- 하루 30곳 `daily-places: 30` ✔, 실행 기록 Task 2·3 ✔
- 타입 이름: `CollectPlanner.Queries`, `CollectJobStore.State/RunLog`, `CollectScheduleProperties`, `DailyCollectJob.runOnce(): CollectJobStore.RunLog` — Task 1~4 같은 이름 ✔
