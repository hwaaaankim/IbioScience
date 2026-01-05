package com.dev.IbioScience.service.board.event;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

public class EventFileStorageUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] RAND_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private EventFileStorageUtil() {}

    public static String todayFolder() {
        return LocalDate.now().format(DAY_FMT);
    }

    public static String sanitizeOriginalName(String original) {
        if (!StringUtils.hasText(original)) return "file";
        // 경로 주입 방지
        String name = original.replace("\\", "/");
        name = name.substring(name.lastIndexOf("/") + 1);
        return name;
    }

    public static String getExt(String originalName) {
        if (!StringUtils.hasText(originalName)) return "";
        int idx = originalName.lastIndexOf('.');
        if (idx < 0) return "";
        String ext = originalName.substring(idx).toLowerCase(); // ".png"
        // 너무 긴 확장자 방지
        if (ext.length() > 10) return "";
        return ext;
    }

    public static String random16() {
        char[] out = new char[16];
        for (int i = 0; i < 16; i++) {
            out[i] = RAND_CHARS[SECURE_RANDOM.nextInt(RAND_CHARS.length)];
        }
        return new String(out);
    }

    public static StoredFile saveToDir(String uploadBasePath, String relDir, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("file required");

        String originalName = sanitizeOriginalName(file.getOriginalFilename());
        String ext = getExt(originalName);
        String storedName = random16() + ext;

        Path dir = Paths.get(uploadBasePath, relDir);
        Files.createDirectories(dir);

        Path target = dir.resolve(storedName);
        // 덮어쓰기 방지: 혹시 충돌나면 재시도
        for (int i = 0; i < 5 && Files.exists(target); i++) {
            storedName = random16() + ext;
            target = dir.resolve(storedName);
        }
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        long size = Files.size(target);

        return StoredFile.builder()
                .originalName(originalName)
                .storedName(storedName)
                .size(size)
                .relPath(Paths.get(relDir, storedName).toString().replace("\\", "/"))
                .build();
    }

    public static void deleteIfExists(String uploadBasePath, String relPath) {
        if (!StringUtils.hasText(relPath)) return;
        try {
            Path p = Paths.get(uploadBasePath, relPath);
            Files.deleteIfExists(p);
        } catch (Exception ignore) {
            // 운영에서는 로깅 권장
        }
    }

    public static MovedFile move(String uploadBasePath, String fromRelPath, String toRelDir) throws IOException {
        if (!StringUtils.hasText(fromRelPath)) throw new IllegalArgumentException("fromRelPath required");
        Path from = Paths.get(uploadBasePath, fromRelPath);
        if (!Files.exists(from)) throw new NoSuchFileException("temp file not found: " + fromRelPath);

        Files.createDirectories(Paths.get(uploadBasePath, toRelDir));

        String fileName = from.getFileName().toString();
        Path to = Paths.get(uploadBasePath, toRelDir, fileName);

        Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);

        long size = Files.size(to);

        return MovedFile.builder()
                .storedName(fileName)
                .size(size)
                .toRelPath(Paths.get(toRelDir, fileName).toString().replace("\\", "/"))
                .build();
    }

    @lombok.Builder
    @lombok.Getter
    public static class StoredFile {
        private final String originalName;
        private final String storedName;
        private final String relPath;
        private final long size;
    }

    @lombok.Builder
    @lombok.Getter
    public static class MovedFile {
        private final String storedName;
        private final String toRelPath;
        private final long size;
    }
}