package com.dev.IbioScience.model.auth.role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
    name = "admin_managed_page",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_admin_managed_page_code", columnNames = "page_code"),
        @UniqueConstraint(name = "uk_admin_managed_page_url", columnNames = "page_url")
    },
    indexes = {
        @Index(name = "ix_admin_managed_page_group_order", columnList = "menu_order, page_order"),
        @Index(name = "ix_admin_managed_page_use", columnList = "use_yn")
    }
)
public class AdminManagedPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 시스템 내부 페이지 코드 */
    @Column(name = "page_code", nullable = false, length = 100)
    private String pageCode;

    /** 화면 표시용 페이지명 */
    @Column(name = "page_name", nullable = false, length = 200)
    private String pageName;

    /** 대표 URL 또는 패턴 */
    @Column(name = "page_url", nullable = false, length = 255)
    private String pageUrl;

    /** 메뉴 그룹명 */
    @Column(name = "menu_group_name", nullable = false, length = 100)
    private String menuGroupName;

    /** 메뉴 그룹 정렬순서 */
    @Column(name = "menu_order", nullable = false)
    private Integer menuOrder;

    /** 그룹 내 페이지 정렬순서 */
    @Column(name = "page_order", nullable = false)
    private Integer pageOrder;

    /** 설명 */
    @Column(name = "description", length = 500)
    private String description;

    /** 사용 여부 */
    @Column(name = "use_yn", nullable = false)
    private boolean useYn;
}