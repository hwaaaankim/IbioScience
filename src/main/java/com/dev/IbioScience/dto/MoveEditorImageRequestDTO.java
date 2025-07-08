package com.dev.IbioScience.dto;

import java.util.List;

import lombok.Data;

@Data
public class MoveEditorImageRequestDTO {
    private String type; // "detailHtml" 또는 "question"
    private String key;  // "detailHtml" 또는 "question_답변ID"
    private String html;
    private List<String> tempImgList;
}