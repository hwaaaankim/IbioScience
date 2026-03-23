package com.dev.IbioScience.service.seller.product;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.seller.product.EditorTempImageUploadResponse;
import com.dev.IbioScience.model.product.dealer.DealerProductDetailImage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Service
public class SellerProductFileService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${spring.upload.path}")
    private String uploadPath;

    public EditorTempImageUploadResponse storeTempEditorImage(Long sellerMemberId, MultipartFile file) {
        StoredFileInfo stored = storeImageFile(sellerMemberId, "temp", file, true);
        return EditorTempImageUploadResponse.builder()
                .url(stored.getUrl())
                .build();
    }

    public StoredFileInfo storeRepresentativeImage(Long sellerMemberId, MultipartFile file) {
        return storeImageFile(sellerMemberId, "repImage", file, false);
    }

    public List<StoredFileInfo> storeAdditionalImages(Long sellerMemberId, List<MultipartFile> files) {
        List<StoredFileInfo> result = new ArrayList<>();
        if (files == null) {
            return result;
        }

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                result.add(storeImageFile(sellerMemberId, "addedImage", file, false));
            }
        }
        return result;
    }

    public StoredFileInfo storeIconImage(Long sellerMemberId, MultipartFile file) {
        return storeImageFile(sellerMemberId, "icon", file, false);
    }

    public FinalizedDetailHtml finalizeDetailHtml(Long sellerMemberId, String detailHtml) {
        if (detailHtml == null || detailHtml.isBlank()) {
            return FinalizedDetailHtml.builder()
                    .finalHtml(detailHtml)
                    .detailImages(new ArrayList<>())
                    .build();
        }

        Document doc = Jsoup.parseBodyFragment(detailHtml);
        String tempPrefix = "/upload/sellerFile/" + sellerMemberId + "/temp/";
        LocalDate today = LocalDate.now();

        List<DealerProductDetailImage> detailImages = new ArrayList<>();
        Map<String, StoredFileInfo> movedMap = new LinkedHashMap<>();

        int sortOrder = 0;

        for (Element img : doc.select("img[src]")) {
            String src = img.attr("src");
            if (src == null || src.isBlank()) {
                continue;
            }

            if (!src.startsWith(tempPrefix)) {
                continue;
            }

            StoredFileInfo movedInfo = movedMap.get(src);
            if (movedInfo == null) {
                movedInfo = moveTempFileToDetail(sellerMemberId, src, today);
                movedMap.put(src, movedInfo);
            }

            img.attr("src", movedInfo.getUrl());

            DealerProductDetailImage entity = new DealerProductDetailImage();
            entity.setUrl(movedInfo.getUrl());
            entity.setPath(movedInfo.getPath());
            entity.setFileName(movedInfo.getFileName());
            entity.setOriginalFilename(movedInfo.getOriginalFilename());
            entity.setSize(movedInfo.getSize());
            entity.setUploadedAt(LocalDateTime.now());
            entity.setInUse(Boolean.TRUE);
            entity.setSortOrder(sortOrder++);
            detailImages.add(entity);
        }

        return FinalizedDetailHtml.builder()
                .finalHtml(doc.body().html())
                .detailImages(detailImages)
                .build();
    }

    private StoredFileInfo moveTempFileToDetail(Long sellerMemberId, String tempUrl, LocalDate targetDate) {
        try {
            Path sourcePath = resolveUploadUrlToAbsolutePath(tempUrl);
            if (!Files.exists(sourcePath)) {
                throw new IllegalArgumentException("에디터 임시 이미지가 존재하지 않습니다. url=" + tempUrl);
            }

            String fileName = sourcePath.getFileName().toString();
            String originalFilename = extractOriginalFilename(fileName);
            long size = Files.size(sourcePath);

            Path relativeTarget = buildRelativePath(sellerMemberId, "detailImage", targetDate, fileName);
            Path absoluteTarget = Paths.get(uploadPath).resolve(relativeTarget).normalize();

            Files.createDirectories(absoluteTarget.getParent());
            Files.move(sourcePath, absoluteTarget, StandardCopyOption.REPLACE_EXISTING);

            return StoredFileInfo.builder()
                    .url(toUploadUrl(relativeTarget))
                    .path(absoluteTarget.toString())
                    .fileName(fileName)
                    .originalFilename(originalFilename)
                    .size((int) size)
                    .build();

        } catch (IOException e) {
            throw new IllegalStateException("에디터 이미지를 실제 상세 이미지 경로로 이동하는 중 오류가 발생했습니다.", e);
        }
    }

    private StoredFileInfo storeImageFile(Long sellerMemberId, String directoryType, MultipartFile file, boolean keepOriginalNameInfo) {
        validateImageFile(file);

        try {
            LocalDate today = LocalDate.now();
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
            String storedFileName = buildStoredFileName(originalFilename, keepOriginalNameInfo);

            Path relativePath = buildRelativePath(sellerMemberId, directoryType, today, storedFileName);
            Path absolutePath = Paths.get(uploadPath).resolve(relativePath).normalize();

            Files.createDirectories(absolutePath.getParent());
            file.transferTo(absolutePath.toFile());

            return StoredFileInfo.builder()
                    .url(toUploadUrl(relativePath))
                    .path(absolutePath.toString())
                    .fileName(storedFileName)
                    .originalFilename(originalFilename)
                    .size((int) file.getSize())
                    .build();

        } catch (IOException e) {
            throw new IllegalStateException("파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지 파일이 없습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }
    }

    private Path resolveUploadUrlToAbsolutePath(String uploadUrl) {
        String prefix = "/upload/";
        if (!uploadUrl.startsWith(prefix)) {
            throw new IllegalArgumentException("잘못된 업로드 URL입니다. url=" + uploadUrl);
        }

        String relativeText = uploadUrl.substring(prefix.length());
        Path relativePath = Paths.get(relativeText).normalize();
        return Paths.get(uploadPath).resolve(relativePath).normalize();
    }

    private Path buildRelativePath(Long sellerMemberId, String directoryType, LocalDate date, String fileName) {
        return Paths.get(
                "sellerFile",
                String.valueOf(sellerMemberId),
                directoryType,
                DATE_FORMATTER.format(date),
                fileName
        );
    }

    private String toUploadUrl(Path relativePath) {
        return "/upload/" + relativePath.toString().replace(File.separatorChar, '/');
    }

    private String buildStoredFileName(String originalFilename, boolean keepOriginalNameInfo) {
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex);
        }

        String baseName = dotIndex >= 0 ? originalFilename.substring(0, dotIndex) : originalFilename;
        baseName = baseName.replaceAll("[^a-zA-Z0-9가-힣._-]", "_");

        String uuid = UUID.randomUUID().toString().replace("-", "");
        if (keepOriginalNameInfo) {
            return uuid + "__" + baseName + extension;
        }
        return uuid + "_" + baseName + extension;
    }

    private String extractOriginalFilename(String storedFileName) {
        int idx = storedFileName.indexOf("__");
        if (idx < 0) {
            return storedFileName;
        }
        return storedFileName.substring(idx + 2);
    }

    @Getter
    @Builder
    public static class StoredFileInfo {
        private String url;
        private String path;
        private String fileName;
        private String originalFilename;
        private Integer size;
    }

    @Getter
    @Builder
    public static class FinalizedDetailHtml {
        private String finalHtml;
        private List<DealerProductDetailImage> detailImages;
    }

    public StoredFile saveRepresentativeImage(Long sellerMemberId, MultipartFile file) {
        return store(file, buildRelativeDir(sellerMemberId, "repImage"));
    }

    public StoredFile saveAdditionalImage(Long sellerMemberId, MultipartFile file) {
        return store(file, buildRelativeDir(sellerMemberId, "addedImage"));
    }

    public StoredFile saveIconImage(Long sellerMemberId, MultipartFile file) {
        return store(file, buildRelativeDir(sellerMemberId, "icon"));
    }

    public EditorSyncResult moveTempEditorImagesToDetailDirectory(Long sellerMemberId, String rawHtml) throws IOException {
        String safeHtml = rawHtml == null ? "" : rawHtml;
        Document document = Jsoup.parseBodyFragment(safeHtml);

        List<StoredFile> finalImages = new ArrayList<>();

        for (Element img : document.select("img[src]")) {
            String src = img.attr("src");

            if (!hasText(src)) {
                continue;
            }

            if (src.startsWith("/upload/sellerFile/" + sellerMemberId + "/temp/")) {
                String relativeUploadPath = src.replaceFirst("^/upload/", "");
                Path source = Paths.get(uploadPath).resolve(relativeUploadPath);

                if (Files.exists(source)) {
                    String fileName = source.getFileName().toString();
                    Path targetDir = Paths.get(uploadPath).resolve(buildRelativeDir(sellerMemberId, "detailImage"));
                    Files.createDirectories(targetDir);

                    Path target = uniqueTarget(targetDir, fileName);
                    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

                    String relativeTarget = Paths.get(uploadPath).relativize(target).toString().replace("\\", "/");
                    String url = "/upload/" + relativeTarget;

                    img.attr("src", url);

                    finalImages.add(buildStoredFileFromPath(target, fileName, url));
                }
            }
        }

        for (Element img : document.select("img[src]")) {
            String src = img.attr("src");
            if (!hasText(src)) {
                continue;
            }

            if (src.startsWith("/upload/sellerFile/" + sellerMemberId + "/detailImage/")) {
                boolean alreadyAdded = finalImages.stream().anyMatch(item -> item.getUrl().equals(src));
                if (alreadyAdded) {
                    continue;
                }

                String relativeUploadPath = src.replaceFirst("^/upload/", "");
                Path file = Paths.get(uploadPath).resolve(relativeUploadPath);

                if (Files.exists(file)) {
                    finalImages.add(buildStoredFileFromPath(file, file.getFileName().toString(), src));
                }
            }
        }

        return new EditorSyncResult(document.body().html(), finalImages);
    }

    public void deleteFileQuietly(String absolutePath) {
        if (!hasText(absolutePath)) {
            return;
        }

        try {
            Files.deleteIfExists(Paths.get(absolutePath));
        } catch (Exception ignored) {
        }
    }

    private StoredFile store(MultipartFile file, String relativeDir) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("업로드 파일이 비어 있습니다.");
            }

            Path targetDir = Paths.get(uploadPath).resolve(relativeDir);
            Files.createDirectories(targetDir);

            String originalFilename = file.getOriginalFilename();
            String extension = extractExtension(originalFilename);
            String savedFileName = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);

            Path targetFile = targetDir.resolve(savedFileName);
            file.transferTo(targetFile);

            String relativePath = Paths.get(uploadPath).relativize(targetFile).toString().replace("\\", "/");
            String url = "/upload/" + relativePath;

            return new StoredFile(
                    targetFile.toAbsolutePath().toString(),
                    url,
                    savedFileName,
                    originalFilename,
                    Math.toIntExact(file.getSize())
            );
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    private StoredFile buildStoredFileFromPath(Path path, String fileName, String url) throws IOException {
        return new StoredFile(
                path.toAbsolutePath().toString(),
                url,
                fileName,
                fileName,
                (int) Files.size(path)
        );
    }

    private Path uniqueTarget(Path targetDir, String fileName) {
        String extension = extractExtension(fileName);
        String baseName = UUID.randomUUID().toString();
        String finalFileName = extension.isBlank() ? baseName : baseName + "." + extension;
        return targetDir.resolve(finalFileName);
    }

    private String buildRelativeDir(Long sellerMemberId, String leaf) {
        String date = LocalDate.now().toString();
        return "sellerFile/" + sellerMemberId + "/" + leaf + "/" + date + "/";
    }

    private String extractExtension(String fileName) {
        if (!hasText(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Getter
    @AllArgsConstructor
    public static class StoredFile {
        private String path;
        private String url;
        private String fileName;
        private String originalFilename;
        private Integer size;
    }

    @Getter
    @AllArgsConstructor
    public static class EditorSyncResult {
        private String html;
        private List<StoredFile> images;
    }
}