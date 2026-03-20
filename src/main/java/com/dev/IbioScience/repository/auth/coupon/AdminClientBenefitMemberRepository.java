package com.dev.IbioScience.repository.auth.coupon;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.model.auth.Member;

import jakarta.persistence.LockModeType;

public interface AdminClientBenefitMemberRepository extends JpaRepository<Member, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :memberId")
    Optional<Member> findByIdForUpdate(@Param("memberId") Long memberId);
}