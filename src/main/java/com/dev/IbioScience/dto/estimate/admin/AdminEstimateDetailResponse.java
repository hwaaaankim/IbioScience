package com.dev.IbioScience.dto.estimate.admin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.dev.IbioScience.enums.estimate.EstimateAnswerStatus;
import com.dev.IbioScience.enums.estimate.EstimateCheckStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminEstimateDetailResponse {

    private Long estimateId;
    private Long memberId;
    private String memberUserId;
    private String memberEmail;
    private String memberContactPhone;
    private String memberName;

    private String title;
    private String detailContent;

    private LocalDateTime requestedAt;
    private LocalDateTime checkedAt;
    private LocalDateTime answeredAt;

    private EstimateCheckStatus checkStatus;
    private EstimateAnswerStatus answerStatus;

    private List<ItemDto> items = new ArrayList<>();
    private List<AttachmentDto> attachments = new ArrayList<>();

    @Getter
    @Setter
    public static class ItemDto {
        private Long itemId;
        private Integer quantity;
        private String largeCategoryName;
        private String mediumCategoryName;
        private String smallCategoryName;
        private String brandName;
        private String productName;
        private String productCode;
    }

    @Getter
    @Setter
    public static class AttachmentDto {
        private Long attachmentId;
        private String originalFileName;
        private String fileUrl;
        private String contentType;
        private Long fileSize;
    }
}