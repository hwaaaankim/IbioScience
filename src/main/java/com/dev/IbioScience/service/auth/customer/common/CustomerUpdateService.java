package com.dev.IbioScience.service.auth.customer.common;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.customer.auth.CustomerPersonalInfoUpdateRequest;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.embedded.Address;
import com.dev.IbioScience.repository.auth.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerUpdateService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional(readOnly = true)
	public Member getMemberOrThrow(Long id) {
		return memberRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
	}

	@Transactional
	public Member updatePersonal(Long actorId, CustomerPersonalInfoUpdateRequest req) {
		Long targetId;
		try {
			targetId = Long.valueOf(req.getId());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("잘못된 요청입니다.");
		}

		Member m = getMemberOrThrow(targetId);

		// 본인만 수정 가능 (정책 변경 시 알려주세요)
		if (!m.getId().equals(actorId)) {
			throw new SecurityException("본인 정보만 수정할 수 있습니다.");
		}

		// 아이디 중복 (자기 자신 제외)
		if (memberRepository.existsByUsernameAndIdNot(req.getUsername().trim(), m.getId())) {
			throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
		}

		// 반영: 아이디/비번/이름
		m.setUsername(req.getUsername().trim());
		m.setPassword(passwordEncoder.encode(req.getPassword()));
		m.setName(req.getName().trim());

		// 휴대폰/유선
		String mobile = String.join("-", req.getMobile1(), req.getMobile2(), req.getMobile3());
		m.setMobile(mobile);

		if (isNotBlank(req.getTel1()) && isNotBlank(req.getTel2()) && isNotBlank(req.getTel3())) {
			m.setTel(String.join("-", req.getTel1(), req.getTel2(), req.getTel3()));
		} else {
			m.setTel(null);
		}

		// 주소
		Address addr = m.getAddress();
		if (addr == null)
			addr = new Address();
		addr.setPostcode(req.getZipcode());
		addr.setRoadAddress(req.getRoadAddress());
		addr.setJibunAddress(req.getJibunAddress());
		addr.setDetailAddress(req.getDetailAddress());
		m.setAddress(addr);

		// 이메일/소속
		m.setEmail(req.getEmail().trim());
		m.setOrganizationName(nullToEmpty(req.getOrganizationName()));

		// 감사성 정보
		m.setLastPasswordChangedAt(LocalDateTime.now());
		m.setMustChangePassword(false);

		// JPA dirty checking
		return m;
	}

	private boolean isNotBlank(String s) {
		return s != null && !s.trim().isEmpty();
	}

	private String nullToEmpty(String s) {
		return (s == null) ? "" : s;
	}
}