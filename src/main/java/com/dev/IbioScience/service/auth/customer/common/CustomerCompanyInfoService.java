package com.dev.IbioScience.service.auth.customer.common;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.customer.auth.CompanyInfoUpdateRequest;
import com.dev.IbioScience.dto.customer.auth.CompanyInfoUpdateResponse;
import com.dev.IbioScience.model.auth.CompanyProfile;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.embedded.Address;
import com.dev.IbioScience.repository.auth.CompanyProfileRepository;
import com.dev.IbioScience.repository.auth.MemberRepository;
import com.dev.IbioScience.utils.UploadPathHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerCompanyInfoService {

	private final MemberRepository memberRepository;
	private final CompanyProfileRepository companyProfileRepository;
	private final UploadPathHelper uploadPathHelper;
	private final PasswordEncoder passwordEncoder;

	/* ========== 조회 ========== */
	@Transactional(readOnly = true)
	public CompanyInfoUpdateResponse loadCompanyInfoOrThrow(Long memberId) {
		Member m = memberRepository.findById(memberId)
				.orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

		CompanyProfile c = m.getCompanyProfile();
		if (c == null)
			throw new IllegalStateException("기업회원 정보가 없습니다.");

		// 원본 값
		String mobileRaw = nvl(m.getMobile());
		String email     = nvl(m.getEmail());

		String bizTelRaw = nvl(c.getRepresentativeTel());
		String faxRaw    = nvl(c.getFax());
		String bizNoRaw  = nvl(c.getBusinessRegistrationNumber());

		// 회사 주소 = 사업장 주소 (요청사항: 사업장 주소 필수, 지번 선택)
		Address w = c.getCompanyAddress();

		// 사업자등록증(단일) → 프리뷰용
		String url        = nvl(c.getBusinessRegImageRoad());
		String path       = nvl(c.getBusinessRegImagePath());
		String fileName   = extractFileName(path);
		String contentType= guessContentType(path);
		long   size       = fileSize(path);

		// ===== 분할(휴대폰/전화/팩스) =====
		String[] mobParts = splitKoreanPhone(mobileRaw); // [p1,p2,p3]
		String[] telParts = splitKoreanPhone(bizTelRaw);
		String[] faxParts = splitKoreanPhone(faxRaw);

		// ===== 분할(사업자등록번호 10자리 → 3-2-5) =====
		String[] bizNoParts = splitBizNo(bizNoRaw); // [3,2,5]

		return CompanyInfoUpdateResponse.builder()
				// 회원
				.memberId(m.getId())
				.username(nvl(m.getUsername()))
				.name(nvl(m.getName()))
				.mobile1(mobParts[0])
				.mobile2(mobParts[1])
				.mobile3(mobParts[2])
				.email(email)

				// 기본주소
				.zip(m.getAddress() != null ? nvl(m.getAddress().getPostcode())      : "")
				.road(m.getAddress() != null ? nvl(m.getAddress().getRoadAddress())   : "")
				.jibun(m.getAddress() != null ? nvl(m.getAddress().getJibunAddress()) : "")
				.detail(m.getAddress() != null ? nvl(m.getAddress().getDetailAddress()): "")

				// 회사
				.companyId(c.getId())
				.companyName(nvl(c.getCompanyName()))
				.ceoName(nvl(c.getCeoName()))
				.department(nvl(c.getDepartment()))
				.companyEmail(nvl(c.getInvoiceEmail()))
				.bizType(nvl(c.getBusinessType()))
				.bizItem(nvl(c.getBusinessItem()))

				.bizTel1(telParts[0])
				.bizTel2(telParts[1])
				.bizTel3(telParts[2])

				.fax1(faxParts[0])
				.fax2(faxParts[1])
				.fax3(faxParts[2])

				.bizNo1(bizNoParts[0])
				.bizNo2(bizNoParts[1])
				.bizNo3(bizNoParts[2])

				// 사업장 주소
				.workplaceZip(   w != null ? nvl(w.getPostcode())     : "")
				.workplaceRoad(  w != null ? nvl(w.getRoadAddress())  : "")
				.workplaceJibun( w != null ? nvl(w.getJibunAddress()) : "")
				.workplaceDetail(w != null ? nvl(w.getDetailAddress()): "")

				// 사업자등록증
				.bizRegOriginalName(fileName)
				.bizRegPublicUrl(url)
				.bizRegContentType(contentType)
				.bizRegSize(size)
				.build();
	}

	/* ========== 수정 ========== */
	@Transactional
	public void updateCompanyInfo(Long pathMemberId, CompanyInfoUpdateRequest req, MultipartFile newBizRegFile) {
		// 0) 식별/권한 방어
		if (!Objects.equals(pathMemberId, req.getMemberId())) {
			throw new IllegalArgumentException("잘못된 요청입니다. (식별자 불일치)");
		}

		Member m = memberRepository.findById(pathMemberId)
				.orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
		CompanyProfile c = m.getCompanyProfile();
		if (c == null)
			throw new IllegalStateException("기업회원 정보가 없습니다.");

		// 1) 아이디 변경 + 중복검사
		String newUsername = req.getUsername().trim();
		boolean usernameChanged = !newUsername.equalsIgnoreCase(m.getUsername());
		if (usernameChanged) {
			if (!req.isUsernameDupChecked()) {
				throw new IllegalArgumentException("아이디 중복확인을 완료해 주세요.");
			}
			if (memberRepository.existsByUsernameAndIdNot(newUsername, m.getId())) {
				throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
			}
			m.setUsername(newUsername);
		}

		// 2) 비밀번호 변경 규칙
		String p1 = nvl(req.getPassword());
		String p2 = nvl(req.getPasswordCheck());
		if (!p1.isEmpty() || !p2.isEmpty()) {
			if (p1.isEmpty() || p2.isEmpty()) {
				throw new IllegalArgumentException("비밀번호와 비밀번호 확인을 모두 입력해 주세요.");
			}
			if (!p1.equals(p2)) {
				throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
			}
			m.setPassword(passwordEncoder.encode(p1));
			m.setLastPasswordChangedAt(LocalDateTime.now());
		}

		// 3) 기본 주소/연락처/이메일 등 필수 검증 & 반영 (지번 선택)
		m.setName(req.getName().trim());
		m.setEmail(req.getEmail().trim());

		String mobile = joinDash(req.getMobile1(), req.getMobile2(), req.getMobile3());
		m.setMobile(mobile);

		Address memberAddr = Address.builder()
				.postcode(req.getZip())
				.roadAddress(req.getRoad())
				.jibunAddress(nvl(req.getJibun()))
				.detailAddress(req.getDetail())
				.build();
		m.setAddress(memberAddr);

		// 4) 회사 정보 반영
		c.setCompanyName(req.getCompanyName().trim());
		c.setCeoName(req.getCeoName().trim());
		c.setDepartment(nvl(req.getDepartment()));
		c.setInvoiceEmail(nvl(req.getCompanyEmail()));
		c.setBusinessType(nvl(req.getBizType()));
		c.setBusinessItem(nvl(req.getBizItem()));

		String tel = joinDash(req.getBizTel1(), req.getBizTel2(), req.getBizTel3());
		c.setRepresentativeTel(nvl(tel));

		String fax = joinDash(req.getFax1(), req.getFax2(), req.getFax3());
		c.setFax(nvl(fax));

		// 사업자등록번호(가중치 검증)
		String bizNoDigits = (nvl(req.getBizNo1()) + nvl(req.getBizNo2()) + nvl(req.getBizNo3())).replaceAll("\\D", "");
		if (bizNoDigits.length() != 10 || !isValidKRBizNumber(bizNoDigits)) {
			throw new IllegalArgumentException("사업자등록번호가 올바르지 않습니다.");
		}
		// DB에는 하이픈 제거 상태로 저장(요구에 맞춰 조정 가능)
		c.setBusinessRegistrationNumber(formatBizNo(bizNoDigits).replaceAll("-", ""));

		// 5) 사업장 주소(지번 선택)
		Address workplace = Address.builder()
				.postcode(req.getWorkplaceZip())
				.roadAddress(req.getWorkplaceRoad())
				.jibunAddress(nvl(req.getWorkplaceJibun()))
				.detailAddress(req.getWorkplaceDetail())
				.build();
		c.setCompanyAddress(workplace);

		// 6) 사업자등록증 파일 처리 (단일)
		boolean hasExisting = StringUtils.hasText(c.getBusinessRegImagePath());
		boolean deleteExisting = req.isDeleteExistingBizReg();

		if (deleteExisting && hasExisting && (newBizRegFile == null || newBizRegFile.isEmpty())) {
			// 삭제만 하고 신규 업로드가 없으면 규칙 위반(최소 1개)
			throw new IllegalArgumentException("사업자등록증은 최소 1개 이상 등록되어야 합니다.");
		}

		if (deleteExisting && hasExisting) {
			// 기존 파일 삭제
			safeDelete(c.getBusinessRegImagePath());
			c.setBusinessRegImagePath(null);
			c.setBusinessRegImageRoad(null);
		}

		if (newBizRegFile != null && !newBizRegFile.isEmpty()) {
			// 신규 업로드
			Path saved = uploadPathHelper.saveBizRegFileForCustomer(m.getId(), newBizRegFile);
			c.setBusinessRegImagePath(saved.toString());
			c.setBusinessRegImageRoad(uploadPathHelper.publicUrlOf(saved));
		}

		// 최종 보장: 최소 1개
		if (!StringUtils.hasText(c.getBusinessRegImagePath())) {
			throw new IllegalArgumentException("사업자등록증은 최소 1개 이상 등록되어야 합니다.");
		}

		// 저장
		companyProfileRepository.save(c);
		memberRepository.save(m);
	}

	/* ====== 유틸 ====== */

	private String nvl(String s) {
		return s == null ? "" : s.trim();
	}

	private String joinDash(String a, String b, String c) {
		a = nvl(a);
		b = nvl(b);
		c = nvl(c);
		return String.join("-", a, b, c)
				.replaceAll("^-+|-+$", "")
				.replaceAll("--+", "-");
	}

	/**
	 * 한국 전화번호 분할
	 * - 입력 내 비숫자 제거 후 [p1,p2,p3]
	 * - 휴대폰(010/011/016/017/018/019): 3-4-4
	 * - 02는 2-3/4-4, 기타 지역번호는 3-3/4-4
	 * - 길이 이슈가 있어도 가능한 범위에서 최대한 분할
	 */
	private String[] splitKoreanPhone(String raw) {
		String digits = nvl(raw).replaceAll("\\D+", "");
		String p1 = "", p2 = "", p3 = "";

		if (digits.isEmpty()) {
			return new String[] { p1, p2, p3 };
		}

		// 휴대폰
		if (digits.startsWith("010") || digits.startsWith("011")
				|| digits.startsWith("016") || digits.startsWith("017")
				|| digits.startsWith("018") || digits.startsWith("019")) {
			if (digits.length() >= 3) p1 = digits.substring(0, 3);
			if (digits.length() >= 7) p2 = digits.substring(3, 7);
			else if (digits.length() > 3) p2 = digits.substring(3);
			if (digits.length() >= 11) p3 = digits.substring(7, Math.min(11, digits.length()));
			else if (digits.length() > 7) p3 = digits.substring(7);
			return new String[] { nvl(p1), nvl(p2), nvl(p3) };
		}

		// 서울 02
		if (digits.startsWith("02")) {
			if (digits.length() >= 2) p1 = digits.substring(0, 2);
			if (digits.length() >= 6) {
				int remain = digits.length() - 2;
				if (remain == 7) {      // 2-3-4
					p2 = digits.substring(2, 5);
					p3 = digits.substring(5);
				} else if (remain == 8) { // 2-4-4
					p2 = digits.substring(2, 6);
					p3 = digits.substring(6);
				} else if (remain < 7) {
					int mid = Math.min(5, digits.length());
					p2 = digits.substring(2, mid);
					if (digits.length() > mid) p3 = digits.substring(mid);
				} else {
					p2 = digits.substring(2, digits.length() - 4);
					p3 = digits.substring(digits.length() - 4);
				}
			} else if (digits.length() > 2) {
				int mid = Math.min(digits.length(), 6);
				p2 = digits.substring(2, mid);
				if (digits.length() > mid) p3 = digits.substring(mid);
			}
			return new String[] { nvl(p1), nvl(p2), nvl(p3) };
		}

		// 기타 지역번호(보통 3자리)
		if (digits.length() >= 3) {
			p1 = digits.substring(0, 3);
			if (digits.length() - 3 <= 4) {
				p2 = digits.substring(3);
			} else {
				p3 = digits.substring(digits.length() - 4);
				p2 = digits.substring(3, digits.length() - 4);
			}
		} else {
			p1 = digits; // 매우 짧은 경우
		}

		return new String[] { nvl(p1), nvl(p2), nvl(p3) };
	}

	/**
	 * 사업자등록번호 분할 (10자리 → 3-2-5)
	 * - 비숫자 제거 후 가능한 범위에서 최대한 분할
	 */
	private String[] splitBizNo(String raw) {
		String digits = nvl(raw).replaceAll("\\D+", "");
		String a = "", b = "", c = "";
		if (digits.isEmpty()) return new String[] { a, b, c };

		if (digits.length() >= 3) a = digits.substring(0, 3);
		else return new String[] { digits, b, c };

		if (digits.length() >= 5) b = digits.substring(3, 5);
		else if (digits.length() > 3) return new String[] { a, digits.substring(3), c };

		if (digits.length() > 5) c = digits.substring(5, Math.min(digits.length(), 10));

		return new String[] { nvl(a), nvl(b), nvl(c) };
	}

	private String formatBizNo(String digits10) {
		String d = digits10.replaceAll("\\D", "");
		if (d.length() != 10)
			return digits10;
		return d.substring(0, 3) + "-" + d.substring(3, 5) + "-" + d.substring(5);
	}

	private String extractFileName(String path) {
		if (!StringUtils.hasText(path))
			return null;
		return new File(path).getName();
	}

	private String guessContentType(String path) {
		try {
			if (!StringUtils.hasText(path))
				return null;
			return Files.probeContentType(Path.of(path));
		} catch (Exception e) {
			return null;
		}
	}

	private long fileSize(String path) {
		try {
			if (!StringUtils.hasText(path))
				return 0L;
			return Files.size(Path.of(path));
		} catch (Exception e) {
			return 0L;
		}
	}

	private void safeDelete(String path) {
		try {
			if (!StringUtils.hasText(path))
				return;
			Files.deleteIfExists(Path.of(path));
		} catch (Exception ignore) {
		}
	}

	/**
	 * 국세청 가중치 검증
	 */
	private boolean isValidKRBizNumber(String digits10) {
		if (!digits10.matches("\\d{10}"))
			return false;
		int[] w = { 1, 3, 7, 1, 3, 7, 1, 3, 5 };
		int sum = 0;
		for (int i = 0; i < 9; i++)
			sum += (digits10.charAt(i) - '0') * w[i];
		sum += ((digits10.charAt(8) - '0') * 5) / 10;
		int check = (10 - (sum % 10)) % 10;
		return check == (digits10.charAt(9) - '0');
	}
}
