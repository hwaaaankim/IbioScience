package com.dev.IbioScience.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class UploadPathHelper {

    @Value("${spring.upload.path}")
    private String uploadRoot;

    public Path resolveCustomerBase(Long userId) {
        return Paths.get(uploadRoot, "commonPath", "customer", String.valueOf(userId));
    }

    public Path saveBizRegFileForCustomer(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        Path base = resolveCustomerBase(userId);
        try {
            Files.createDirectories(base);
            String original = StringUtils.cleanPath(file.getOriginalFilename());
            String filename = System.currentTimeMillis() + "_" + original;
            Path target = base.resolve(filename);
            file.transferTo(target.toFile());
            return target.normalize();
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
    }

    public String publicUrlOf(Path savedPath) {
        Path rel = Paths.get(uploadRoot).relativize(savedPath);
        return "/upload/" + rel.toString().replace("\\", "/");
    }
}