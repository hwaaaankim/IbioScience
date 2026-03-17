package com.dev.IbioScience.dto.estimate.admin;

import org.springframework.util.StringUtils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EstimateMailSendRequest {

    private String subject;
    private String bodyHtml;

    public String getNormalizedSubject() {
        return StringUtils.hasText(subject) ? subject.trim() : null;
    }

    public String getNormalizedBodyHtml() {
        return StringUtils.hasText(bodyHtml) ? bodyHtml.trim() : null;
    }
}