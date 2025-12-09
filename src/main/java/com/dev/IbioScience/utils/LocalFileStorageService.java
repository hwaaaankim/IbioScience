package com.dev.IbioScience.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    /**
     * 업로드 루트 디렉토리
     *
     * - application.yml 의 spring.upload.path 값을 그대로 사용
     *   예)
     *   - 로컬: D:/IbioScience/
     *   - 서버: /home/ubuntu/IbioScience/files/
     *
     * 최종 저장 경로 예)
     *   {spring.upload.path}/product/{productId}/review/{memberId}/{yyyyMMdd}/{uuid}.jpg
     */
    private final Path uploadRootDir;

    public LocalFileStorageService(
            @Value("${spring.upload.path}") String uploadRootDir) {

        this.uploadRootDir = Paths.get(uploadRootDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRootDir);
            log.info("업로드 루트 디렉토리: {}", this.uploadRootDir);
        } catch (IOException e) {
            log.error("업로드 루트 디렉토리 생성 실패: {}", this.uploadRootDir, e);
            throw new RuntimeException("업로드 디렉토리를 생성할 수 없습니다.", e);
        }
    }

    @Override
    public FileSaveResult saveReviewImage(Long productId,
                                          Long memberId,
                                          Long reviewId,
                                          MultipartFile file) throws IOException {

        if (productId == null) {
            throw new IllegalArgumentException("상품 ID가 필요합니다.");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID가 필요합니다.");
        }
        if (reviewId == null) {
            throw new IllegalArgumentException("리뷰 ID가 필요합니다.");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = StringUtils.getFilenameExtension(originalFilename);
        String uuid = UUID.randomUUID().toString();

        String saveName = uuid;
        if (StringUtils.hasText(ext)) {
            saveName = saveName + "." + ext;
        }

        // 폴더 구조:
        // {spring.upload.path}/product/{productId}/review/{memberId}/{yyyyMMdd}/
        Path dir = this.uploadRootDir
                .resolve("product")
                .resolve(String.valueOf(productId))
                .resolve("review")
                .resolve(String.valueOf(memberId))
                .resolve(dateStr);

        Files.createDirectories(dir);

        Path target = dir.resolve(saveName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // 실제 파일 시스템상의 전체 경로 (삭제용)
        String storedPath = target.toAbsolutePath().toString();

        // 브라우저에서 접근할 URL
        // WebMvcConfig 의 /upload/** → spring.upload.path 매핑 전제
        // /upload/product/{productId}/review/{memberId}/{yyyyMMdd}/{fileName}
        String relativeUrl = "/upload/product/"
                + productId + "/review/" + memberId + "/" + dateStr + "/" + saveName;

        log.debug("리뷰 이미지 저장 완료. path={}, url={}", storedPath, relativeUrl);

        return new FileSaveResult(storedPath, relativeUrl, saveName);
    }

    @Override
    public void delete(String path) throws IOException {
        if (!StringUtils.hasText(path)) {
            return;
        }

        try {
            Path target = Paths.get(path);
            Files.deleteIfExists(target);
            log.debug("파일 삭제 완료. path={}", path);
        } catch (Exception e) {
            // 파일이 없거나 삭제 실패해도 서비스 전체가 죽지 않도록 경고만 남깁니다.
            log.warn("파일 삭제 실패. path={}", path, e);
        }
    }
}