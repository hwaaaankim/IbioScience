package com.dev.IbioScience.dto.estimate.admin;

import java.time.LocalDateTime;

import com.dev.IbioScience.enums.estimate.EstimateAnswerStatus;
import com.dev.IbioScience.enums.estimate.EstimateCheckStatus;
import com.dev.IbioScience.enums.estimate.admin.AdminEstimateProgressFilter;

import lombok.Getter;

@Getter
public class AdminEstimateListRowDto {

    private final Long estimateId;
    private final Long memberId;
    private final String memberUserId;
    private final String memberEmail;
    private final String memberContactPhone;
    private final String memberName;
    private final long itemCount;
    private final EstimateCheckStatus checkStatus;
    private final EstimateAnswerStatus answerStatus;
    private final LocalDateTime requestedAt;
    private final LocalDateTime checkedAt;
    private final LocalDateTime answeredAt;
    private final String title;

    public AdminEstimateListRowDto(
            Long estimateId,
            Long memberId,
            String memberUserId,
            String memberEmail,
            String memberContactPhone,
            String memberName,
            Long itemCount,
            EstimateCheckStatus checkStatus,
            EstimateAnswerStatus answerStatus,
            LocalDateTime requestedAt,
            LocalDateTime checkedAt,
            LocalDateTime answeredAt,
            String title
    ) {
        this.estimateId = estimateId;
        this.memberId = memberId;
        this.memberUserId = memberUserId;
        this.memberEmail = memberEmail;
        this.memberContactPhone = memberContactPhone;
        this.memberName = memberName;
        this.itemCount = itemCount == null ? 0L : itemCount;
        this.checkStatus = checkStatus;
        this.answerStatus = answerStatus;
        this.requestedAt = requestedAt;
        this.checkedAt = checkedAt;
        this.answeredAt = answeredAt;
        this.title = title;
    }

    public String getProgressCode() {
        if (EstimateAnswerStatus.ANSWERED.equals(answerStatus)) {
            return AdminEstimateProgressFilter.ANSWERED.name();
        }

        if (EstimateCheckStatus.CHECKED.equals(checkStatus)) {
            return AdminEstimateProgressFilter.CHECKED.name();
        }

        return AdminEstimateProgressFilter.UNCHECKED.name();
    }

    public String getProgressLabel() {
        if (EstimateAnswerStatus.ANSWERED.equals(answerStatus)) {
            return AdminEstimateProgressFilter.ANSWERED.getLabel();
        }

        if (EstimateCheckStatus.CHECKED.equals(checkStatus)) {
            return AdminEstimateProgressFilter.CHECKED.getLabel();
        }

        return AdminEstimateProgressFilter.UNCHECKED.getLabel();
    }
}