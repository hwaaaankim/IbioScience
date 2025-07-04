package com.dev.IbioScience.dto;

import java.util.List;

import lombok.Data;

@Data
public class MoveEditorImageRequestDTO {
    private String type; // "detailHtml" 또는 "question_12" 등
    private String html;
    private List<String> tempImgList;
}