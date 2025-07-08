package com.dev.IbioScience.dto;

import java.util.List;

import lombok.Data;

//결과 DTO
@Data
public class EditorImageUploadResultDTO {
    private String newHtml;          // 교체된 HTML (base64 → 실제 URL로 변경된 src)
    private List<String> imageUrls;  // 업로드된 실제 이미지 URL 리스트
}
