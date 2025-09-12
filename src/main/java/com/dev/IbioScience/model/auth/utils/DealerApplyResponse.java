package com.dev.IbioScience.model.auth.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DealerApplyResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;

    // 필요 시 추가 정보
    @JsonProperty("applicationId")
    private Long applicationId;

    @JsonProperty("status")
    private String status;
}