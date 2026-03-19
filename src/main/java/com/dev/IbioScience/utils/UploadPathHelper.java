package com.dev.IbioScience.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class UploadPathHelper {

	private static final Logger log = LoggerFactory.getLogger(UploadPathHelper.class);

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

	public Path resolveSellerLogoBase(Long userId) {
		return Paths.get(uploadRoot, "commonPath", "customer", String.valueOf(userId), "seller-logo");
	}

	public Path saveSellerLogoForCustomer(Long userId, MultipartFile file) {
		if (file == null || file.isEmpty()) return null;

		Path base = resolveSellerLogoBase(userId);
		try {
			Files.createDirectories(base);
			String original = StringUtils.cleanPath(file.getOriginalFilename());
			String filename = System.currentTimeMillis() + "_" + original;
			Path target = base.resolve(filename);
			file.transferTo(target.toFile());
			return target.normalize();
		} catch (IOException e) {
			throw new RuntimeException("셀러 로고 저장 실패", e);
		}
	}

	public String publicUrlOf(Path savedPath) {
		Path rel = Paths.get(uploadRoot).relativize(savedPath);
		return "/upload/" + rel.toString().replace("\\", "/");
	}

	public void deleteIfExists(Path path) {
		if (path == null) return;

		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			throw new RuntimeException("파일 삭제 실패: " + path, e);
		}
	}

	public void deleteIfExistsQuietly(Path path) {
		if (path == null) return;

		try {
			Files.deleteIfExists(path);
		} catch (Exception e) {
			log.error("파일 삭제 실패 path={}", path, e);
		}
	}
}