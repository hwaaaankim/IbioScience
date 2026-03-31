package com.dev.IbioScience.service.settlement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.model.auth.DealerSettlementPolicy;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.SellerDealerProfile;
import com.dev.IbioScience.model.settlement.DealerSettlementPolicyHistory;
import com.dev.IbioScience.repository.auth.MemberRepository;
import com.dev.IbioScience.repository.settlement.DealerSettlementPolicyHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DealerSettlementPolicyHistoryService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DealerSettlementPolicyHistoryRepository historyRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void syncHistoryOnPolicySave(SellerDealerProfile seller, DealerSettlementPolicy policy, Long changedByMemberId) {
        LocalDate today = LocalDate.now(KST);

        DealerSettlementPolicyHistory currentHistory = findCurrentHistory(seller.getId(), today);
        DealerSettlementPolicyHistory scheduledHistory = findFirstScheduledHistory(seller.getId(), today);

        if (currentHistory == null && scheduledHistory == null) {
            policy.setNextSettlementDate(null);
            historyRepository.save(buildHistory(
                seller,
                policy,
                resolveInitialApplyStartDate(seller),
                null,
                changedByMemberId
            ));
            return;
        }

        if (currentHistory == null) {
            throw new IllegalStateException("현재 적용 중인 정산 정책 히스토리를 찾을 수 없습니다.");
        }

        boolean sameAsCurrent = isSamePolicy(currentHistory, policy);
        boolean sameAsScheduled = isSamePolicy(scheduledHistory, policy);

        if (scheduledHistory == null) {
            if (sameAsCurrent) {
                policy.setNextSettlementDate(null);
                return;
            }

            LocalDate newApplyStart = resolveRequestedFutureApplyStartDate(policy, null, today);
            validateNewApplyStart(currentHistory, newApplyStart, today);

            currentHistory.setApplyEndDate(newApplyStart.minusDays(1));
            historyRepository.save(currentHistory);

            policy.setNextSettlementDate(newApplyStart);
            historyRepository.save(buildHistory(
                seller,
                policy,
                newApplyStart,
                null,
                changedByMemberId
            ));
            return;
        }

        if (sameAsCurrent) {
            currentHistory.setApplyEndDate(null);
            historyRepository.save(currentHistory);

            historyRepository.delete(scheduledHistory);
            policy.setNextSettlementDate(null);
            return;
        }

        LocalDate requestedStart = resolveRequestedFutureApplyStartDate(policy, scheduledHistory, today);
        validateNewApplyStart(currentHistory, requestedStart, today);

        if (sameAsScheduled) {
            currentHistory.setApplyEndDate(requestedStart.minusDays(1));
            historyRepository.save(currentHistory);

            scheduledHistory.setApplyStartDate(requestedStart);
            scheduledHistory.setApplyEndDate(null);
            refreshHistorySnapshot(scheduledHistory, seller, policy, changedByMemberId);
            historyRepository.save(scheduledHistory);

            policy.setNextSettlementDate(requestedStart);
            return;
        }

        currentHistory.setApplyEndDate(requestedStart.minusDays(1));
        historyRepository.save(currentHistory);

        scheduledHistory.setApplyStartDate(requestedStart);
        scheduledHistory.setApplyEndDate(null);
        refreshHistorySnapshot(scheduledHistory, seller, policy, changedByMemberId);
        historyRepository.save(scheduledHistory);

        policy.setNextSettlementDate(requestedStart);
    }

    private DealerSettlementPolicyHistory findCurrentHistory(Long sellerDealerProfileId, LocalDate targetDate) {
        List<DealerSettlementPolicyHistory> rows = historyRepository.findEffectiveHistoriesAtDate(
            sellerDealerProfileId,
            targetDate
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private DealerSettlementPolicyHistory findFirstScheduledHistory(Long sellerDealerProfileId, LocalDate targetDate) {
        List<DealerSettlementPolicyHistory> rows = historyRepository.findScheduledHistoriesAfterDate(
            sellerDealerProfileId,
            targetDate
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private boolean isSamePolicy(DealerSettlementPolicyHistory history, DealerSettlementPolicy policy) {
        if (history == null || policy == null) {
            return false;
        }

        return compareBigDecimal(history.getCommissionRate(), policy.getCommissionRate()) == 0
            && history.getCycle() == policy.getCycle()
            && history.getBasis() == policy.getBasis();
    }

    private LocalDate resolveRequestedFutureApplyStartDate(
        DealerSettlementPolicy policy,
        DealerSettlementPolicyHistory scheduledHistory,
        LocalDate today
    ) {
        if (policy.getNextSettlementDate() != null) {
            if (!policy.getNextSettlementDate().isAfter(today)) {
                throw new IllegalArgumentException("정산 정책 변경 시 nextSettlementDate 는 오늘 이후 날짜여야 합니다.");
            }
            return policy.getNextSettlementDate();
        }

        if (scheduledHistory != null) {
            return scheduledHistory.getApplyStartDate();
        }

        throw new IllegalArgumentException("정산 정책 변경 시 nextSettlementDate 는 필수입니다.");
    }

    private void validateNewApplyStart(
        DealerSettlementPolicyHistory currentHistory,
        LocalDate newApplyStart,
        LocalDate today
    ) {
        if (newApplyStart == null) {
            throw new IllegalArgumentException("새 정책 적용 시작일이 없습니다.");
        }

        if (!newApplyStart.isAfter(today)) {
            throw new IllegalArgumentException("새 정책 적용 시작일은 오늘 이후여야 합니다.");
        }

        if (!newApplyStart.isAfter(currentHistory.getApplyStartDate())) {
            throw new IllegalArgumentException("새 정책 적용 시작일은 기존 현재 정책 시작일보다 뒤여야 합니다.");
        }
    }

    private void refreshHistorySnapshot(
        DealerSettlementPolicyHistory target,
        SellerDealerProfile seller,
        DealerSettlementPolicy policy,
        Long changedByMemberId
    ) {
        Member changedBy = changedByMemberId == null ? null : memberRepository.findById(changedByMemberId).orElse(null);
        Member sellerMember = seller.getMember();

        target.setSellerDealerProfile(seller);
        target.setSourcePolicyId(policy.getId());
        target.setCommissionRate(policy.getCommissionRate() == null ? BigDecimal.ZERO : policy.getCommissionRate());
        target.setCycle(policy.getCycle());
        target.setBasis(policy.getBasis());

        target.setChangedByMemberId(changedBy != null ? changedBy.getId() : null);
        target.setChangedByUsername(changedBy != null ? changedBy.getUsername() : null);
        target.setChangedByName(changedBy != null ? changedBy.getName() : null);

        target.setSellerMemberIdSnapshot(sellerMember != null ? sellerMember.getId() : null);
        target.setMemberUsernameSnapshot(sellerMember != null ? sellerMember.getUsername() : null);
        target.setMemberNameSnapshot(sellerMember != null ? sellerMember.getName() : null);
        target.setMemberEmailSnapshot(sellerMember != null ? sellerMember.getEmail() : null);
        target.setMemberMobileSnapshot(sellerMember != null ? sellerMember.getMobile() : null);
        target.setCompanyNameSnapshot(
            seller.getCompanyProfile() != null ? seller.getCompanyProfile().getCompanyName() : null
        );
        target.setShopNameSnapshot(seller.getShopName());
        target.setSupplierCodeSnapshot(seller.getSupplierCode());
    }

    private DealerSettlementPolicyHistory buildHistory(
        SellerDealerProfile seller,
        DealerSettlementPolicy policy,
        LocalDate applyStartDate,
        LocalDate applyEndDate,
        Long changedByMemberId
    ) {
        Member changedBy = changedByMemberId == null ? null : memberRepository.findById(changedByMemberId).orElse(null);
        Member sellerMember = seller.getMember();

        return DealerSettlementPolicyHistory.builder()
            .sellerDealerProfile(seller)
            .sourcePolicyId(policy.getId())
            .applyStartDate(applyStartDate)
            .applyEndDate(applyEndDate)
            .commissionRate(policy.getCommissionRate() == null ? BigDecimal.ZERO : policy.getCommissionRate())
            .cycle(policy.getCycle())
            .basis(policy.getBasis())
            .changedByMemberId(changedBy != null ? changedBy.getId() : null)
            .changedByUsername(changedBy != null ? changedBy.getUsername() : null)
            .changedByName(changedBy != null ? changedBy.getName() : null)
            .sellerMemberIdSnapshot(sellerMember != null ? sellerMember.getId() : null)
            .memberUsernameSnapshot(sellerMember != null ? sellerMember.getUsername() : null)
            .memberNameSnapshot(sellerMember != null ? sellerMember.getName() : null)
            .memberEmailSnapshot(sellerMember != null ? sellerMember.getEmail() : null)
            .memberMobileSnapshot(sellerMember != null ? sellerMember.getMobile() : null)
            .companyNameSnapshot(
                seller.getCompanyProfile() != null ? seller.getCompanyProfile().getCompanyName() : null
            )
            .shopNameSnapshot(seller.getShopName())
            .supplierCodeSnapshot(seller.getSupplierCode())
            .build();
    }

    private LocalDate resolveInitialApplyStartDate(SellerDealerProfile seller) {
        if (seller.getDealStartDate() != null) {
            return seller.getDealStartDate();
        }
        if (seller.getCreatedAt() != null) {
            return seller.getCreatedAt().toLocalDate();
        }
        return LocalDate.now(KST);
    }

    private int compareBigDecimal(BigDecimal a, BigDecimal b) {
        BigDecimal left = a == null ? BigDecimal.ZERO : a;
        BigDecimal right = b == null ? BigDecimal.ZERO : b;
        return left.compareTo(right);
    }
}