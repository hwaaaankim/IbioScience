package com.dev.IbioScience.dto.customer.auth;

import java.time.LocalDateTime;

import com.dev.IbioScience.enums.auth.CustomerType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ClientApplyDetailDto {

    // ===== Member =====
    private Long memberId;
    private String username;
    private String name;
    private String mobile;
    private String tel;
    private String email;
    private String organizationName;

    private String aPostcode;
    private String aRoadAddress;
    private String aDetailAddress;

    private LocalDateTime joinedAt;
    private CustomerType customerType; // PERSONAL / BUSINESS

    // ===== CompanyProfile (BUSINESS only) =====
    private String companyName;
    private String department;
    private String ceoName;
    private String businessType;
    private String businessItem;
    private String representativeTel;
    private String fax;
    private String invoiceEmail;
    private String businessRegistrationNumber;

    private String cPostcode;
    private String cRoadAddress;
    private String cDetailAddress;

    private String businessRegImageRoad; // 이미지 URL

    public ClientApplyDetailDto(
            Long memberId,
            String username,
            String name,
            String mobile,
            String tel,
            String email,
            String organizationName,
            String aPostcode,
            String aRoadAddress,
            String aDetailAddress,
            LocalDateTime joinedAt,
            CustomerType customerType,

            String companyName,
            String department,
            String ceoName,
            String businessType,
            String businessItem,
            String representativeTel,
            String fax,
            String invoiceEmail,
            String businessRegistrationNumber,
            String cPostcode,
            String cRoadAddress,
            String cDetailAddress,
            String businessRegImageRoad
    ) {
        this.memberId = memberId;
        this.username = username;
        this.name = name;
        this.mobile = mobile;
        this.tel = tel;
        this.email = email;
        this.organizationName = organizationName;
        this.aPostcode = aPostcode;
        this.aRoadAddress = aRoadAddress;
        this.aDetailAddress = aDetailAddress;
        this.joinedAt = joinedAt;
        this.customerType = customerType;

        this.companyName = companyName;
        this.department = department;
        this.ceoName = ceoName;
        this.businessType = businessType;
        this.businessItem = businessItem;
        this.representativeTel = representativeTel;
        this.fax = fax;
        this.invoiceEmail = invoiceEmail;
        this.businessRegistrationNumber = businessRegistrationNumber;
        this.cPostcode = cPostcode;
        this.cRoadAddress = cRoadAddress;
        this.cDetailAddress = cDetailAddress;
        this.businessRegImageRoad = businessRegImageRoad;
    }

    public String getContact() {
        if (mobile != null && !mobile.trim().isEmpty()) return mobile;
        if (tel != null && !tel.trim().isEmpty()) return tel;
        return "-";
    }

    public boolean isBusiness() {
        return customerType == CustomerType.BUSINESS;
    }
}