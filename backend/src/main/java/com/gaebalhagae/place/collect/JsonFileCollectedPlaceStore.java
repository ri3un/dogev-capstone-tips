package com.gaebalhagae.place.collect;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** DB 없이 쓰는 임시 저장소. 장소 전체를 {collect.place-dir}/places.json 파일 하나에 저장합니다. */
@Component
@ConditionalOnProperty(name = "collect.store", havingValue = "json", matchIfMissing = true)
public class JsonFileCollectedPlaceStore implements CollectedPlaceStore {

    private final ObjectMapper objectMapper;
    private final Path file;

    public JsonFileCollectedPlaceStore(ObjectMapper objectMapper,
                                       @Value("${collect.place-dir:data/place}") String baseDir) {
        this.objectMapper = objectMapper;
        this.file = Path.of(baseDir, "places.json");
    }

    /** 파일 하나의 모양: 마지막 저장 시각, 장소 수, 장소 목록 */
    record PlacesFile(LocalDateTime savedAt, int count, List<CollectedPlace> places) {
    }

    @Override
    public synchronized void saveAll(List<CollectedPlace> places) {
        try {
            Files.createDirectories(file.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file.toFile(), new PlacesFile(LocalDateTime.now(), places.size(), places));
        } catch (IOException e) {
            throw new UncheckedIOException("장소 파일 저장 실패", e);
        }
    }

    @Override
    public List<CollectedPlace> findAll() {
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(file.toFile(), PlacesFile.class).places();
        } catch (IOException e) {
            throw new UncheckedIOException("장소 파일 읽기 실패", e);
        }
    }
}
