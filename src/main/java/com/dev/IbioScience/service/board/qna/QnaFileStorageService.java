package com.dev.IbioScience.service.board.qna;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class QnaFileStorageService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final SecureRandom RND = new SecureRandom();
    private static final String ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @Value("${spring.upload.path}")
    private String uploadPath; // 예: D:/IbioScience/

    /**
     * CKEditor 임시 업로드 저장
     * - {uploadPath}/qna/temp/{yyyy-MM-dd}/랜덤16자리.확장자
     * - 반환 URL: /upload/qna/temp/{yyyy-MM-dd}/파일명
     */
    public String saveTemp(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("file required");

        String day = LocalDate.now().format(DAY_FMT);
        Path dir = Paths.get(uploadPath, "qna", "temp", day);
        Files.createDirectories(dir);

        String ext = getExt(file.getOriginalFilename());
        String storedName = random16() + ext;

        Path target = dir.resolve(storedName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return "/upload/qna/temp/" + day + "/" + storedName;
    }

    /**
     * URL(/upload/**)을 uploadPath 기준 실제 파일 절대경로로 변환
     * 예) /upload/qna/temp/2026-01-07/abc.jpg
     *  -> {uploadPath}/qna/temp/2026-01-07/abc.jpg
     */
    public Path toAbsolutePathByUploadUrl(String uploadUrl) {
        if (!StringUtils.hasText(uploadUrl)) throw new IllegalArgumentException("uploadUrl required");
        if (!uploadUrl.startsWith("/upload/")) {
            throw new IllegalArgumentException("not an upload url: " + uploadUrl);
        }
        String relative = uploadUrl.substring("/upload/".length()); // qna/temp/...
        return Paths.get(uploadPath, relative);
    }

    /**
     * temp URL을 main으로 이동
     * tempUrl이 temp가 아니면 moved=false로 그대로 반환(치환 불필요)
     */
    public MoveResult moveTempToMain(Long qnaId, String tempUrl) throws IOException {
        if (qnaId == null || qnaId <= 0) throw new IllegalArgumentException("qnaId required");
        if (!StringUtils.hasText(tempUrl)) throw new IllegalArgumentException("tempUrl required");

        if (!tempUrl.startsWith("/upload/qna/temp/")) {
            return new MoveResult(tempUrl, toAbsolutePathSafe(tempUrl), false);
        }

        Path tempFile = toAbsolutePathByUploadUrl(tempUrl);
        if (!Files.exists(tempFile)) {
            throw new IllegalStateException("temp file not found: " + tempUrl);
        }

        String day = LocalDate.now().format(DAY_FMT);
        Path mainDir = Paths.get(uploadPath, "qna", "main", String.valueOf(qnaId), day);
        Files.createDirectories(mainDir);

        String fileName = tempFile.getFileName().toString();
        Path mainFile = mainDir.resolve(fileName);

        Files.move(tempFile, mainFile, StandardCopyOption.REPLACE_EXISTING);

        String mainUrl = "/upload/qna/main/" + qnaId + "/" + day + "/" + fileName;
        return new MoveResult(mainUrl, mainFile.toString(), true);
    }

    /**
     * /upload/** 로컬 파일이면 실제 파일 삭제
     * (외부 URL은 무시)
     */
    public void deleteByUrlIfLocalUpload(String url) {
        try {
            if (!StringUtils.hasText(url)) return;
            if (!url.startsWith("/upload/")) return;

            Path file = toAbsolutePathByUploadUrl(url);
            if (Files.exists(file)) {
                Files.delete(file);
            }
        } catch (Exception e) {
            // 운영에서는 로깅 권장
        }
    }

    /**
     * 안전하게 url->storedPath 문자열을 얻기 위한 헬퍼
     * (uploadUrl 아니면 null 반환)
     */
    public String toAbsolutePathSafe(String url) {
        try {
            if (!StringUtils.hasText(url)) return null;
            if (!url.startsWith("/upload/")) return null;
            return toAbsolutePathByUploadUrl(url).toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String getExt(String original) {
        if (!StringUtils.hasText(original)) return "";
        int idx = original.lastIndexOf('.');
        if (idx < 0) return "";
        String ext = original.substring(idx).trim();
        if (ext.length() > 10) return "";
        return ext;
    }

    private static String random16() {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(ALPHANUM.charAt(RND.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }

    public record MoveResult(String url, String storedPath, boolean moved) {}
}