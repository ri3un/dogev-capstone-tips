package com.gaebalhagae.dog.service;

import com.gaebalhagae.global.response.BusinessException;
import com.gaebalhagae.global.response.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 반려견 사진을 서버 디스크(UPLOAD_DIR/dogs)에 저장합니다. 배포에서는 Nginx 가 /uploads 를 바로 제공합니다.
 * 확장자·Content-Type 은 믿지 않고 파일 앞부분(매직 바이트)으로 형식을 판단합니다.
 */
@Slf4j
@Component
public class PhotoStorage {

    static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final String URL_PREFIX = "/uploads/dogs/";

    private final Path dir;

    public PhotoStorage(@Value("${app.upload-dir}") String uploadDir) {
        this.dir = Path.of(uploadDir, "dogs").toAbsolutePath().normalize();
    }

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
        try {
            byte[] head = new byte[12];
            int read;
            try (InputStream in = file.getInputStream()) {
                read = in.readNBytes(head, 0, head.length);
            }
            String ext = extensionOf(Arrays.copyOf(head, read));
            Files.createDirectories(dir);
            String name = UUID.randomUUID() + "." + ext;
            file.transferTo(dir.resolve(name));
            return URL_PREFIX + name;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "사진을 저장하지 못했습니다");
        }
    }

    /** 우리가 저장한 파일만 지웁니다 (경로 조작 방지) */
    public void delete(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) {
            return;
        }
        Path target = dir.resolve(url.substring(URL_PREFIX.length())).normalize();
        if (!target.startsWith(dir)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("사진 삭제 실패 {}", target, e);
        }
    }

    static String extensionOf(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') {
            return "png";
        }
        if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return "webp";
        }
        throw new BusinessException(ErrorCode.INVALID_FILE);
    }
}
