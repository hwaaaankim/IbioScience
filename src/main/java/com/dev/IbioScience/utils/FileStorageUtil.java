package com.dev.IbioScience.utils;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileStorageUtil {
    
	public String save(MultipartFile file, String dir) {
        try {
            File d = new File(dir);
            if (!d.exists() && !d.mkdirs()) throw new RuntimeException("디렉토리 생성 실패: " + dir);
            String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            String newName = UUID.randomUUID().toString().replace("-", "") + "_" + original;
            Path target = new File(d, newName).toPath();
            file.transferTo(target);
            return target.toFile().getAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
    }
}
