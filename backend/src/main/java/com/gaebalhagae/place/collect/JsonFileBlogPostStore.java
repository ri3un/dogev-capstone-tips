package com.gaebalhagae.place.collect;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * DB 없이 쓰는 임시 저장소. 장소마다 {collect.dir}/{장소명}.json 파일 하나에 저장합니다.
 * collect.store=json 일 때(기본값) 사용되고, DB가 생기면 collect.store=jpa 구현체로 교체합니다.
 */
@Component
@ConditionalOnProperty(name = "collect.store", havingValue = "json", matchIfMissing = true)
public class JsonFileBlogPostStore implements BlogPostStore {

    private final ObjectMapper objectMapper;
    private final Path baseDir;

    public JsonFileBlogPostStore(ObjectMapper objectMapper, @Value("${collect.dir:data/blog}") String baseDir) {
        this.objectMapper = objectMapper;
        this.baseDir = Path.of(baseDir);
    }

    /** 파일 하나의 모양: 장소명, 마지막 수집 시각, 글 목록 */
    record PlaceFile(String placeName, LocalDateTime collectedAt, List<BlogPost> posts) {
    }

    @Override
    public synchronized void saveAll(String placeName, List<BlogPost> posts) {
        List<BlogPost> merged = new ArrayList<>(findByPlace(placeName));
        merged.addAll(posts);
        try {
            Files.createDirectories(baseDir);
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(fileOf(placeName).toFile(), new PlaceFile(placeName, LocalDateTime.now(), merged));
        } catch (IOException e) {
            throw new UncheckedIOException("블로그 수집 파일 저장 실패: " + placeName, e);
        }
    }

    @Override
    public List<BlogPost> findByPlace(String placeName) {
        Path file = fileOf(placeName);
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(file.toFile(), PlaceFile.class).posts();
        } catch (IOException e) {
            throw new UncheckedIOException("블로그 수집 파일 읽기 실패: " + placeName, e);
        }
    }

    @Override
    public boolean existsByUrl(String placeName, String url) {
        return findByPlace(placeName).stream().anyMatch(p -> p.url().equals(url));
    }

    // 윈도우 파일 이름에 쓸 수 없는 문자와 공백은 _ 로 바꿈
    private Path fileOf(String placeName) {
        return baseDir.resolve(placeName.replaceAll("[\\\\/:*?\"<>|\\s]+", "_") + ".json");
    }
}
