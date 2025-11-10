package com.dev.IbioScience.service.auth.temp;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.dev.IbioScience.dto.member.auth.RootInitResponse;
import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.enums.auth.MemberDomain;
import com.dev.IbioScience.enums.auth.MemberRole;
import com.dev.IbioScience.enums.auth.MemberStatus;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.repository.auth.MemberRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RootInitService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RootInitResponse createRootIfAbsent() {
        // 1) 이미 root 아이디가 있거나, ROOT 역할 계정이 있으면 생성하지 않음
        boolean existsById = memberRepository.existsByUsername("root");
        boolean existsRootRole = memberRepository.findAll().stream()
                .anyMatch(m -> m.getRole() == MemberRole.ROOT);

        if (existsById || existsRootRole) {
            Member m = memberRepository.findByUsername("root").orElse(null);
            Long id = (m != null) ? m.getId() : null;
            return new RootInitResponse(false, id, "root", MemberRole.ROOT.name(), "ROOT 계정이 이미 존재합니다.");
        }

        // 2) 새 ROOT 계정 생성
        Member root = Member.builder()
                .username("root")
                .password(passwordEncoder.encode("12345"))
                .name("시스템루트")
                .tel(null)
                .mobile(null)
                .email("root@ibio.local")
                .address(null) // 운영계정이라 주소 불필요
                .domain(MemberDomain.COMPANY)
                .dealerType(DealerType.NONE)
                .role(MemberRole.ROOT)
                .status(MemberStatus.ACTIVE)
                .companyProfile(null)
                .organizationName(null)
                .joinedAt(LocalDateTime.now())
                .withdrewAt(null)
                .mustChangePassword(true)    // 최초 로그인 시 비번변경 강제
                .lastPasswordChangedAt(null)
                .position("루트")
                .useYn(true)
                .build();

        root = memberRepository.save(root);

        return new RootInitResponse(true, root.getId(), root.getUsername(), root.getRole().name(), "ROOT 계정을 생성했습니다.");
    }
}