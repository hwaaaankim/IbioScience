package com.dev.IbioScience.model.auth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.dev.IbioScience.dto.view.UiMemberType;
import com.dev.IbioScience.enums.auth.CustomerType;
import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.enums.auth.MemberStatus;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PrincipalDetails implements UserDetails {

	private static final long serialVersionUID = 2025L;

	private final Member member;

	public Member getMember() {
		return member;
	}

	/* =========================
	 *  ✅ 가격/회원 구분(공통)
	 * ========================= */

	public enum PricePolicy {
		GUEST,
		PERSONAL_NORMAL,
		COMPANY_BUYER_GRADE,
		COMPANY_SELLER_GRADE
	}

	public CustomerType getEffectiveCustomerType() {
		if (member == null) return null;
		return member.getCustomerType();
	}

	public DealerType getEffectiveDealerType() {
		if (member == null) return DealerType.NONE;
		return member.getDealerType() == null ? DealerType.NONE : member.getDealerType();
	}

	public boolean isEffectiveCompanyMember() {
		if (member == null) return false;
		return member.getCompanyProfile() != null;
	}

	public boolean isEffectiveDealer() {
		return getEffectiveDealerType() != DealerType.NONE;
	}

	public boolean isStaff() {
		return member != null && member.getCustomerType() == CustomerType.STAFF;
	}

	/**
	 * ✅ 공통 가격 정책 계산
	 *
	 * 전제:
	 * - 로그인 가능 상태: ACTIVE, WITHDRAWN
	 * - 그 외(PENDING/SUSPENDED/DORMANT/DELETED): 로그인 차단 → 여기서는 GUEST 처리
	 */
	public PricePolicy getPricePolicy() {

		if (member == null) return PricePolicy.GUEST;

		MemberStatus status = member.getStatus();
		boolean loginAllowedStatus = (status == MemberStatus.ACTIVE || status == MemberStatus.WITHDRAWN);

		// ✅ ACTIVE / WITHDRAWN 만 정상 처리, 나머지는 GUEST로 폴백
		if (!loginAllowedStatus) return PricePolicy.GUEST;

		// 직원 정책(필요 시 확장)
		if (isStaff()) return PricePolicy.PERSONAL_NORMAL;

		final boolean company = isEffectiveCompanyMember();
		final DealerType dealer = getEffectiveDealerType();

		if (!company && dealer == DealerType.NONE) return PricePolicy.PERSONAL_NORMAL;
		if (company && dealer == DealerType.BUYER) return PricePolicy.COMPANY_BUYER_GRADE;
		if (company && dealer == DealerType.SELLER) return PricePolicy.COMPANY_SELLER_GRADE;

		return PricePolicy.PERSONAL_NORMAL;
	}

	public String getPricePolicyKey() {
		return getPricePolicy().name();
	}

	/* =========================
	 *  ✅ 화면 분기용 회원유형
	 * ========================= */

	public UiMemberType getUiMemberType() {

		if (member == null) return UiMemberType.PERSONAL_USER;

		CustomerType ctype = member.getCustomerType();
		DealerType dealer = getEffectiveDealerType();

		if (ctype == null) {
			ctype = (member.getCompanyProfile() != null) ? CustomerType.BUSINESS : CustomerType.PERSONAL;
		}

		if (ctype == CustomerType.STAFF) {
			return UiMemberType.STAFF;
		}

		if (ctype == CustomerType.PERSONAL) {
			if (dealer == DealerType.BUYER) return UiMemberType.PERSONAL_BUYER_DEALER;
			return UiMemberType.PERSONAL_USER;
		}

		if (ctype == CustomerType.BUSINESS) {
			if (dealer == DealerType.SELLER) return UiMemberType.COMPANY_SELLER_DEALER;
			if (dealer == DealerType.BUYER) return UiMemberType.COMPANY_BUYER_DEALER;
			return UiMemberType.COMPANY_USER;
		}

		return UiMemberType.PERSONAL_USER;
	}

	public String getUiMemberTypeName() {
		return getUiMemberType().name();
	}

	/* =========================
	 *  ✅ Spring Security 계약
	 * ========================= */

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {

		if (member == null) return List.of();

		Set<GrantedAuthority> auths = new LinkedHashSet<>();

		if (member.getRole() != null) {
			auths.add(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()));
		}

		DealerType dealer = getEffectiveDealerType();
		if (dealer == DealerType.BUYER) {
			auths.add(new SimpleGrantedAuthority("ROLE_BUYER_DEALER"));
		} else if (dealer == DealerType.SELLER) {
			auths.add(new SimpleGrantedAuthority("ROLE_SELLER_DEALER"));
			auths.add(new SimpleGrantedAuthority("ROLE_BUYER_DEALER"));
		}

		return new ArrayList<>(auths);
	}

	@Override
	public String getPassword() {
		return member.getPassword();
	}

	@Override
	public String getUsername() {
		return member.getUsername();
	}

	@Override public boolean isAccountNonExpired() { return true; }
	@Override public boolean isAccountNonLocked() { return true; }
	@Override public boolean isCredentialsNonExpired() { return true; }

	/**
	 * ✅ 로그인 가능 여부
	 * 요구사항:
	 * - ACTIVE: 로그인 가능
	 * - WITHDRAWN: 로그인 가능
	 * - PENDING/SUSPENDED/DORMANT/DELETED: 로그인 불가
	 */
	@Override
	public boolean isEnabled() {
		if (member == null) return false;
		if (!member.isUseYn()) return false;

		MemberStatus status = member.getStatus();
		return (status == MemberStatus.ACTIVE || status == MemberStatus.WITHDRAWN);
	}
}