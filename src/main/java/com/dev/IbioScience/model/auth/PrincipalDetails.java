package com.dev.IbioScience.model.auth;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.dev.IbioScience.model.auth.enums.MemberStatus;

import lombok.RequiredArgsConstructor;

/** 인증 주체 */
@RequiredArgsConstructor
public class PrincipalDetails implements UserDetails {

    /**
	 * 
	 */
	private static final long serialVersionUID = 2025L;
	private final Member member;

    public Member getMember() { return member; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = "ROLE_" + member.getRole().name();
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() { return member.getPassword(); }

    @Override
    public String getUsername() { return member.getLoginId(); }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return member.getStatus() != MemberStatus.SUSPENDED; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return member.getStatus() == MemberStatus.ACTIVE; }
}