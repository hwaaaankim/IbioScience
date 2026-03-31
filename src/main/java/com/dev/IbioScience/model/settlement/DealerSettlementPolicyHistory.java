package com.dev.IbioScience.model.settlement;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.dev.IbioScience.enums.product.SettlementBasis;
import com.dev.IbioScience.enums.product.SettlementCycle;
import com.dev.IbioScience.model.auth.SellerDealerProfile;
import com.dev.IbioScience.model.auth.embedded.BaseTimeEntity;

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
    name = "tb_dealer_settlement_policy_history",
    indexes = {
        @Index(name = "ix_settle_policy_hist_seller", columnList = "seller_dealer_profile_id"),
        @Index(name = "ix_settle_policy_hist_apply_start", columnList = "apply_start_date"),
        @Index(name = "ix_settle_policy_hist_apply_end", columnList = "apply_end_date")
    }
)
public class DealerSettlementPolicyHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대상 셀러 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_dealer_profile_id", nullable = false)
    private SellerDealerProfile sellerDealerProfile;

    /** 현재 policy 테이블의 id 스냅샷 */
    @Column(name = "source_policy_id")
    private Long sourcePolicyId;

    /** 이 정책이 적용되는 시작일(포함) */
    @Column(name = "apply_start_date", nullable = false)
    private LocalDate applyStartDate;

    /** 이 정책이 적용되는 종료일(포함), 현재 활성정책이면 null */
    @Column(name = "apply_end_date")
    private LocalDate applyEndDate;

    /** 수수료율 스냅샷 */
    @Column(name = "commission_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal commissionRate;

    /** 정산주기 스냅샷 */
    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_cycle", nullable = false, length = 20)
    private SettlementCycle cycle;

    /** 정산기준 스냅샷 */
    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_basis", nullable = false, length = 30)
    private SettlementBasis basis;

    /** 변경자 정보 스냅샷 */
    @Column(name = "changed_by_member_id")
    private Long changedByMemberId;

    @Column(name = "changed_by_username", length = 100)
    private String changedByUsername;

    @Column(name = "changed_by_name", length = 100)
    private String changedByName;

    /** 셀러/회원/회사 스냅샷 */
    @Column(name = "seller_member_id_snapshot")
    private Long sellerMemberIdSnapshot;

    @Column(name = "member_username_snapshot", length = 100)
    private String memberUsernameSnapshot;

    @Column(name = "member_name_snapshot", length = 100)
    private String memberNameSnapshot;

    @Column(name = "member_email_snapshot", length = 200)
    private String memberEmailSnapshot;

    @Column(name = "member_mobile_snapshot", length = 30)
    private String memberMobileSnapshot;

    @Column(name = "company_name_snapshot", length = 200)
    private String companyNameSnapshot;

    @Column(name = "shop_name_snapshot", length = 200)
    private String shopNameSnapshot;

    @Column(name = "supplier_code_snapshot", length = 40)
    private String supplierCodeSnapshot;
}