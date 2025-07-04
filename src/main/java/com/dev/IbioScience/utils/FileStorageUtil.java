package com.dev.IbioScience.utils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileStorageUtil {

    public String save(MultipartFile file, String dirPath) throws IOException {
        File dir = new File(dirPath);
        if (!dir.exists()) dir.mkdirs();

        // 중복 방지 파일명 (타임스탬프+uuid)
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String newName = System.currentTimeMillis() + "_" + UUID.randomUUID() + (ext != null ? "." + ext : "");
        File dest = new File(dir, newName);

        file.transferTo(dest);

        return dest.getAbsolutePath(); // 또는 Web에서 사용할 URL 경로 반환
    }
}
