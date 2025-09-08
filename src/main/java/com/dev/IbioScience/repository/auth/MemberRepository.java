package com.dev.IbioScience.repository.auth;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.enums.MemberStatus;

/** 회원 리포지토리 */
public interface MemberRepository extends JpaRepository<Member, Long> {
	boolean existsByLoginId(String loginId);
	boolean existsByUsername(String username);
    Optional<Member> findByUsername(String username);
    Optional<Member> findByLoginId(String loginId);
    List<Member> findByStatus(MemberStatus status);
}