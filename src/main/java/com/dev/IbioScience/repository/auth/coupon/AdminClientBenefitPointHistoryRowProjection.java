package com.dev.IbioScience.repository.auth.coupon;

import java.time.LocalDateTime;

public interface AdminClientBenefitPointHistoryRowProjection {

    String getChangeType();

    Long getAmount();

    String getOrderNo();

    String getSourceText();

    LocalDateTime getOccurredAt();
}