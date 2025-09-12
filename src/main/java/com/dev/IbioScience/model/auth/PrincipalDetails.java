package com.dev.IbioScience.model.auth;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.dev.IbioScience.dto.view.UiMemberType;
import com.dev.IbioScience.model.auth.enums.CustomerType;
import com.dev.IbioScience.model.auth.enums.DealerType;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PrincipalDetails implements UserDetails {

	private static final long serialVersionUID = 2025L;
	private final Member member;

	public Member getMember() {
		return member;
	}

	/** ✅ 뷰/템플릿 분기용 회원유형 판별 (customerType만으로 결정) */
	public UiMemberType getUiMemberType() {
		if (member == null)
			return UiMemberType.PERSONAL_USER;

		final CustomerType ctype = member.getCustomerType();
		final DealerType dealer = member.getDealerType();

		// 0) 우리회사 직원
		if (ctype == CustomerType.STAFF) {
			return UiMemberType.STAFF;
		}

		// 1) 개인 소비자
		if (ctype == CustomerType.PERSONAL) {
			if (dealer == DealerType.BUYER)
				return UiMemberType.PERSONAL_BUYER_DEALER;
			return UiMemberType.PERSONAL_USER;
		}

		// 2) 회사(법인) 소비자
		if (ctype == CustomerType.BUSINESS) {
			if (dealer == DealerType.SELLER)
				return UiMemberType.COMPANY_SELLER_DEALER; // 판매=구매 포함
			if (dealer == DealerType.BUYER)
				return UiMemberType.COMPANY_BUYER_DEALER;
			return UiMemberType.COMPANY_USER;
		}

		// 3) 폴백
		return UiMemberType.PERSONAL_USER;
	}

	public String getUiMemberTypeName() {
		return getUiMemberType().name();
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
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}