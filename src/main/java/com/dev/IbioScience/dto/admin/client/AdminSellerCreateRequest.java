package com.dev.IbioScience.dto.admin.client;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.enums.auth.DealerGrade;
import com.dev.IbioScience.enums.auth.MemberStatus;
import com.dev.IbioScience.enums.product.OrganizationCategory;
import com.dev.IbioScience.enums.product.SettlementBasis;
import com.dev.IbioScience.enums.product.SettlementCycle;
import com.dev.IbioScience.enums.product.SupplyStructure;
import com.dev.IbioScience.enums.product.SupplyType;
import com.dev.IbioScience.enums.product.TradingStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminSellerCreateRequest {

	/* =========================
	 * 1. Member
	 * ========================= */
	private String username;
	private String password;
	private String passwordConfirm;

	private String name;
	private String tel;
	private String mobile;
	private String email;
	private Long point;

	private String organizationName;
	private String position;

	private String mPostcode;
	private String mRoadAddress;
	private String mJibunAddress;
	private String mDetailAddress;

	private MemberStatus status;
	private boolean useYn;
	private boolean primary;
	private boolean mustChangePassword;

	/* =========================
	 * 2. CompanyProfile
	 * ========================= */
	private String companyName;
	private String department;
	private String ceoName;
	private String businessType;
	private String businessItem;
	private String representativeTel;
	private String fax;
	private String invoiceEmail;
	private String businessRegistrationNumber;
	private String companyHomepageUrl;

	private String cPostcode;
	private String cRoadAddress;
	private String cJibunAddress;
	private String cDetailAddress;

	private OrganizationCategory organizationCategory;

	private MultipartFile businessRegFile;

	/* =========================
	 * 3. BuyerDealerProfile
	 * ========================= */
	private DealerGrade buyerGrade;
	private BigDecimal buyerCustomDiscountRate;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate buyerEffectiveFrom;

	/* =========================
	 * 4. SellerDealerProfile
	 * ========================= */
	private String shopName;
	private String supplierCode;
	private TradingStatus tradingStatus;
	private SupplyType supplyType;
	private SupplyStructure supplyStructure;
	private String productTypeText;
	private String sellerTel;
	private String sellerFax;
	private String sellerHomepageUrl;

	private String bPostcode;
	private String bRoadAddress;
	private String bJibunAddress;
	private String bDetailAddress;

	private String rPostcode;
	private String rRoadAddress;
	private String rJibunAddress;
	private String rDetailAddress;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate dealStartDate;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate dealStopDate;

	private MultipartFile sellerLogoFile;

	/* =========================
	 * 4-1. DealerSettlementPolicy
	 * ========================= */
	private BigDecimal settlementCommissionRate;
	private SettlementCycle settlementCycle;
	private SettlementBasis settlementBasis;

	/* =========================
	 * 5. DealerCategoryPermission
	 * ========================= */
	private String categoryPermissionsJson;
}