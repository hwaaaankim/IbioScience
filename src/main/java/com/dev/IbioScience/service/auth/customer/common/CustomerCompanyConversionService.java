package com.dev.IbioScience.service.auth.customer.common;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.customer.auth.ConversionToCompanyRequest;
import com.dev.IbioScience.enums.auth.CompanyConversionStatus;
import com.dev.IbioScience.enums.auth.CustomerType;
import com.dev.IbioScience.enums.auth.MemberStatus;
import com.dev.IbioScience.enums.logging.MemberAuditAction;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.embedded.Address;
import com.dev.IbioScience.model.auth.utils.CompanyConversionApplication;
import com.dev.IbioScience.repository.auth.MemberRepository;
import com.dev.IbioScience.repository.auth.utils.CompanyConversionApplicationRepository;
import com.dev.IbioScience.service.logging.MemberAuditLogService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerCompanyConversionService {

	private final MemberRepository memberRepository;
	private final CompanyConversionApplicationRepository companyConversionApplicationRepository;

	private final MemberAuditLogService memberAuditLogService;

	@Value("${spring.upload.path}")
	private String uploadPath;

	@Transactional
	public void convertToCompany(Long memberId, ConversionToCompanyRequest form, MultipartFile[] bizRegFiles)
			throws Exception {

		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

		// 전환 신청은 “기존 계정이 로그인 가능” 상태여야 의미가 있습니다.
		if (member.getStatus() != MemberStatus.ACTIVE) {
			throw new IllegalArgumentException("승인 완료된 회원만 전환 신청이 가능합니다.");
		}

		// 이미 기업이거나 회사프로필이 있으면 신청 불가
		if (member.getCompanyProfile() != null || member.getCustomerType() == CustomerType.BUSINESS) {
			throw new IllegalArgumentException("이미 기업회원입니다.");
		}

		// 중복 신청 방지
		if (companyConversionApplicationRepository.existsByApplicant_IdAndStatus(memberId, CompanyConversionStatus.PENDING)) {
			throw new IllegalArgumentException("이미 기업 전환 신청이 대기 중입니다.");
		}

		// 파일 최소 1개 필수
		if (bizRegFiles == null || bizRegFiles.length == 0 || bizRegFiles[0].isEmpty()) {
			throw new IllegalArgumentException("사업자등록증 파일을 최소 1개 첨부해 주세요.");
		}

		// === 파일 저장(첫 번째 파일 사용) ===
		MultipartFile first = bizRegFiles[0];

		Path dir = Path.of(uploadPath, "commonPath", String.valueOf(memberId));
		Files.createDirectories(dir);

		String ext = getExt(first.getOriginalFilename());
		String stored = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
		Path saved = dir.resolve(stored);

		Files.copy(first.getInputStream(), saved, StandardCopyOption.REPLACE_EXISTING);

		String fsPath = saved.toAbsolutePath().toString();
		String road = "/upload/commonPath/" + memberId + "/" + stored;

		// 주소
		Address cAddr = new Address();
		cAddr.setPostcode(form.getCPostcode());
		cAddr.setRoadAddress(form.getCRoadAddress());
		cAddr.setJibunAddress(null); // form에 없으면 null
		cAddr.setDetailAddress(nullSafe(form.getCDetailAddress()));

		CompanyConversionApplication app = CompanyConversionApplication.builder()
			.applicant(member)
			.status(CompanyConversionStatus.PENDING)
			.requestedAt(LocalDateTime.now())
			.processedAt(null)
			.expiredAt(null)

			.companyName(form.getCompanyName())
			.department(form.getDepartment())
			.ceoName(form.getCeoName())
			.businessType(form.getBusinessType())
			.businessItem(form.getBusinessItem())
			.invoiceEmail(form.getInvoiceEmail())
			.businessRegistrationNumber(form.getBusinessRegistrationNumber())
			.representativeTel(nullSafe(form.getRepresentativeTel()))
			.fax(nullSafe(form.getFax()))
			.organizationCategory(form.getOrganizationCategory())
			.companyAddress(cAddr)
			.businessRegImagePath(fsPath)
			.businessRegImageRoad(road)

			.note(null)
			.processNote(null)
			.approvedCompanyProfile(null)
			.homepageUrl(null)
			.build();

		companyConversionApplicationRepository.save(app);

		// ✅ 감사로그: “전환신청”만 기록(회원 필드 변경 없음)
		memberAuditLogService.logEvent(
			member,
			MemberAuditAction.PERSONAL_TO_COMPANY_CONVERSION,
			"COMPANY_CONVERSION_REQUESTED appId=" + app.getId(),
			memberId
		);
	}

	private static String nullSafe(String v) {
		return v == null ? "" : v;
	}

	private static String getExt(String name) {
		if (name == null) return "";
		int idx = name.lastIndexOf('.');
		return (idx >= 0) ? name.substring(idx) : "";
	}
}