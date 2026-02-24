package com.dev.IbioScience.repository.auth;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.enums.auth.MemberStatus;
import com.dev.IbioScience.model.auth.Member;

/** 회원 리포지토리 */
public interface MemberRepository extends JpaRepository<Member, Long> , MemberClientSearchRepository{
	boolean existsByUsername(String username);
    Optional<Member> findByUsername(String username);
    List<Member> findByStatus(MemberStatus status);
    /** 업데이트 시 본인 제외 중복 체크 */
    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByEmail(String email);
}