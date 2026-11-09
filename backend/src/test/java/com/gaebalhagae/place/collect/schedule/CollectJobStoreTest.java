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
