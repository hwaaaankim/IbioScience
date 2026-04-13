package com.dev.IbioScience.model.auth.role;


import com.dev.IbioScience.enums.auth.role.AdminPermissionAction;

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
    name = "admin_managed_page_endpoint",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_admin_page_endpoint",
            columnNames = {"page_id", "http_method", "path_pattern", "action"}
        )
    },
    indexes = {
        @Index(name = "ix_admin_page_endpoint_action", columnList = "action"),
        @Index(name = "ix_admin_page_endpoint_use", columnList = "use_yn")
    }
)
public class AdminManagedPageEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 연결된 페이지 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "page_id", nullable = false)
    private AdminManagedPage page;

    /** HTTP Method (GET/POST/PUT/DELETE 등) */
    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    /** Ant Path Pattern */
    @Column(name = "path_pattern", nullable = false, length = 255)
    private String pathPattern;

    /** 해당 엔드포인트가 요구하는 권한 액션 */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private AdminPermissionAction action;

    /** 사용 여부 */
    @Column(name = "use_yn", nullable = false)
    private boolean useYn;
}