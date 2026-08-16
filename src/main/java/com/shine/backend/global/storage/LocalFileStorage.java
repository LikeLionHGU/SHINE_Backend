package com.shine.backend.global.storage;

import com.shine.backend.global.exception.BusinessException;
import com.shine.backend.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 서버 디스크에 저장한다.
 *
 * 키 규칙: sheets/{userId}/{yyyy-MM-dd}/{uuid}.{ext}
 * 사용자별로 폴더가 나뉘어 있어 탈퇴 시 통째로 지우기 쉽다.
 */
@Slf4j
@Component
public class LocalFileStorage implements FileStorage {

    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "heic", "webp");

    private final Path root;

    public LocalFileStorage(@Value("${app.storage.path}") String path) {
        this.root = Paths.get(path).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            log.info("이미지 저장 경로: {}", root);
        } catch (IOException e) {
            throw new IllegalStateException("저장 폴더를 만들지 못했습니다: " + root, e);
        }
    }

    @Override
    public List<String> storeAll(List<MultipartFile> files, Long userId) {
        List<String> keys = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                keys.add(store(file, userId));
            }
            return keys;
        } catch (RuntimeException e) {
            // 중간에 실패하면 이미 저장한 것도 지운다. 고아 파일이 남으면 디스크만 먹는다.
            keys.forEach(this::delete);
            throw e;
        }
    }

    @Override
    public String store(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "빈 파일입니다.");
        }

        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED.contains(ext)) {
            throw new BusinessException(ErrorCode.INVALID_CONTENT_TYPE);
        }

        String key = "sheets/%d/%s/%s.%s".formatted(
                userId, LocalDate.now(), UUID.randomUUID(), ext);
        Path target = resolve(key);

        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return key;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "파일 저장에 실패했습니다.");
        }
    }

    @Override
    public InputStream read(String key) {
        try {
            return Files.newInputStream(resolve(key));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "이미지를 찾을 수 없습니다.");
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            log.warn("이미지 삭제 실패 key={}", key, e);
        }
    }

    @Override
    public void deleteAllByUser(Long userId) {
        Path dir = root.resolve("sheets").resolve(String.valueOf(userId));
        if (!Files.exists(dir)) return;

        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        } catch (IOException e) {
            log.warn("사용자 이미지 폴더 삭제 실패 userId={}", userId, e);
        }
    }

    /**
     * 경로 조작을 막는다.
     * key에 "../"가 섞여 들어오면 저장 폴더 밖의 파일을 읽거나 지울 수 있다.
     */
    private Path resolve(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return target;
    }

    private String extensionOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase();
    }
}
