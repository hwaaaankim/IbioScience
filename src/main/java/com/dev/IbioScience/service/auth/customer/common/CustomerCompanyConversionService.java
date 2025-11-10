package com.dev.IbioScience.service.auth.customer.common;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.customer.auth.ConversionToCompanyRequest;
import com.dev.IbioScience.enums.auth.CustomerType;
import com.dev.IbioScience.enums.auth.MemberDomain;
import com.dev.IbioScience.model.auth.CompanyProfile;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.embedded.Address;
import com.dev.IbioScience.repository.auth.CompanyProfileRepository;
import com.dev.IbioScience.repository.auth.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerCompanyConversionService {

	private final MemberRepository memberRepository;
	private final CompanyProfileRepository companyProfileRepository;

	@Value("${spring.upload.path}")
	private String uploadPath; // 예: /var/www/upload

	@Transactional
	public void convertToCompany(Long memberId, ConversionToCompanyRequest form, MultipartFile[] bizRegFiles)
			throws Exception {

		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

		// 파일 최소 1개 필수
		if (bizRegFiles == null || bizRegFiles.length == 0 || bizRegFiles[0].isEmpty()) {
			throw new IllegalArgumentException("사업자등록증 파일을 최소 1개 첨부해 주세요.");
		}

		// === CompanyProfile 생성 ===
		CompanyProfile profile = new CompanyProfile();
		profile.setCompanyName(form.getCompanyName());
		profile.setDepartment(form.getDepartment());
		profile.setCeoName(form.getCeoName());
		profile.setBusinessType(form.getBusinessType());
		profile.setBusinessItem(form.getBusinessItem());
		profile.setInvoiceEmail(form.getInvoiceEmail());
		profile.setBusinessRegistrationNumber(form.getBusinessRegistrationNumber());
		profile.setRepresentativeTel(nullSafe(form.getRepresentativeTel()));
		profile.setFax(nullSafe(form.getFax()));
		profile.setOrganizationCategory(form.getOrganizationCategory());

		// 주소
		Address addr = new Address();
		addr.setPostcode(form.getCPostcode());
		addr.setRoadAddress(form.getCRoadAddress());
		addr.setDetailAddress(nullSafe(form.getCDetailAddress()));
		profile.setCompanyAddress(addr);

		// === 파일 저장(첫 번째 파일 사용) ===
		MultipartFile first = bizRegFiles[0];

		// 경로: /{uploadPath}/commonPath/{memberId}/
		Path dir = Path.of(uploadPath, "commonPath", String.valueOf(memberId));
		Files.createDirectories(dir);

		String ext = getExt(first.getOriginalFilename());
		String stored = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
		Path saved = dir.resolve(stored);

		Files.copy(first.getInputStream(), saved, StandardCopyOption.REPLACE_EXISTING);

		// DB 경로(실제 파일 경로) + 접근 URL (/upload/** → uploadPath 매핑 가정)
		String fsPath = saved.toAbsolutePath().toString();
		String road = "/upload/commonPath/" + memberId + "/" + stored;

		profile.setBusinessRegImagePath(fsPath);
		profile.setBusinessRegImageRoad(road);

		// 저장
		companyProfileRepository.save(profile);

		// === 멤버 갱신 ===
		member.setCompanyProfile(profile);
		member.setCustomerType(CustomerType.BUSINESS);
		if (member.getDomain() == null)
			member.setDomain(MemberDomain.CUSTOMER);

		memberRepository.save(member);
	}

	private static String nullSafe(String v) {
		return v == null ? "" : v;
	}

	private static String getExt(String name) {
		if (name == null)
			return "";
		int idx = name.lastIndexOf('.');
		return (idx >= 0) ? name.substring(idx) : "";
	}
}