package com.dev.IbioScience.dto.customer.auth;

import java.time.LocalDateTime;

import com.dev.IbioScience.enums.auth.CustomerType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ClientApplyRowDto {

    private Long memberId;
    private String username;

    private String companyName; // 개인이면 null
    private String name;

    private String mobile;
    private String tel;

    private LocalDateTime joinedAt;
    private CustomerType customerType; // PERSONAL / BUSINESS

    public ClientApplyRowDto(Long memberId, String username, String companyName, String name,
                            String mobile, String tel, LocalDateTime joinedAt, CustomerType customerType) {
        this.memberId = memberId;
        this.username = username;
        this.companyName = companyName;
        this.name = name;
        this.mobile = mobile;
        this.tel = tel;
        this.joinedAt = joinedAt;
        this.customerType = customerType;
    }

    public String getContact() {
        if (mobile != null && !mobile.trim().isEmpty()) return mobile;
        if (tel != null && !tel.trim().isEmpty()) return tel;
        return "-";
    }

    public String getCompanyNameOrDash() {
        if (companyName != null && !companyName.trim().isEmpty()) return companyName;
        return "- 없음-";
    }
}