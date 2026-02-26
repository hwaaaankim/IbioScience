package com.dev.IbioScience.service.auth.customer.common;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.customer.auth.CompanySignUpRequest;
import com.dev.IbioScience.dto.customer.auth.PersonalSignUpRequest;
import com.dev.IbioScience.enums.auth.CustomerType;
import com.dev.IbioScience.enums.auth.DealerGrade;
import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.enums.auth.MemberDomain;
import com.dev.IbioScience.enums.auth.MemberRole;
import com.dev.IbioScience.enums.auth.MemberStatus;
import com.dev.IbioScience.enums.logging.MemberAuditAction;
import com.dev.IbioScience.enums.product.OrganizationCategory;
import com.dev.IbioScience.model.auth.BuyerDealerProfile;
import com.dev.IbioScience.model.auth.CompanyProfile;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.embedded.Address;
import com.dev.IbioScience.repository.auth.BuyerDealerProfileRepository;
import com.dev.IbioScience.repository.auth.CompanyProfileRepository;
import com.dev.IbioScience.repository.auth.MemberRepository;
import com.dev.IbioScience.service.logging.MemberAuditLogService;
import com.dev.IbioScience.utils.UploadPathHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerSignUpService {

	private final CompanyProfileRepository companyProfileRepository;
	private final PasswordEncoder passwordEncoder;
	private final MemberRepository memberRepository;
	private final UploadPathHelper uploadPathHelper;

	private final BuyerDealerProfileRepository buyerDealerProfileRepository;
	private final MemberAuditLogService memberAuditLogService;

	@Transactional
	public Member registerPersonal(PersonalSignUpRequest dto) {

		if (memberRepository.existsByUsername(dto.getUsername())) {
			throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
		}

		Member m = new Member();
		m.setUsername(dto.getUsername());
		m.setPassword(passwordEncoder.encode(dto.getPassword()));
		m.setName(dto.getName());
		m.setTel(dto.getTel());
		m.setMobile(dto.getMobile());
		m.setEmail(dto.getEmail());
		m.setAddress(dto.getAddress());

		m.setDomain(MemberDomain.CUSTOMER);
		m.setCustomerType(CustomerType.PERSONAL);

		m.setDealerType(DealerType.NONE);

		m.setRole(MemberRole.USER);

		// ✅ 변경: 승인 전까지 로그인 불가
		m.setStatus(MemberStatus.PENDING);

		m.setJoinedAt(LocalDateTime.now());
		m.setMustChangePassword(false);
		m.setLastPasswordChangedAt(LocalDateTime.now());

		m.setOrganizationName(dto.getOrganizationName());
		m.setUseYn(true);
		m.setPrimary(false);

		Member saved = memberRepository.save(m);

		memberAuditLogService.logEvent(
			saved,
			MemberAuditAction.PERSONAL_SIGNUP,
			"PERSONAL_SIGNUP_REQUESTED",
			null
		);

		return saved;
	}

	@Transactional
	public Member registerCompany(CompanySignUpRequest dto) {

		if (!(dto.isAgreeTerms() && dto.isAgreePrivacy())) {
			throw new IllegalArgumentException("필수 약관에 동의해 주세요.");
		}

		if (memberRepository.existsByUsername(dto.getUsername().trim())) {
			throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
		}

		CompanyProfile company = CompanyProfile.builder()
			.companyName(dto.getCompanyName().trim())
			.department(nvl(dto.getDepartment()))
			.ceoName(dto.getCeoName().trim())
			.businessType(dto.getBusinessType().trim())
			.businessItem(dto.getBusinessItem().trim())
			.representativeTel(nvl(dto.getRepresentativeTel()))
			.fax(nvl(dto.getFax()))
			.invoiceEmail(dto.getInvoiceEmail().trim())
			.businessRegistrationNumber(dto.getBusinessRegistrationNumber().trim())
			.companyAddress(Address.builder()
				.postcode(dto.getCPostcode())
				.roadAddress(dto.getCRoadAddress())
				.detailAddress(nvl(dto.getCDetailAddress()))
				.build())
			.organizationCategory(mapOrg(dto.getOrganizationCategory()))
			.build();

		company = companyProfileRepository.save(company);

		Member member = Member.builder()
			.username(dto.getUsername().trim())
			.password(passwordEncoder.encode(dto.getPassword()))
			.name(dto.getName().trim())
			.tel(nvl(dto.getTel()))
			.mobile(dto.getMobile().trim())
			.email(dto.getEmail().trim())
			.address(Address.builder()
				.postcode(dto.getAPostcode())
				.roadAddress(dto.getARoadAddress())
				.detailAddress(nvl(dto.getADetailAddress()))
				.build())
			.domain(MemberDomain.CUSTOMER)
			.customerType(CustomerType.BUSINESS)

			// 기업가입은 승인 전 로그인 불가라 BUYER여도 “유효 권한 노출” 문제 없음
			.dealerType(DealerType.BUYER)

			.role(MemberRole.USER)
			.status(MemberStatus.PENDING)
			.companyProfile(company)
			.organizationName(null)
			.joinedAt(LocalDateTime.now())
			.mustChangePassword(false)
			.lastPasswordChangedAt(LocalDateTime.now())
			.position(null)
			.useYn(true)
			.isPrimary(false)
			.build();

		member = memberRepository.save(member);

		// (기존 로직 유지) 기업회원 가입 시 BuyerDealerProfile 생성
		ensureBuyerDealerProfileExists(member);

		if (dto.getBizRegFile() != null && !dto.getBizRegFile().isEmpty()) {
			java.nio.file.Path saved = uploadPathHelper.saveBizRegFileForCustomer(member.getId(), dto.getBizRegFile());
			company.setBusinessRegImagePath(saved.toString());
			company.setBusinessRegImageRoad(uploadPathHelper.publicUrlOf(saved));
			companyProfileRepository.save(company);
		}

		memberAuditLogService.logEvent(
			member,
			MemberAuditAction.COMPANY_SIGNUP,
			"COMPANY_SIGN_UP_REQUESTED",
			null
		);

		return member;
	}

	private void ensureBuyerDealerProfileExists(Member member) {
		if (member == null || member.getId() == null) return;
		if (member.getCustomerType() != CustomerType.BUSINESS) return;
		if (buyerDealerProfileRepository.existsByMember_Id(member.getId())) return;

		BuyerDealerProfile profile = BuyerDealerProfile.builder()
			.member(member)
			.grade(DealerGrade.EXCEPTION)
			.customDiscountRate(null)
			.effectiveFrom(LocalDate.now())
			.build();

		buyerDealerProfileRepository.save(profile);
	}

	private String nvl(String s) {
		return s == null ? "" : s.trim();
	}

	private OrganizationCategory mapOrg(String raw) {
		try {
			return OrganizationCategory.valueOf(raw);
		} catch (Exception e) {
			return OrganizationCategory.GROUP_C;
		}
	}
}