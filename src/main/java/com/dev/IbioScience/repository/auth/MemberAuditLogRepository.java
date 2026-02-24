package com.dev.IbioScience.repository.auth;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.enums.logging.MemberAuditAction;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.MemberAuditLog;

public interface MemberAuditLogRepository extends JpaRepository<MemberAuditLog, Long> {
	List<MemberAuditLog> findByMemberOrderByCreatedAtDesc(Member member);
	
	long countByActionAndCreatedAtBetween(MemberAuditAction action, LocalDateTime start, LocalDateTime end);

	Page<MemberAuditLog> findByActionAndCreatedAtBetweenOrderByCreatedAtDesc(
			MemberAuditAction action,
			LocalDateTime start,
			LocalDateTime end,
			Pageable pageable
	);
}