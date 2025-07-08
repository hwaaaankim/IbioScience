package com.dev.IbioScience.dto;

import java.util.List;

import lombok.Data;

// DTO (프론트에서 전달받을 요청 구조)
@Data
public class EditorImageUploadRequestDTO {
    private String html;            // 원본 HTML
    private List<String> base64List; // 에디터에서 추출된 base64 이미지 리스트
    private String type;             // "detailHtml" 또는 "question_1"
}


