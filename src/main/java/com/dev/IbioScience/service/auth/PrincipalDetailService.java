package com.dev.IbioScience.service.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.dev.IbioScience.enums.auth.MemberStatus;
import com.dev.IbioScience.exception.InactiveMemberException;
import com.dev.IbioScience.exception.UseYnDisabledException;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.PrincipalDetails;
import com.dev.IbioScience.repository.auth.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrincipalDetailService implements UserDetailsService {

	private final MemberRepository memberRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		Member m = memberRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 아이디입니다."));

		// 사용여부 우선 차단
		if (!m.isUseYn()) {
			throw new UseYnDisabledException("사용불가 계정입니다. 관리자에게 문의하세요.");
		}

		// ✅ 상태 차단 정책: ACTIVE/WITHDRAWN만 허용
		MemberStatus st = m.getStatus();
		boolean loginAllowed = (st == MemberStatus.ACTIVE || st == MemberStatus.WITHDRAWN);

		if (!loginAllowed) {
			// FailureHandler가 "계정 상태:"를 파싱하므로 형식 유지
			throw new InactiveMemberException("사용할 수 없는 계정 상태: " + (st == null ? "UNKNOWN" : st.name()));
		}

		return new PrincipalDetails(m);
	}
}