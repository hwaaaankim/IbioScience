package com.dev.IbioScience.service.auth.admin.common;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.member.auth.StaffUpdateRequest;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.enums.MemberRole;
import com.dev.IbioScience.repository.auth.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminMemberCommonService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Member getMemberOrThrow(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 직원입니다."));
    }

    /**
     * 규칙:
     * - role 변경: canChangeRole == true 여야 함 (루트 본인은 false) + ROOT로 변경 금지
     * - useYn, isPrimary 변경: canManage == true (루트/마스터)
     * - username/name/password/position/tel/mobile/email: self만 변경
     * - password: 빈값이면 미변경, 값 있으면 8자 이상 + self일 때만 mustChangePassword=false & lastPasswordChangedAt=now
     */
    @Transactional
    public void updateStaff(StaffUpdateRequest req, Long currentUserId,
                            boolean canManage, boolean canChangeRole) {

        Member target = getMemberOrThrow(req.getId());
        boolean isSelf = target.getId().equals(currentUserId);

        // ===== 역할(role) 변경 - 분리된 플래그로 제어 =====
        if (canChangeRole && req.getRole() != null) {
            MemberRole newRole = req.getRole();
            if (newRole == MemberRole.ROOT) {
                // 프론트에서 옵션을 안 주더라도 서버에서 한 번 더 차단
                throw new IllegalArgumentException("ROOT 권한으로는 변경할 수 없습니다.");
            }
            target.setRole(newRole); // MASTER/OPERATOR/ADMIN 허용
        }

        // ===== 사용여부 / 대표여부 - 루트/마스터 가능 =====
        if (canManage) {
            if (req.getUseYn() != null) {
                target.setUseYn(Boolean.TRUE.equals(req.getUseYn()));
            }
            if (req.getIsPrimary() != null) {
                target.setPrimary(Boolean.TRUE.equals(req.getIsPrimary()));
            }
        }

        // ===== 본인(self)만 변경 가능한 필드들 =====
        if (isSelf) {
            if (req.getUsername() != null && !req.getUsername().isBlank()) {
                String u = req.getUsername().trim();
                if (memberRepository.existsByUsernameAndIdNot(u, target.getId())) {
                    throw new IllegalStateException("이미 사용 중인 아이디입니다.");
                }
                target.setUsername(u);
            }
            if (req.getName() != null && !req.getName().isBlank()) {
                target.setName(req.getName().trim());
            }
            if (req.getPosition() != null) {
                target.setPosition(req.getPosition().trim());
            }
            if (req.getTel() != null) {
                target.setTel(req.getTel().trim());
            }
            if (req.getMobile() != null) {
                target.setMobile(req.getMobile().trim());
            }
            if (req.getEmail() != null) {
                target.setEmail(req.getEmail().trim());
            }

            // 비밀번호: 빈값이면 미변경
            if (req.getPassword() != null && !req.getPassword().isBlank()) {
                if (req.getPassword().length() < 8) {
                    throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");
                }
                String enc = passwordEncoder.encode(req.getPassword());
                target.setPassword(enc);

                target.setMustChangePassword(false);
                target.setLastPasswordChangedAt(LocalDateTime.now());
            }
        }

        memberRepository.save(target);
    }
}