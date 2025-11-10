package com.dev.IbioScience.service.auth;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.member.auth.StaffCreateRequest;
import com.dev.IbioScience.enums.auth.CustomerType;
import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.enums.auth.MemberDomain;
import com.dev.IbioScience.enums.auth.MemberRole;
import com.dev.IbioScience.enums.auth.MemberStatus;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.repository.auth.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public boolean existsUsername(String username) {
        return memberRepository.existsByUsername(username);
    }

    @Transactional
    public Long createStaff(StaffCreateRequest req) {
        String position = blankToDash(req.getPosition());
        String tel      = blankToDash(req.getTel());
        String mobile   = blankToDash(req.getMobile());
        String email    = blankToDash(req.getEmail());

        MemberRole role = MemberRole.valueOf(req.getRole()); // MASTER/OPERATOR/ADMIN만 UI에서 전달

        Member m = Member.builder()
                .username(req.getUsername().trim())
                .password(passwordEncoder.encode(req.getPassword()))
                .name(req.getName().trim())
                .position(position)
                .tel(tel)
                .mobile(mobile)
                .email(email)

                // ✅ 현재 프로젝트 관례 유지(사내직원 표기): domain은 기존대로 COMPANY 사용
                .domain(MemberDomain.COMPANY)

                // ✅ 신규 기준: 사내직원은 customerType = STAFF 로 고정
                .customerType(CustomerType.STAFF)

                .dealerType(DealerType.NONE)          // 직원은 딜러 아님
                .role(role)
                .status(MemberStatus.ACTIVE)
                .mustChangePassword(true)             // 최초 로그인 시 변경 강제
                .useYn(Boolean.TRUE.equals(req.getUseYn()))
                .joinedAt(LocalDateTime.now())
                .isPrimary(Boolean.TRUE.equals(req.getIsPrimary()))
                .build();

        m = memberRepository.save(m);
        return m.getId();
    }

    private String blankToDash(String v) {
        return (v == null || v.trim().isEmpty()) ? "-" : v.trim();
    }
}