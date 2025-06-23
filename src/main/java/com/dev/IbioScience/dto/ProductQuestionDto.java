package com.dev.IbioScience.dto;

import java.util.List;

import com.dev.IbioScience.model.product.status.QuestionType;

import lombok.Data;

@Data
public class ProductQuestionDto {
    private Long id; // 기존 항목 수정시 포함, 신규시 null
    private String label;
    private String placeholder;
    private QuestionType type;
    private Boolean required; // 표시여부 or 필수입력여부
    private Integer sortOrder;
    private String display; // (Y/N) 만약 표시여부 따로 관리시
    private List<String> options; // select 옵션 리스트(선택지)
}