package com.dev.IbioScience.service.front.myPgae;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.enums.auth.MemberStatus;
import com.dev.IbioScience.model.auth.Member;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

@Service
public class MemberWithdrawalService {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public void applyWithdrawal(Long memberId) {

        Member member = em.find(Member.class, memberId, LockModeType.PESSIMISTIC_WRITE);
        if (member == null) {
            throw new IllegalArgumentException("회원 정보를 찾을 수 없습니다.");
        }

        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new IllegalArgumentException("이미 신청내역이 있습니다.");
        }

        // 정책상 즉시 탈퇴처리
        member.setStatus(MemberStatus.WITHDRAWN);
        member.setWithdrewAt(LocalDateTime.now());

    }
}