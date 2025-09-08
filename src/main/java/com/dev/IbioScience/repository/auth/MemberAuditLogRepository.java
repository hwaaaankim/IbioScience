package com.dev.IbioScience.repository.auth;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.MemberAuditLog;

public interface MemberAuditLogRepository extends JpaRepository<MemberAuditLog, Long> {
	List<MemberAuditLog> findByMemberOrderByCreatedAtDesc(Member member);
}