package com.dev.IbioScience.model.auth.role;

import com.dev.IbioScience.enums.auth.MemberRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
    name = "admin_role_page_permission",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_admin_role_page_permission", columnNames = {"page_id", "role"})
    },
    indexes = {
        @Index(name = "ix_admin_role_page_permission_role", columnList = "role")
    }
)
public class AdminRolePagePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 연결 페이지 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "page_id", nullable = false)
    private AdminManagedPage page;

    /** 대상 역할 */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MemberRole role;

    /** 조회 가능 여부 */
    @Column(name = "can_view", nullable = false)
    private boolean canView;

    /** 등록 가능 여부 */
    @Column(name = "can_create", nullable = false)
    private boolean canCreate;

    /** 수정 가능 여부 */
    @Column(name = "can_update", nullable = false)
    private boolean canUpdate;

    /** 삭제 가능 여부 */
    @Column(name = "can_delete", nullable = false)
    private boolean canDelete;
}