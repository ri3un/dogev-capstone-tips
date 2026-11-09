package com.gaebalhagae.place.collect.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gaebalhagae.place.collect.BlogCollectService;
import com.gaebalhagae.place.collect.CollectedPlace;
import com.gaebalhagae.place.collect.CollectedPlaceStore;
import com.gaebalhagae.place.collect.PlaceCollectService;
import com.gaebalhagae.place.dto.PlaceImportRequest;
import com.gaebalhagae.place.review.ReviewSummary;
import com.gaebalhagae.place.review.ReviewSummaryService;
import com.gaebalhagae.place.service.PlaceImportService;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
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
                List.of("평촌", "범계"), List.of("애견동반 카페"), 0, null);
        job = new DailyCollectJob(props, placeCollect, placeStore, blogCollect, summaries, importer, jobStore, clock);

        when(placeCollect.collect(eq(""), eq("경기 안양시"), any())).thenReturn(new PlaceCollectService.CollectResult(
                List.of(new PlaceCollectService.KeywordResult("평촌 애견동반 카페", 10, 5, 2),
                        new PlaceCollectService.KeywordResult("범계 애견동반 카페", 10, 3, 1)), 3));
        when(placeStore.findAll()).thenReturn(List.of(place("1", "멍카페"), place("2", "냥식당"), place("3", "공원")));
        Map<String, ReviewSummary> saved = new HashMap<>();
        when(summaries.findByPlace(anyString())).thenAnswer(a -> Optional.ofNullable(saved.get(a.<String>getArgument(0))));
        when(summaries.isConfirmed(any())).thenAnswer(a -> a.getArgument(0) != null
                && "멍카페".equals(a.<ReviewSummary>getArgument(0).placeName()));
        when(blogCollect.collect(anyString())).thenAnswer(a -> new BlogCollectService.CollectResult(a.getArgument(0), 5, 3, 3, 0, 3));
        when(summaries.summarize(anyString(), eq(false))).thenAnswer(a -> {
            ReviewSummary s = new ReviewSummary(a.getArgument(0), List.of("요약"), List.of(), Map.of(), List.of(), 10,
                    List.of(), "groq", null);
            saved.put(s.placeName(), s);
            return new ReviewSummaryService.SummaryResult(s.placeName(), "SUMMARIZED", 10, s);
        });
        when(importer.importData(any())).thenAnswer(
                a -> new PlaceImportRequest.Result(a.<PlaceImportRequest>getArgument(0).places().size(), 0, 0, List.of()));
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
        ArgumentCaptor<PlaceImportRequest> sent = ArgumentCaptor.forClass(PlaceImportRequest.class);
        order.verify(importer).importData(sent.capture());
        // 애견동반이 확인된 곳(멍카페)만 DB 로, 미확인(냥식당)·아직 안 본 곳(공원)은 넣지 않음
        assertThat(sent.getValue().places()).extracting(CollectedPlace::name).containsExactly("멍카페");
        assertThat(sent.getValue().summaries()).extracting(ReviewSummary::placeName).containsExactly("멍카페");

        assertThat(log.newPlaces()).isEqualTo(3);
        assertThat(log.summarized()).isEqualTo(2);
        assertThat(log.imported()).isEqualTo(1);
        // 검색어 2개를 다 돌아 한 바퀴, 미확인 냥식당은 14일 뒤(11/24)까지 쉼
        assertThat(jobStore.loadState()).isEqualTo(new CollectJobStore.State(0, 1, Map.of("냥식당", LocalDate.of(2026, 11, 24))));
        assertThat(jobStore.recentLogs(1)).hasSize(1);
    }

    @Test
    void 한곳이_실패해도_나머지와_import_는_계속한다() {
        when(blogCollect.collect("멍카페")).thenThrow(new IllegalStateException("네이버 응답 없음"));

        CollectJobStore.RunLog log = job.runOnce();

        assertThat(log.failed()).isEqualTo(1);
        assertThat(log.failedPlaces()).containsExactly("멍카페");
        assertThat(log.summarized()).isEqualTo(1);
        assertThat(log.imported()).isZero(); // 남은 냥식당은 미확인
    }

    @Test
    void 분류를_정하면_그_분류_장소만_고른다() {
        CollectScheduleProperties parkOnly = new CollectScheduleProperties(true, "-", 30, 2, "경기 안양시",
                List.of("평촌"), List.of("강아지 공원"), 0, "PARK");
        DailyCollectJob parkJob = new DailyCollectJob(parkOnly, placeCollect, placeStore, blogCollect, summaries,
                importer, jobStore, clock);
        when(placeStore.findAll()).thenReturn(List.of(place("1", "멍카페"),
                new CollectedPlace("9", "평촌중앙공원", "여행 > 공원", "AT4", "경기 안양시 동안구", "", 37.39, 126.95,
                        "", "", List.of(), null)));

        parkJob.runOnce();

        verify(blogCollect).collect("평촌중앙공원");
        verify(blogCollect, never()).collect("멍카페");
    }

    private static CollectedPlace place(String id, String name) {
        return new CollectedPlace(id, name, "음식점 > 카페", "CE7", "경기 안양시 동안구", "", 37.39, 126.95, "", "", List.of(), null);
    }
}
