package com.dev.IbioScience.model.auth;

import com.dev.IbioScience.model.auth.embedded.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회원 변경 로그 — 가입/탈퇴(액션명) + 일반필드 변경(fieldName 기반)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "member_audit_log", indexes = { @Index(name = "ix_audit_member", columnList = "member_id"),
		@Index(name = "ix_audit_created_at", columnList = "createdAt") })
public class MemberAuditLog extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 대상 멤버 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	/** 액션(JOIN, WITHDRAW, UPDATE, ROLE_CHANGE 등 — 자유기재) */
	@Column(length = 50, nullable = false)
	private String action;

	/** 변경 필드명 (가입/탈퇴 등 이벤트성 로그는 null 가능) */
	@Column(length = 100)
	private String fieldName;

	/** 이전값/이후값 (간단 텍스트) */
	@Column(length = 1000)
	private String oldValue;

	@Column(length = 1000)
	private String newValue;

	/** 수행자(관리자) ID (선택) */
	private Long actorMemberId;
}