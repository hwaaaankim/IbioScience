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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "member_memo",
    indexes = {
        @Index(name = "ix_member_memo_target_member", columnList = "target_member_id"),
        @Index(name = "ix_member_memo_created_at", columnList = "created_at"),
        @Index(name = "ix_member_memo_target_created", columnList = "target_member_id, created_at")
    }
)
public class MemberMemo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 메모 대상 회원 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_member_id", nullable = false)
    private Member targetMember;

    /** 작성자(아이바이오 직원/관리자) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_member_id", nullable = false)
    private Member writerMember;

    /** 메모 내용 */
    @Lob
    @Column(nullable = false)
    private String content;
}