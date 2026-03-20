package com.dev.IbioScience.dto.admin.benefit;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PointHistoryRowResponse {

    private String changeType;   // PLUS / MINUS
    private Long amount;
    private String orderNo;
    private String sourceText;
    private LocalDateTime occurredAt;
}