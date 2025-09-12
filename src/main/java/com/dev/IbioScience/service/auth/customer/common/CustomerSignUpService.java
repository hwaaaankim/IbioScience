package com.dev.IbioScience.service.auth.customer.common;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.customer.auth.CompanySignUpRequest;
import com.dev.IbioScience.dto.customer.auth.PersonalSignUpRequest;
import com.dev.IbioScience.model.auth.CompanyProfile;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.MemberAuditLog;
import com.dev.IbioScience.model.auth.embedded.Address;
import com.dev.IbioScience.model.auth.enums.CustomerType;
import com.dev.IbioScience.model.auth.enums.DealerType;
import com.dev.IbioScience.model.auth.enums.MemberDomain;
import com.dev.IbioScience.model.auth.enums.MemberRole;
import com.dev.IbioScience.model.auth.enums.MemberStatus;
import com.dev.IbioScience.model.auth.enums.OrganizationCategory;
import com.dev.IbioScience.repository.auth.CompanyProfileRepository;
import com.dev.IbioScience.repository.auth.MemberAuditLogRepository;
import com.dev.IbioScience.repository.auth.MemberRepository;
import com.dev.IbioScience.utils.UploadPathHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerSignUpService {

	private final MemberAuditLogRepository auditLogRepository;
	private final CompanyProfileRepository companyProfileRepository;
	private final MemberAuditLogRepository memberAuditLogRepository;
	private final PasswordEncoder passwordEncoder;
	private final MemberRepository memberRepository;
	private final UploadPathHelper uploadPathHelper;

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

	    // ✅ 소비자 도메인 + 개인 소비자 유형
	    m.setDomain(MemberDomain.CUSTOMER);
	    m.setCustomerType(CustomerType.PERSONAL);

	    m.setDealerType(DealerType.NONE);
	    m.setRole(MemberRole.USER);
	    m.setStatus(MemberStatus.ACTIVE);

	    m.setJoinedAt(LocalDateTime.now());
	    m.setMustChangePassword(false);
	    m.setLastPasswordChangedAt(LocalDateTime.now());

	    m.setOrganizationName(dto.getOrganizationName());
	    m.setUseYn(true);
	    m.setPrimary(false);

	    Member saved = memberRepository.save(m);

	    // 감사로그
	    MemberAuditLog log = MemberAuditLog.builder()
	            .member(saved)
	            .action("JOIN")
	            .fieldName(null)
	            .oldValue(null)
	            .newValue("PERSONAL_SIGNUP")
	            .actorMemberId(null)
	            .build();
	    auditLogRepository.save(log);

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

	    // 1) CompanyProfile
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

	    // 2) Member
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
	            // ✅ 소비자 도메인 + 기업(사업자) 소비자 유형
	            .domain(MemberDomain.CUSTOMER)
	            .customerType(CustomerType.BUSINESS)
	            .dealerType(DealerType.NONE)
	            .role(MemberRole.USER)
	            .status(MemberStatus.PENDING) // 검수 후 ACTIVE
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

	    // 3) 사업자등록증 파일 저장
	    if (dto.getBizRegFile() != null && !dto.getBizRegFile().isEmpty()) {
	        java.nio.file.Path saved = uploadPathHelper.saveBizRegFileForCustomer(member.getId(), dto.getBizRegFile());
	        company.setBusinessRegImagePath(saved.toString());
	        company.setBusinessRegImageRoad(uploadPathHelper.publicUrlOf(saved));
	        companyProfileRepository.save(company);
	    }

	    // 4) 감사로그
	    memberAuditLogRepository.save(
	            MemberAuditLog.builder()
	                    .member(member)
	                    .action("JOIN")
	                    .newValue("COMPANY_SIGN_UP")
	                    .build()
	    );

	    return member;
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