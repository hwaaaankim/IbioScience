package com.dev.IbioScience.dto.estimate;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstimateListItemDto {

    private Long id;

    private String answerStatus;
    private String answerStatusLabel;

    private String checkStatus;
    private String checkStatusLabel;

    private String title;

    /** 문의한 상품명 요약 (예: 제품A, 제품B, 제품C) */
    private String productSummary;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime answeredAt;
}