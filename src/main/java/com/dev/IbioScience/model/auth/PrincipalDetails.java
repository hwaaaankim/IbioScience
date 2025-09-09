package com.dev.IbioScience.model.auth;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.RequiredArgsConstructor;

/** 인증 주체 */
@RequiredArgsConstructor
public class PrincipalDetails implements UserDetails {

	private static final long serialVersionUID = 2025L;
	private final Member member;

	public Member getMember() {
		return member;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		String role = "ROLE_" + member.getRole().name();
		return List.of(new SimpleGrantedAuthority(role));
	}

	@Override
	public String getPassword() {
		return member.getPassword();
	}

	@Override
	public String getUsername() {
		return member.getUsername();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	} // 세밀 검사는 Service에서 처리

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	// ★ 상태/사용여부는 Service에서 커스텀 예외로 이미 차단 → 여기서는 true
	@Override
	public boolean isEnabled() {
		return true;
	}
}