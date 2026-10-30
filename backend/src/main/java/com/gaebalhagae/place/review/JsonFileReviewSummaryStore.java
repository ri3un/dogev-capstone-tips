package com.gaebalhagae.place.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** DB 없이 쓰는 임시 저장소. 장소마다 {collect.summary-dir}/{장소명}.json 에 덮어씁니다. */
@Component
@ConditionalOnProperty(name = "collect.store", havingValue = "json", matchIfMissing = true)
public class JsonFileReviewSummaryStore implements ReviewSummaryStore {

    private final ObjectMapper objectMapper;
    private final Path baseDir;

    public JsonFileReviewSummaryStore(ObjectMapper objectMapper,
                                      @Value("${collect.summary-dir:data/summary}") String baseDir) {
        this.objectMapper = objectMapper;
        this.baseDir = Path.of(baseDir);
    }

    @Override
    public void save(ReviewSummary summary) {
        try {
            Files.createDirectories(baseDir);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(fileOf(summary.placeName()).toFile(), summary);
        } catch (IOException e) {
            throw new UncheckedIOException("요약 파일 저장 실패: " + summary.placeName(), e);
        }
    }

    @Override
    public Optional<ReviewSummary> findByPlace(String placeName) {
        Path file = fileOf(placeName);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(file.toFile(), ReviewSummary.class));
        } catch (IOException e) {
            throw new UncheckedIOException("요약 파일 읽기 실패: " + placeName, e);
        }
    }

    // 블로그 수집 파일과 같은 규칙으로 파일 이름을 만듦
    private Path fileOf(String placeName) {
        return baseDir.resolve(placeName.replaceAll("[\\\\/:*?\"<>|\\s]+", "_") + ".json");
    }
}
