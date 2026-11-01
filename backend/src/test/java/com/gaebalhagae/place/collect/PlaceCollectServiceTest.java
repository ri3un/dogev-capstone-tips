package com.gaebalhagae.place.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlaceCollectServiceTest {

    @TempDir
    Path tempDir;

    private final KakaoPlaceClient client = mock(KakaoPlaceClient.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private CollectedPlaceStore store;
    private PlaceCollectService service;

    @BeforeEach
    void setUp() {
        ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());
        store = new JsonFileCollectedPlaceStore(om, tempDir.toString());
        service = new PlaceCollectService(client, store, clock);
    }

    private static KakaoPlaceClient.Document doc(String id, String name, String address) {
        return new KakaoPlaceClient.Document(id, name, "음식점 > 카페", "CE7", "", address, "", "126.95", "37.39", "url" + id);
    }

    @Test
    void 다른_지역_주소는_빼고_저장한다() {
        when(client.search("안양 애견동반 카페")).thenReturn(List.of(
                doc("1", "카페A", "경기 안양시 동안구 평촌동 1"),
                doc("2", "카페B", "경기 군포시 산본동 1")));

        var result = service.collect("안양", "경기 안양시", List.of("애견동반 카페"));

        assertThat(result.keywords().get(0).inArea()).isEqualTo(1);
        assertThat(store.findAll()).extracting(CollectedPlace::name).containsExactly("카페A");
        assertThat(store.findAll().get(0).lat()).isEqualTo(37.39);
    }

    @Test
    void 같은_장소는_한_번만_저장하고_검색어만_추가한다() {
        when(client.search("안양 애견동반 카페")).thenReturn(List.of(doc("1", "카페A", "경기 안양시 동안구 1")));
        when(client.search("안양 애견동반 브런치")).thenReturn(List.of(doc("1", "카페A", "경기 안양시 동안구 1")));

        service.collect("안양", "경기 안양시", List.of("애견동반 카페"));
        var result = service.collect("안양", "경기 안양시", List.of("애견동반 브런치"));

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.keywords().get(0).added()).isZero();
        assertThat(store.findAll().get(0).foundBy()).containsExactly("안양 애견동반 카페", "안양 애견동반 브런치");
    }
}
