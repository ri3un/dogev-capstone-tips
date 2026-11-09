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
