package com.dev.IbioScience.dto.customer.auth.crm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.dev.IbioScience.enums.auth.CustomerType;
import com.dev.IbioScience.enums.auth.DealerGrade;
import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.enums.auth.MemberDomain;
import com.dev.IbioScience.enums.auth.MemberRole;
import com.dev.IbioScience.enums.auth.MemberStatus;
import com.dev.IbioScience.enums.product.OrganizationCategory;
import com.dev.IbioScience.enums.product.SettlementBasis;
import com.dev.IbioScience.enums.product.SettlementCycle;
import com.dev.IbioScience.enums.product.SupplyStructure;
import com.dev.IbioScience.enums.product.SupplyType;
import com.dev.IbioScience.enums.product.TradingStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDetailHomeDto {

    private Long memberId;
    private String activeTab; // "home"

    private MemberSection member;
    private CompanySection company; // nullable
    private BuyerSection buyer;     // nullable
    private SellerSection seller;   // nullable

    private List<MemoItem> latestMemos;

    // ===== Enum Options (select 구성용) =====
    private List<String> dealerGrades;
    private List<String> tradingStatuses;
    private List<String> supplyTypes;
    private List<String> supplyStructures;
    private List<String> settlementCycles;
    private List<String> settlementBases;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MemberSection {
        private Long id;
        private String username;
        private String name;
        private String tel;
        private String mobile;
        private String email;
        private Long point;

        private MemberDomain domain;
        private CustomerType customerType;
        private DealerType dealerType;
        private MemberRole role;
        private MemberStatus status;

        private String organizationName;

        private LocalDateTime joinedAt;
        private LocalDateTime withdrewAt;

        private boolean mustChangePassword;
        private LocalDateTime lastPasswordChangedAt;

        private String position;
        private boolean useYn;
        private boolean isPrimary;

        // 주소(회원)
        private String postcode;
        private String roadAddress;
        private String jibunAddress;
        private String detailAddress;

        // 기업연결 여부
        private Long companyProfileId; // nullable
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CompanySection {
        private Long id;
        private String companyName;
        private String department;
        private String ceoName;
        private String businessType;
        private String businessItem;
        private String representativeTel;
        private String fax;
        private String invoiceEmail;
        private String businessRegistrationNumber;
        private String businessRegImageRoad;
        private String homepageUrl;

        private OrganizationCategory organizationCategory;

        // 주소(회사)
        private String postcode;
        private String roadAddress;
        private String jibunAddress;
        private String detailAddress;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BuyerSection {
        private Long id;
        private DealerGrade grade;
        private BigDecimal customDiscountRate;
        private LocalDate effectiveFrom;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SellerSection {
        private Long id;

        // SellerDealerProfile
        private Long companyProfileId;
        private String shopName;
        private String logoImageRoad;
        private String supplierCode;

        private TradingStatus tradingStatus;
        private SupplyType supplyType;
        private SupplyStructure supplyStructure;

        private String productTypeText;
        private String tel;
        private String fax;
        private String homepageUrl;

        private LocalDate dealStartDate;
        private LocalDate dealStopDate;

        // 주소(사업장)
        private String bizPostcode;
        private String bizRoadAddress;
        private String bizJibunAddress;
        private String bizDetailAddress;

        // 주소(반품)
        private String returnPostcode;
        private String returnRoadAddress;
        private String returnJibunAddress;
        private String returnDetailAddress;

        // contacts
        private List<SellerContactItem> contacts;

        // settlement
        private SettlementPolicy settlementPolicy;

        // category permissions (ID만 내려주고, 이름은 JS에서 API로 매핑)
        private List<CategoryPermissionItem> categoryPermissions;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SellerContactItem {
        private Long id;
        private String name;
        private String phone;
        private String email;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SettlementPolicy {
        private Long id;
        private BigDecimal commissionRate;
        private SettlementCycle cycle;
        private SettlementBasis basis;
        private LocalDate nextSettlementDate;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CategoryPermissionItem {
        private Long id;
        private Long largeId;
        private Long mediumId; // nullable
        private Long smallId;  // nullable
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MemoItem {
        private Long id;
        private String content;
        private Long writerMemberId;
        private String writerName;
        private LocalDateTime createdAt;
    }
}