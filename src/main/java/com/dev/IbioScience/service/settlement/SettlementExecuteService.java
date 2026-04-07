package com.dev.IbioScience.service.settlement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.dev.IbioScience.dto.settlement.SettlementExecutePreviewResponse;
import com.dev.IbioScience.dto.settlement.SettlementExecutePreviewRowDto;
import com.dev.IbioScience.dto.settlement.SettlementExecuteResultResponse;
import com.dev.IbioScience.dto.settlement.SettlementExecuteSearchRequest;
import com.dev.IbioScience.dto.settlement.SettlementOrderSummarySourceDto;
import com.dev.IbioScience.enums.product.SettlementBasis;
import com.dev.IbioScience.enums.product.SettlementCycle;
import com.dev.IbioScience.enums.settlement.SettlementBatchStatus;
import com.dev.IbioScience.enums.settlement.SettlementOrderInclusionStatus;
import com.dev.IbioScience.enums.settlement.SettlementPayStatus;
import com.dev.IbioScience.model.auth.DealerSettlementPolicy;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.PrincipalDetails;
import com.dev.IbioScience.model.auth.SellerDealerProfile;
import com.dev.IbioScience.model.order.Order;
import com.dev.IbioScience.model.settlement.DealerSettlement;
import com.dev.IbioScience.model.settlement.DealerSettlementBatch;
import com.dev.IbioScience.model.settlement.DealerSettlementOrder;
import com.dev.IbioScience.model.settlement.DealerSettlementPolicyHistory;
import com.dev.IbioScience.repository.settlement.DealerSettlementBatchRepository;
import com.dev.IbioScience.repository.settlement.DealerSettlementOrderRepository;
import com.dev.IbioScience.repository.settlement.DealerSettlementPolicyHistoryRepository;
import com.dev.IbioScience.repository.settlement.DealerSettlementPolicyRepository;
import com.dev.IbioScience.repository.settlement.DealerSettlementRepository;
import com.dev.IbioScience.repository.settlement.SettlementOrderSourceQueryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettlementExecuteService {

    private final DealerSettlementPolicyRepository policyRepository;
    private final DealerSettlementPolicyHistoryRepository policyHistoryRepository;
    private final DealerSettlementPolicyHistoryService policyHistoryService;
    private final DealerSettlementBatchRepository settlementBatchRepository;
    private final DealerSettlementRepository settlementRepository;
    private final DealerSettlementOrderRepository settlementOrderRepository;
    private final SettlementOrderSourceQueryRepository settlementOrderSourceQueryRepository;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public SettlementExecutePreviewResponse preview(SettlementExecuteSearchRequest request) {
        ExecutionCondition condition = normalizeAndValidate(request);

        debug("==================================================");
        debug("[SETTLEMENT][PREVIEW] START");
        debug("[SETTLEMENT][PREVIEW] request.fromDate=" + request.getFromDate()
            + ", request.toDate=" + request.getToDate()
            + ", request.cycles=" + request.getCycles()
            + ", request.bases=" + request.getBases()
            + ", request.keyword=" + normalizeKeyword(request.getKeyword()));
        debug("[SETTLEMENT][PREVIEW] effectiveFromDate=" + condition.getEffectiveFromDate()
            + ", effectiveToDate=" + condition.getEffectiveToDate());

        List<Candidate> candidates = buildCandidates(request, condition);

        debug("[SETTLEMENT][PREVIEW] candidateCount=" + candidates.size());
        for (Candidate candidate : candidates) {
            debug("[SETTLEMENT][PREVIEW][CANDIDATE] sellerDealerProfileId=" + candidate.getSellerDealerProfile().getId()
                + ", cycle=" + candidate.getCycle()
                + ", basis=" + candidate.getBasis()
                + ", period=" + candidate.getPeriodStartDate() + " ~ " + candidate.getPeriodEndDate()
                + ", orderCount=" + candidate.getOrderCount()
                + ", itemCount=" + candidate.getItemCount()
                + ", gross=" + candidate.getGrossAmount()
                + ", commission=" + candidate.getCommissionAmount()
                + ", settlement=" + candidate.getSettlementAmount()
                + ", commissionRate=" + candidate.getCommissionRate());
        }
        debug("[SETTLEMENT][PREVIEW] END");
        debug("==================================================");

        return toPreviewResponse(candidates);
    }

    @Transactional
    public SettlementExecuteResultResponse execute(SettlementExecuteSearchRequest request, Authentication authentication) {
        ExecutionCondition condition = normalizeAndValidate(request);
        Member admin = resolveAdmin(authentication);

        debug("==================================================");
        debug("[SETTLEMENT][RUN] START");
        debug("[SETTLEMENT][RUN] request.fromDate=" + request.getFromDate()
            + ", request.toDate=" + request.getToDate()
            + ", request.cycles=" + request.getCycles()
            + ", request.bases=" + request.getBases()
            + ", request.keyword=" + normalizeKeyword(request.getKeyword()));
        debug("[SETTLEMENT][RUN] effectiveFromDate=" + condition.getEffectiveFromDate()
            + ", effectiveToDate=" + condition.getEffectiveToDate()
            + ", adminId=" + (admin != null ? admin.getId() : null));

        List<Candidate> candidates = buildCandidates(request, condition);

        debug("[SETTLEMENT][RUN] candidateCount=" + candidates.size());

        DealerSettlementBatch batch = settlementBatchRepository.save(
            DealerSettlementBatch.builder()
                .requestedFromDate(resolveRequestedFromDate(condition, candidates))
                .requestedToDate(condition.getEffectiveToDate())
                .requestedCyclesCsv(toCsv(request.getCycles()))
                .requestedBasesCsv(toCsv(request.getBases()))
                .keyword(normalizeKeyword(request.getKeyword()))
                .requestedByMemberId(admin != null ? admin.getId() : null)
                .requestedByUsername(admin != null ? admin.getUsername() : null)
                .requestedByName(admin != null ? admin.getName() : null)
                .status(SettlementBatchStatus.RUNNING)
                .targetHistoryCount(candidates.size())
                .createdSettlementCount(0)
                .startedAt(LocalDateTime.now())
                .build()
        );

        List<SettlementExecutePreviewRowDto> createdRows = new ArrayList<>();
        int createdCount = 0;

        for (Candidate candidate : candidates) {
            boolean exactExists = settlementRepository
                .findBySellerDealerProfile_IdAndPeriodStartDateAndPeriodEndDateAndSettlementBasis(
                    candidate.getSellerDealerProfile().getId(),
                    candidate.getPeriodStartDate(),
                    candidate.getPeriodEndDate(),
                    candidate.getBasis()
                )
                .isPresent();

            debug("[SETTLEMENT][RUN][CHECK-EXACT] sellerDealerProfileId=" + candidate.getSellerDealerProfile().getId()
                + ", basis=" + candidate.getBasis()
                + ", period=" + candidate.getPeriodStartDate() + " ~ " + candidate.getPeriodEndDate()
                + ", exactExists=" + exactExists);

            if (exactExists) {
                continue;
            }

            DealerSettlementPolicyHistory referenceHistory = resolveReferenceHistory(candidate, admin);

            DealerSettlement settlement = DealerSettlement.builder()
                .sellerDealerProfile(candidate.getSellerDealerProfile())
                .policyHistory(referenceHistory)
                .batch(batch)
                .periodStartDate(candidate.getPeriodStartDate())
                .periodEndDate(candidate.getPeriodEndDate())
                .settlementCycle(candidate.getCycle())
                .settlementBasis(candidate.getBasis())
                .commissionRate(candidate.getCommissionRate())
                .grossAmount(candidate.getGrossAmount())
                .commissionAmount(candidate.getCommissionAmount())
                .settlementAmount(candidate.getSettlementAmount())
                .orderCount(candidate.getOrderCount())
                .itemCount(candidate.getItemCount())
                .payStatus(SettlementPayStatus.UNPAID)
                .executedAt(LocalDateTime.now())
                .sellerMemberIdSnapshot(candidate.getSellerMemberIdSnapshot())
                .memberUsernameSnapshot(candidate.getMemberUsername())
                .memberNameSnapshot(candidate.getMemberName())
                .memberEmailSnapshot(candidate.getMemberEmail())
                .memberMobileSnapshot(candidate.getMemberMobile())
                .companyNameSnapshot(candidate.getCompanyName())
                .shopNameSnapshot(candidate.getShopName())
                .supplierCodeSnapshot(candidate.getSupplierCode())
                .build();

            DealerSettlement saved = settlementRepository.save(settlement);

            debug("[SETTLEMENT][RUN][SAVE-SETTLEMENT] settlementId=" + saved.getId()
                + ", sellerDealerProfileId=" + candidate.getSellerDealerProfile().getId()
                + ", period=" + candidate.getPeriodStartDate() + " ~ " + candidate.getPeriodEndDate()
                + ", basis=" + candidate.getBasis()
                + ", cycle=" + candidate.getCycle()
                + ", commissionRate=" + candidate.getCommissionRate());

            for (SettlementOrderSummarySourceDto orderSummary : candidate.getOrderSummaries()) {
                long orderCommissionAmount = calculateCommissionAmount(
                    orderSummary.getDealerAmount(),
                    candidate.getCommissionRate()
                );
                long orderSettlementAmount = orderSummary.getDealerAmount() - orderCommissionAmount;

                DealerSettlementOrder settlementOrder = DealerSettlementOrder.builder()
                    .settlement(saved)
                    .order(em.getReference(Order.class, orderSummary.getOrderId()))
                    .orderIdSnapshot(orderSummary.getOrderId())
                    .orderNoSnapshot(orderSummary.getOrderNo())
                    .ordererNameSnapshot(orderSummary.getOrdererName())
                    .basisDateSnapshot(orderSummary.getBasisDate())
                    .inclusionStatus(SettlementOrderInclusionStatus.NORMAL)
                    .dealerItemAmount(orderSummary.getDealerAmount())
                    .commissionAmount(orderCommissionAmount)
                    .settlementAmount(orderSettlementAmount)
                    .dealerItemCount(orderSummary.getDealerItemCount().intValue())
                    .memo(null)
                    .build();

                settlementOrderRepository.save(settlementOrder);

                debug("[SETTLEMENT][RUN][SAVE-ORDER] settlementId=" + saved.getId()
                    + ", orderId=" + orderSummary.getOrderId()
                    + ", orderNo=" + orderSummary.getOrderNo()
                    + ", basisDate=" + orderSummary.getBasisDate()
                    + ", inclusionStatus=" + SettlementOrderInclusionStatus.NORMAL
                    + ", dealerAmount=" + orderSummary.getDealerAmount()
                    + ", commissionAmount=" + orderCommissionAmount
                    + ", settlementAmount=" + orderSettlementAmount
                    + ", dealerItemCount=" + orderSummary.getDealerItemCount());
            }

            createdRows.add(
                SettlementExecutePreviewRowDto.builder()
                    .sellerDealerProfileId(candidate.getSellerDealerProfile().getId())
                    .memberUsername(candidate.getMemberUsername())
                    .memberName(candidate.getMemberName())
                    .companyName(candidate.getCompanyName())
                    .shopName(candidate.getShopName())
                    .cycle(candidate.getCycle())
                    .basis(candidate.getBasis())
                    .periodStartDate(candidate.getPeriodStartDate())
                    .periodEndDate(candidate.getPeriodEndDate())
                    .orderCount(candidate.getOrderCount())
                    .itemCount(candidate.getItemCount())
                    .grossAmount(candidate.getGrossAmount())
                    .commissionAmount(candidate.getCommissionAmount())
                    .settlementAmount(candidate.getSettlementAmount())
                    .build()
            );
            createdCount++;
        }

        batch.setCreatedSettlementCount(createdCount);
        batch.setStatus(SettlementBatchStatus.COMPLETED);
        batch.setFinishedAt(LocalDateTime.now());
        batch.setMessage("정산 실행 완료");
        settlementBatchRepository.save(batch);

        debug("[SETTLEMENT][RUN] batchId=" + batch.getId() + ", createdCount=" + createdCount);
        debug("[SETTLEMENT][RUN] END");
        debug("==================================================");

        return SettlementExecuteResultResponse.builder()
            .batchId(batch.getId())
            .createdCount(createdCount)
            .createdItems(createdRows)
            .build();
    }

    /**
     * 핵심 규칙
     * 1. 검색 날짜에 해당하는 오더를 조회한다. fromDate 가 없으면 전체 기간 조회다.
     * 2. 전체 기간 조회 시 셀러별 정산 시작일은 SellerDealerProfile.dealStartDate 이다.
     * 3. 중복 정산 판단은 DealerSettlement / DealerSettlementOrder 로만 한다.
     * 4. PolicyHistory 는 지급완료 판단 기준이 아니라, 날짜별 정책 변경 반영과 참조 스냅샷용이다.
     */
    private List<Candidate> buildCandidates(SettlementExecuteSearchRequest request, ExecutionCondition condition) {
        LocalDateTime fromDateTime = condition.getEffectiveFromDate() != null
            ? condition.getEffectiveFromDate().atStartOfDay()
            : null;

        LocalDateTime toDateTimeExclusive = condition.getEffectiveToDate().plusDays(1).atStartOfDay();

        debug("[SETTLEMENT][BUILD] fromDateTime=" + fromDateTime
            + ", toDateTimeExclusive=" + toDateTimeExclusive);

        List<Long> sellerDealerProfileIds = settlementOrderSourceQueryRepository.findSellerDealerProfileIdsHavingDealerOrders(
            fromDateTime,
            toDateTimeExclusive,
            request.getBases(),
            normalizeKeyword(request.getKeyword())
        );

        debug("[SETTLEMENT][BUILD] sellerDealerProfileIds=" + sellerDealerProfileIds);

        if (sellerDealerProfileIds == null || sellerDealerProfileIds.isEmpty()) {
            debug("[SETTLEMENT][BUILD] no sellerDealerProfileIds -> return []");
            return List.of();
        }

        Map<Long, DealerSettlementPolicy> currentPolicyMap = policyRepository.findAllForExecutionBySellerDealerProfileIds(
                sellerDealerProfileIds
            ).stream()
            .filter(Objects::nonNull)
            .filter(policy -> policy.getSellerDealerProfile() != null && policy.getSellerDealerProfile().getId() != null)
            .collect(Collectors.toMap(
                policy -> policy.getSellerDealerProfile().getId(),
                policy -> policy,
                (left, right) -> left,
                LinkedHashMap::new
            ));

        Map<Long, List<DealerSettlementPolicyHistory>> historiesBySellerId = policyHistoryRepository
            .findAllForExecutionBySellerDealerProfileIds(sellerDealerProfileIds, null, condition.getEffectiveToDate())
            .stream()
            .filter(Objects::nonNull)
            .filter(history -> history.getSellerDealerProfile() != null && history.getSellerDealerProfile().getId() != null)
            .sorted(
                Comparator.comparing(DealerSettlementPolicyHistory::getApplyStartDate, Comparator.nullsLast(LocalDate::compareTo))
                    .thenComparing(DealerSettlementPolicyHistory::getId)
            )
            .collect(Collectors.groupingBy(
                history -> history.getSellerDealerProfile().getId(),
                LinkedHashMap::new,
                Collectors.toList()
            ));

        debug("[SETTLEMENT][BUILD] currentPolicyMap.keys=" + currentPolicyMap.keySet());
        debug("[SETTLEMENT][BUILD] historiesBySellerId.keys=" + historiesBySellerId.keySet());

        List<Candidate> candidates = new ArrayList<>();

        for (Long sellerDealerProfileId : sellerDealerProfileIds) {
            DealerSettlementPolicy currentPolicy = currentPolicyMap.get(sellerDealerProfileId);
            List<DealerSettlementPolicyHistory> histories = historiesBySellerId.getOrDefault(
                sellerDealerProfileId,
                List.of()
            );

            debug("[SETTLEMENT][SELLER] sellerDealerProfileId=" + sellerDealerProfileId
                + ", hasCurrentPolicy=" + (currentPolicy != null)
                + ", historyCount=" + histories.size());

            if (currentPolicy == null || currentPolicy.getSellerDealerProfile() == null) {
                debug("[SETTLEMENT][SELLER] currentPolicy 또는 sellerDealerProfile 없음 -> skip sellerDealerProfileId=" + sellerDealerProfileId);
                continue;
            }

            addCandidatesForSeller(
                request,
                condition,
                currentPolicy,
                histories,
                candidates
            );
        }

        candidates.sort(
            Comparator.comparing(Candidate::getMemberUsername, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(Candidate::getPeriodStartDate)
                .thenComparing(Candidate::getPeriodEndDate)
                .thenComparing(candidate -> candidate.getBasis().name())
        );

        return candidates;
    }

    private void addCandidatesForSeller(
        SettlementExecuteSearchRequest request,
        ExecutionCondition condition,
        DealerSettlementPolicy currentPolicy,
        List<DealerSettlementPolicyHistory> histories,
        List<Candidate> candidates
    ) {
        SellerDealerProfile sellerDealerProfile = currentPolicy.getSellerDealerProfile();
        Long sellerDealerProfileId = sellerDealerProfile.getId();

        if (sellerDealerProfileId == null) {
            debug("[SETTLEMENT][SELLER-CANDIDATE] sellerDealerProfileId null -> skip");
            return;
        }

        LocalDate effectiveStart = resolveSellerSettlementStartDate(condition, sellerDealerProfile);
        LocalDate effectiveEnd = condition.getEffectiveToDate();

        debug("[SETTLEMENT][SELLER-CANDIDATE] sellerDealerProfileId=" + sellerDealerProfileId
            + ", effectiveStart=" + effectiveStart
            + ", effectiveEnd=" + effectiveEnd
            + ", dealStartDate=" + sellerDealerProfile.getDealStartDate());

        if (effectiveStart == null || effectiveEnd.isBefore(effectiveStart)) {
            debug("[SETTLEMENT][SELLER-CANDIDATE] invalid effective range -> skip sellerDealerProfileId=" + sellerDealerProfileId);
            return;
        }

        List<DealerSettlement> overlappingSettlements = settlementRepository.findOverlappingSettlementsAnyBasis(
            sellerDealerProfileId,
            effectiveStart,
            effectiveEnd
        );

        debug("[SETTLEMENT][SELLER-CANDIDATE] sellerDealerProfileId=" + sellerDealerProfileId
            + ", overlappingSettlements.size=" + (overlappingSettlements != null ? overlappingSettlements.size() : 0));

        List<PolicyWindow> policyWindows = resolvePolicyWindows(
            request,
            currentPolicy,
            histories,
            effectiveStart,
            effectiveEnd
        );

        debug("[SETTLEMENT][SELLER-CANDIDATE] sellerDealerProfileId=" + sellerDealerProfileId
            + ", policyWindows.size=" + policyWindows.size());

        for (PolicyWindow policyWindow : policyWindows) {
            addCandidatesForPolicyWindow(
                sellerDealerProfile,
                currentPolicy,
                overlappingSettlements,
                policyWindow,
                effectiveEnd,
                candidates
            );
        }
    }

    private void addCandidatesForPolicyWindow(
        SellerDealerProfile sellerDealerProfile,
        DealerSettlementPolicy currentPolicy,
        List<DealerSettlement> overlappingSettlements,
        PolicyWindow policyWindow,
        LocalDate effectiveEnd,
        List<Candidate> candidates
    ) {
        Long sellerDealerProfileId = sellerDealerProfile.getId();

        List<SettlementOrderSummarySourceDto> orderSummaries = settlementOrderSourceQueryRepository.findDealerOrderSummaries(
            sellerDealerProfileId,
            policyWindow.getBasis(),
            policyWindow.getWindowStartDate().atStartOfDay(),
            policyWindow.getWindowEndDate().plusDays(1).atStartOfDay()
        );

        debug("[SETTLEMENT][POLICY-WINDOW] sellerDealerProfileId=" + sellerDealerProfileId
            + ", window=" + policyWindow.getWindowStartDate() + " ~ " + policyWindow.getWindowEndDate()
            + ", cycle=" + policyWindow.getCycle()
            + ", basis=" + policyWindow.getBasis()
            + ", commissionRate=" + policyWindow.getCommissionRate()
            + ", orderSummaries.size=" + (orderSummaries != null ? orderSummaries.size() : 0));

        if (orderSummaries == null || orderSummaries.isEmpty()) {
            return;
        }

        Map<PeriodKey, List<SettlementOrderSummarySourceDto>> ordersByClosedPeriod = new LinkedHashMap<>();

        for (SettlementOrderSummarySourceDto orderSummary : orderSummaries) {
            if (orderSummary == null || orderSummary.getOrderId() == null || orderSummary.getBasisDate() == null) {
                debug("[SETTLEMENT][ORDER-SKIP] null summary data sellerDealerProfileId=" + sellerDealerProfileId);
                continue;
            }

            LocalDate basisDate = orderSummary.getBasisDate().toLocalDate();

            if (basisDate.isBefore(policyWindow.getWindowStartDate()) || basisDate.isAfter(policyWindow.getWindowEndDate())) {
                debug("[SETTLEMENT][ORDER-SKIP] out of policy window sellerDealerProfileId=" + sellerDealerProfileId
                    + ", orderId=" + orderSummary.getOrderId()
                    + ", basisDate=" + basisDate);
                continue;
            }

            PeriodKey rawPeriod = resolveCyclePeriod(policyWindow.getCycle(), basisDate);

            debug("[SETTLEMENT][ORDER-PERIOD] sellerDealerProfileId=" + sellerDealerProfileId
                + ", orderId=" + orderSummary.getOrderId()
                + ", basisDate=" + basisDate
                + ", rawPeriod=" + rawPeriod.getStartDate() + " ~ " + rawPeriod.getEndDate()
                + ", policyWindow=" + policyWindow.getWindowStartDate() + " ~ " + policyWindow.getWindowEndDate());

            if (rawPeriod.getEndDate().isAfter(effectiveEnd)) {
                debug("[SETTLEMENT][ORDER-SKIP] raw period not closed sellerDealerProfileId=" + sellerDealerProfileId
                    + ", orderId=" + orderSummary.getOrderId()
                    + ", rawPeriodEnd=" + rawPeriod.getEndDate()
                    + ", effectiveEnd=" + effectiveEnd);
                continue;
            }

            LocalDate periodStart = max(policyWindow.getWindowStartDate(), rawPeriod.getStartDate());
            LocalDate periodEnd = min(policyWindow.getWindowEndDate(), rawPeriod.getEndDate());

            if (periodEnd.isBefore(periodStart)) {
                debug("[SETTLEMENT][ORDER-SKIP] clipped period invalid sellerDealerProfileId=" + sellerDealerProfileId
                    + ", orderId=" + orderSummary.getOrderId()
                    + ", clippedPeriod=" + periodStart + " ~ " + periodEnd);
                continue;
            }

            PeriodKey closedPeriod = new PeriodKey(periodStart, periodEnd);
            ordersByClosedPeriod.computeIfAbsent(closedPeriod, key -> new ArrayList<>()).add(orderSummary);
        }

        debug("[SETTLEMENT][POLICY-WINDOW] sellerDealerProfileId=" + sellerDealerProfileId
            + ", closedPeriodCount=" + ordersByClosedPeriod.size());

        for (Map.Entry<PeriodKey, List<SettlementOrderSummarySourceDto>> entry : ordersByClosedPeriod.entrySet()) {
            PeriodKey closedPeriod = entry.getKey();
            List<SettlementOrderSummarySourceDto> periodOrders = entry.getValue();

            if (periodOrders == null || periodOrders.isEmpty()) {
                continue;
            }

            List<PeriodKey> unpaidSegments = subtractSettledPeriods(
                closedPeriod.getStartDate(),
                closedPeriod.getEndDate(),
                overlappingSettlements
            );

            debug("[SETTLEMENT][UNPAID-SEGMENTS] sellerDealerProfileId=" + sellerDealerProfileId
                + ", closedPeriod=" + closedPeriod.getStartDate() + " ~ " + closedPeriod.getEndDate()
                + ", unpaidSegments.size=" + unpaidSegments.size());

            for (PeriodKey unpaidSegment : unpaidSegments) {
                List<SettlementOrderSummarySourceDto> segmentOrders = periodOrders.stream()
                    .filter(summary -> {
                        LocalDate basisDate = summary.getBasisDate().toLocalDate();
                        return !basisDate.isBefore(unpaidSegment.getStartDate())
                            && !basisDate.isAfter(unpaidSegment.getEndDate());
                    })
                    .collect(Collectors.toList());

                debug("[SETTLEMENT][SEGMENT-ORDERS] sellerDealerProfileId=" + sellerDealerProfileId
                    + ", unpaidSegment=" + unpaidSegment.getStartDate() + " ~ " + unpaidSegment.getEndDate()
                    + ", segmentOrders.size=" + segmentOrders.size());

                if (segmentOrders.isEmpty()) {
                    continue;
                }

                List<Long> orderIds = segmentOrders.stream()
                    .map(SettlementOrderSummarySourceDto::getOrderId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

                Set<Long> alreadySettledOrderIds = orderIds.isEmpty()
                    ? Set.of()
                    : new HashSet<>(settlementOrderRepository.findAlreadySettledOrderIds(sellerDealerProfileId, orderIds));

                debug("[SETTLEMENT][ALREADY-SETTLED-ORDER-IDS] sellerDealerProfileId=" + sellerDealerProfileId
                    + ", orderIds=" + orderIds
                    + ", alreadySettledOrderIds=" + alreadySettledOrderIds);

                List<SettlementOrderSummarySourceDto> unsettledOrders = segmentOrders.stream()
                    .filter(summary -> !alreadySettledOrderIds.contains(summary.getOrderId()))
                    .collect(Collectors.toList());

                debug("[SETTLEMENT][UNSETTLED-ORDERS] sellerDealerProfileId=" + sellerDealerProfileId
                    + ", unpaidSegment=" + unpaidSegment.getStartDate() + " ~ " + unpaidSegment.getEndDate()
                    + ", unsettledOrders.size=" + unsettledOrders.size());

                if (unsettledOrders.isEmpty()) {
                    continue;
                }

                long grossAmount = unsettledOrders.stream()
                    .mapToLong(SettlementOrderSummarySourceDto::getDealerAmount)
                    .sum();

                if (grossAmount <= 0L) {
                    debug("[SETTLEMENT][UNSETTLED-ORDERS] grossAmount <= 0 -> skip");
                    continue;
                }

                int itemCount = unsettledOrders.stream()
                    .mapToInt(summary -> summary.getDealerItemCount().intValue())
                    .sum();

                int orderCount = unsettledOrders.size();

                long commissionAmount = calculateCommissionAmount(unsettledOrders, policyWindow.getCommissionRate());
                long settlementAmount = grossAmount - commissionAmount;

                debug("[SETTLEMENT][CANDIDATE-CREATED] sellerDealerProfileId=" + sellerDealerProfileId
                    + ", cycle=" + policyWindow.getCycle()
                    + ", basis=" + policyWindow.getBasis()
                    + ", period=" + unpaidSegment.getStartDate() + " ~ " + unpaidSegment.getEndDate()
                    + ", grossAmount=" + grossAmount
                    + ", commissionAmount=" + commissionAmount
                    + ", settlementAmount=" + settlementAmount
                    + ", commissionRate=" + policyWindow.getCommissionRate()
                    + ", referenceHistoryId=" + (policyWindow.getReferenceHistory() != null ? policyWindow.getReferenceHistory().getId() : null));

                candidates.add(
                    Candidate.builder()
                        .sellerDealerProfile(sellerDealerProfile)
                        .history(policyWindow.getReferenceHistory())
                        .currentPolicy(currentPolicy)
                        .cycle(policyWindow.getCycle())
                        .basis(policyWindow.getBasis())
                        .commissionRate(policyWindow.getCommissionRate())
                        .sellerMemberIdSnapshot(policyWindow.getSellerMemberId())
                        .memberUsername(policyWindow.getMemberUsername())
                        .memberName(policyWindow.getMemberName())
                        .memberEmail(policyWindow.getMemberEmail())
                        .memberMobile(policyWindow.getMemberMobile())
                        .companyName(policyWindow.getCompanyName())
                        .shopName(policyWindow.getShopName())
                        .supplierCode(policyWindow.getSupplierCode())
                        .periodStartDate(unpaidSegment.getStartDate())
                        .periodEndDate(unpaidSegment.getEndDate())
                        .orderSummaries(unsettledOrders)
                        .grossAmount(grossAmount)
                        .commissionAmount(commissionAmount)
                        .settlementAmount(settlementAmount)
                        .orderCount(orderCount)
                        .itemCount(itemCount)
                        .build()
                );
            }
        }
    }

    private List<PolicyWindow> resolvePolicyWindows(
        SettlementExecuteSearchRequest request,
        DealerSettlementPolicy currentPolicy,
        List<DealerSettlementPolicyHistory> histories,
        LocalDate rangeStart,
        LocalDate rangeEnd
    ) {
        if (currentPolicy == null || currentPolicy.getSellerDealerProfile() == null) {
            return List.of();
        }

        TreeSet<LocalDate> boundaries = new TreeSet<>();
        boundaries.add(rangeStart);
        boundaries.add(rangeEnd.plusDays(1));

        if (histories != null) {
            for (DealerSettlementPolicyHistory history : histories) {
                if (history == null || history.getApplyStartDate() == null) {
                    continue;
                }

                LocalDate historyStart = max(rangeStart, history.getApplyStartDate());
                LocalDate rawHistoryEnd = history.getApplyEndDate() != null ? history.getApplyEndDate() : rangeEnd;
                LocalDate historyEnd = min(rangeEnd, rawHistoryEnd);

                if (historyEnd.isBefore(historyStart)) {
                    continue;
                }

                boundaries.add(historyStart);
                boundaries.add(historyEnd.plusDays(1));
            }
        }

        List<LocalDate> boundaryList = new ArrayList<>(boundaries);
        List<PolicyWindow> windows = new ArrayList<>();

        for (int i = 0; i < boundaryList.size() - 1; i++) {
            LocalDate windowStart = boundaryList.get(i);
            LocalDate windowEnd = boundaryList.get(i + 1).minusDays(1);

            if (windowEnd.isBefore(windowStart)) {
                continue;
            }

            DealerSettlementPolicyHistory effectiveHistory = findEffectiveHistory(histories, windowStart);

            SettlementCycle cycle = effectiveHistory != null && effectiveHistory.getCycle() != null
                ? effectiveHistory.getCycle()
                : currentPolicy.getCycle();

            SettlementBasis basis = effectiveHistory != null && effectiveHistory.getBasis() != null
                ? effectiveHistory.getBasis()
                : currentPolicy.getBasis();

            BigDecimal commissionRate = effectiveHistory != null && effectiveHistory.getCommissionRate() != null
                ? effectiveHistory.getCommissionRate()
                : currentPolicy.getCommissionRate();

            if (cycle == null || basis == null) {
                debug("[SETTLEMENT][POLICY-WINDOW-SKIP] cycle or basis null, sellerDealerProfileId="
                    + currentPolicy.getSellerDealerProfile().getId()
                    + ", window=" + windowStart + " ~ " + windowEnd);
                continue;
            }

            if (!matchesRequestedFilters(cycle, basis, request)) {
                debug("[SETTLEMENT][POLICY-WINDOW-SKIP] requested filter mismatch, sellerDealerProfileId="
                    + currentPolicy.getSellerDealerProfile().getId()
                    + ", window=" + windowStart + " ~ " + windowEnd
                    + ", cycle=" + cycle
                    + ", basis=" + basis);
                continue;
            }

            windows.add(buildPolicyWindow(currentPolicy, effectiveHistory, windowStart, windowEnd, cycle, basis, commissionRate));
        }

        return mergeAdjacentPolicyWindows(windows);
    }

    private PolicyWindow buildPolicyWindow(
        DealerSettlementPolicy currentPolicy,
        DealerSettlementPolicyHistory effectiveHistory,
        LocalDate windowStart,
        LocalDate windowEnd,
        SettlementCycle cycle,
        SettlementBasis basis,
        BigDecimal commissionRate
    ) {
        SellerDealerProfile seller = currentPolicy.getSellerDealerProfile();
        Member sellerMember = seller.getMember();

        return PolicyWindow.builder()
            .windowStartDate(windowStart)
            .windowEndDate(windowEnd)
            .referenceHistory(effectiveHistory)
            .cycle(cycle)
            .basis(basis)
            .commissionRate(commissionRate != null ? commissionRate : BigDecimal.ZERO)
            .sellerMemberId(
                effectiveHistory != null && effectiveHistory.getSellerMemberIdSnapshot() != null
                    ? effectiveHistory.getSellerMemberIdSnapshot()
                    : sellerMember != null ? sellerMember.getId() : null
            )
            .memberUsername(
                effectiveHistory != null && StringUtils.hasText(effectiveHistory.getMemberUsernameSnapshot())
                    ? effectiveHistory.getMemberUsernameSnapshot()
                    : sellerMember != null ? sellerMember.getUsername() : null
            )
            .memberName(
                effectiveHistory != null && StringUtils.hasText(effectiveHistory.getMemberNameSnapshot())
                    ? effectiveHistory.getMemberNameSnapshot()
                    : sellerMember != null ? sellerMember.getName() : null
            )
            .memberEmail(
                effectiveHistory != null && StringUtils.hasText(effectiveHistory.getMemberEmailSnapshot())
                    ? effectiveHistory.getMemberEmailSnapshot()
                    : sellerMember != null ? sellerMember.getEmail() : null
            )
            .memberMobile(
                effectiveHistory != null && StringUtils.hasText(effectiveHistory.getMemberMobileSnapshot())
                    ? effectiveHistory.getMemberMobileSnapshot()
                    : sellerMember != null ? sellerMember.getMobile() : null
            )
            .companyName(
                effectiveHistory != null && StringUtils.hasText(effectiveHistory.getCompanyNameSnapshot())
                    ? effectiveHistory.getCompanyNameSnapshot()
                    : seller.getCompanyProfile() != null ? seller.getCompanyProfile().getCompanyName() : null
            )
            .shopName(
                effectiveHistory != null && StringUtils.hasText(effectiveHistory.getShopNameSnapshot())
                    ? effectiveHistory.getShopNameSnapshot()
                    : seller.getShopName()
            )
            .supplierCode(
                effectiveHistory != null && StringUtils.hasText(effectiveHistory.getSupplierCodeSnapshot())
                    ? effectiveHistory.getSupplierCodeSnapshot()
                    : seller.getSupplierCode()
            )
            .build();
    }

    private List<PolicyWindow> mergeAdjacentPolicyWindows(List<PolicyWindow> windows) {
        if (windows == null || windows.isEmpty()) {
            return List.of();
        }

        List<PolicyWindow> sorted = windows.stream()
            .sorted(
                Comparator.comparing(PolicyWindow::getWindowStartDate)
                    .thenComparing(PolicyWindow::getWindowEndDate)
                    .thenComparing(window -> window.getBasis().name())
                    .thenComparing(window -> window.getCycle().name())
            )
            .collect(Collectors.toList());

        List<PolicyWindow> merged = new ArrayList<>();
        PolicyWindow current = sorted.get(0);

        for (int i = 1; i < sorted.size(); i++) {
            PolicyWindow next = sorted.get(i);

            if (canMergePolicyWindow(current, next)) {
                current = current.toBuilder()
                    .windowEndDate(next.getWindowEndDate())
                    .build();
            } else {
                merged.add(current);
                current = next;
            }
        }

        merged.add(current);
        return merged;
    }

    private boolean canMergePolicyWindow(PolicyWindow left, PolicyWindow right) {
        if (left == null || right == null) {
            return false;
        }

        Long leftHistoryId = left.getReferenceHistory() != null ? left.getReferenceHistory().getId() : null;
        Long rightHistoryId = right.getReferenceHistory() != null ? right.getReferenceHistory().getId() : null;

        return left.getWindowEndDate().plusDays(1).isEqual(right.getWindowStartDate())
            && left.getCycle() == right.getCycle()
            && left.getBasis() == right.getBasis()
            && Objects.equals(left.getCommissionRate(), right.getCommissionRate())
            && Objects.equals(leftHistoryId, rightHistoryId)
            && Objects.equals(left.getSellerMemberId(), right.getSellerMemberId())
            && Objects.equals(left.getMemberUsername(), right.getMemberUsername())
            && Objects.equals(left.getMemberName(), right.getMemberName())
            && Objects.equals(left.getMemberEmail(), right.getMemberEmail())
            && Objects.equals(left.getMemberMobile(), right.getMemberMobile())
            && Objects.equals(left.getCompanyName(), right.getCompanyName())
            && Objects.equals(left.getShopName(), right.getShopName())
            && Objects.equals(left.getSupplierCode(), right.getSupplierCode());
    }

    private LocalDate resolveSellerSettlementStartDate(
        ExecutionCondition condition,
        SellerDealerProfile sellerDealerProfile
    ) {
        if (condition.getEffectiveFromDate() != null) {
            return condition.getEffectiveFromDate();
        }

        if (sellerDealerProfile == null) {
            return null;
        }

        return sellerDealerProfile.getDealStartDate();
    }

    private DealerSettlementPolicyHistory findEffectiveHistory(
        List<DealerSettlementPolicyHistory> histories,
        LocalDate targetDate
    ) {
        if (histories == null || histories.isEmpty() || targetDate == null) {
            return null;
        }

        return histories.stream()
            .filter(Objects::nonNull)
            .filter(history -> history.getApplyStartDate() != null)
            .filter(history -> !history.getApplyStartDate().isAfter(targetDate))
            .filter(history -> history.getApplyEndDate() == null || !history.getApplyEndDate().isBefore(targetDate))
            .max(
                Comparator.comparing(DealerSettlementPolicyHistory::getApplyStartDate)
                    .thenComparing(DealerSettlementPolicyHistory::getId)
            )
            .orElse(null);
    }

    private long calculateCommissionAmount(
        List<SettlementOrderSummarySourceDto> unsettledOrders,
        BigDecimal commissionRate
    ) {
        long totalCommissionAmount = 0L;

        for (SettlementOrderSummarySourceDto orderSummary : unsettledOrders) {
            totalCommissionAmount += calculateCommissionAmount(orderSummary.getDealerAmount(), commissionRate);
        }

        return totalCommissionAmount;
    }

    private List<PeriodKey> subtractSettledPeriods(
        LocalDate targetStart,
        LocalDate targetEnd,
        List<DealerSettlement> overlappingSettlements
    ) {
        if (targetStart == null || targetEnd == null || targetEnd.isBefore(targetStart)) {
            return List.of();
        }

        if (overlappingSettlements == null || overlappingSettlements.isEmpty()) {
            return List.of(new PeriodKey(targetStart, targetEnd));
        }

        List<PeriodKey> occupied = overlappingSettlements.stream()
            .filter(Objects::nonNull)
            .filter(settlement -> settlement.getPeriodStartDate() != null && settlement.getPeriodEndDate() != null)
            .filter(settlement ->
                !settlement.getPeriodEndDate().isBefore(targetStart)
                    && !settlement.getPeriodStartDate().isAfter(targetEnd)
            )
            .map(settlement -> new PeriodKey(
                max(targetStart, settlement.getPeriodStartDate()),
                min(targetEnd, settlement.getPeriodEndDate())
            ))
            .sorted(
                Comparator.comparing(PeriodKey::getStartDate)
                    .thenComparing(PeriodKey::getEndDate)
            )
            .collect(Collectors.toList());

        if (occupied.isEmpty()) {
            return List.of(new PeriodKey(targetStart, targetEnd));
        }

        List<PeriodKey> merged = new ArrayList<>();
        PeriodKey current = occupied.get(0);

        for (int i = 1; i < occupied.size(); i++) {
            PeriodKey next = occupied.get(i);

            if (!next.getStartDate().isAfter(current.getEndDate().plusDays(1))) {
                current = new PeriodKey(
                    current.getStartDate(),
                    max(current.getEndDate(), next.getEndDate())
                );
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);

        List<PeriodKey> result = new ArrayList<>();
        LocalDate cursor = targetStart;

        for (PeriodKey used : merged) {
            if (cursor.isBefore(used.getStartDate())) {
                result.add(new PeriodKey(cursor, used.getStartDate().minusDays(1)));
            }

            if (!used.getEndDate().isBefore(cursor)) {
                cursor = used.getEndDate().plusDays(1);
            }

            if (cursor.isAfter(targetEnd)) {
                break;
            }
        }

        if (!cursor.isAfter(targetEnd)) {
            result.add(new PeriodKey(cursor, targetEnd));
        }

        return result;
    }

    private boolean matchesRequestedFilters(
        SettlementCycle cycle,
        SettlementBasis basis,
        SettlementExecuteSearchRequest request
    ) {
        if (request.getCycles() != null
            && !request.getCycles().isEmpty()
            && !request.getCycles().contains(cycle)) {
            return false;
        }

        if (request.getBases() != null
            && !request.getBases().isEmpty()
            && !request.getBases().contains(basis)) {
            return false;
        }

        return true;
    }

    private DealerSettlementPolicyHistory resolveReferenceHistory(Candidate candidate, Member admin) {
        if (candidate.getHistory() != null) {
            return candidate.getHistory();
        }

        if (candidate.getCurrentPolicy() == null) {
            throw new IllegalStateException("정산 저장에 필요한 정책 정보가 없습니다.");
        }

        policyHistoryService.syncHistoryOnPolicySave(
            candidate.getSellerDealerProfile(),
            candidate.getCurrentPolicy(),
            admin != null ? admin.getId() : null
        );

        List<DealerSettlementPolicyHistory> effectiveHistories = policyHistoryRepository.findEffectiveHistoriesAtDate(
            candidate.getSellerDealerProfile().getId(),
            candidate.getPeriodEndDate()
        );

        if (effectiveHistories == null || effectiveHistories.isEmpty()) {
            throw new IllegalStateException("정산 저장에 필요한 정책 히스토리를 찾을 수 없습니다.");
        }

        return effectiveHistories.get(0);
    }

    private SettlementExecutePreviewResponse toPreviewResponse(List<Candidate> candidates) {
        List<SettlementExecutePreviewRowDto> items = candidates.stream()
            .map(candidate -> SettlementExecutePreviewRowDto.builder()
                .sellerDealerProfileId(candidate.getSellerDealerProfile().getId())
                .memberUsername(candidate.getMemberUsername())
                .memberName(candidate.getMemberName())
                .companyName(candidate.getCompanyName())
                .shopName(candidate.getShopName())
                .cycle(candidate.getCycle())
                .basis(candidate.getBasis())
                .periodStartDate(candidate.getPeriodStartDate())
                .periodEndDate(candidate.getPeriodEndDate())
                .orderCount(candidate.getOrderCount())
                .itemCount(candidate.getItemCount())
                .grossAmount(candidate.getGrossAmount())
                .commissionAmount(candidate.getCommissionAmount())
                .settlementAmount(candidate.getSettlementAmount())
                .build())
            .collect(Collectors.toList());

        long totalGross = candidates.stream().mapToLong(Candidate::getGrossAmount).sum();
        long totalCommission = candidates.stream().mapToLong(Candidate::getCommissionAmount).sum();
        long totalSettlement = candidates.stream().mapToLong(Candidate::getSettlementAmount).sum();

        return SettlementExecutePreviewResponse.builder()
            .items(items)
            .count(items.size())
            .totalGrossAmount(totalGross)
            .totalCommissionAmount(totalCommission)
            .totalSettlementAmount(totalSettlement)
            .build();
    }

    /**
     * 정산주기 period 계산
     * 예)
     * - DAY_25 : 1/26~2/25, 2/26~3/25
     * - MONTH_END : 3/1~3/31
     */
    private PeriodKey resolveCyclePeriod(SettlementCycle cycle, LocalDate basisDate) {
        return switch (cycle) {
            case MONTH_END -> new PeriodKey(
                basisDate.withDayOfMonth(1),
                basisDate.withDayOfMonth(basisDate.lengthOfMonth())
            );
            case DAY_1 -> resolveClosingDayPeriod(basisDate, 1);
            case DAY_5 -> resolveClosingDayPeriod(basisDate, 5);
            case DAY_10 -> resolveClosingDayPeriod(basisDate, 10);
            case DAY_15 -> resolveClosingDayPeriod(basisDate, 15);
            case DAY_20 -> resolveClosingDayPeriod(basisDate, 20);
            case DAY_25 -> resolveClosingDayPeriod(basisDate, 25);
        };
    }

    private PeriodKey resolveClosingDayPeriod(LocalDate basisDate, int closingDay) {
        if (basisDate.getDayOfMonth() <= closingDay) {
            LocalDate endDate = basisDate.withDayOfMonth(closingDay);
            LocalDate startDate = basisDate.minusMonths(1).withDayOfMonth(closingDay).plusDays(1);
            return new PeriodKey(startDate, endDate);
        }

        LocalDate startDate = basisDate.withDayOfMonth(closingDay).plusDays(1);
        LocalDate endDate = basisDate.plusMonths(1).withDayOfMonth(closingDay);
        return new PeriodKey(startDate, endDate);
    }

    private long calculateCommissionAmount(long grossAmount, BigDecimal commissionRate) {
        BigDecimal rate = commissionRate == null ? BigDecimal.ZERO : commissionRate;
        return BigDecimal.valueOf(grossAmount)
            .multiply(rate)
            .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
            .longValue();
    }

    private ExecutionCondition normalizeAndValidate(SettlementExecuteSearchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("정산 실행 요청값이 없습니다.");
        }

        LocalDate effectiveFromDate = request.getFromDate();
        LocalDate effectiveToDate = request.getToDate() != null ? request.getToDate() : LocalDate.now();

        if (effectiveFromDate != null && effectiveFromDate.isAfter(effectiveToDate)) {
            throw new IllegalArgumentException("조회 시작일은 종료일보다 늦을 수 없습니다.");
        }

        return new ExecutionCondition(effectiveFromDate, effectiveToDate);
    }

    private LocalDate resolveRequestedFromDate(ExecutionCondition condition, List<Candidate> candidates) {
        if (condition.getEffectiveFromDate() != null) {
            return condition.getEffectiveFromDate();
        }

        return candidates.stream()
            .map(Candidate::getPeriodStartDate)
            .min(LocalDate::compareTo)
            .orElse(condition.getEffectiveToDate());
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private String toCsv(List<?> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    private LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private Member resolveAdmin(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        if (authentication.getPrincipal() instanceof PrincipalDetails pd) {
            return pd.getMember();
        }

        return null;
    }

    private void debug(String message) {
        System.out.println(message);
    }

    @Getter
    @RequiredArgsConstructor
    private static class ExecutionCondition {
        private final LocalDate effectiveFromDate;
        private final LocalDate effectiveToDate;
    }

    @Getter
    @Builder
    private static class Candidate {
        private SellerDealerProfile sellerDealerProfile;
        private DealerSettlementPolicyHistory history;
        private DealerSettlementPolicy currentPolicy;
        private SettlementCycle cycle;
        private SettlementBasis basis;
        private BigDecimal commissionRate;
        private Long sellerMemberIdSnapshot;
        private String memberUsername;
        private String memberName;
        private String memberEmail;
        private String memberMobile;
        private String companyName;
        private String shopName;
        private String supplierCode;
        private LocalDate periodStartDate;
        private LocalDate periodEndDate;
        private List<SettlementOrderSummarySourceDto> orderSummaries;
        private long grossAmount;
        private long commissionAmount;
        private long settlementAmount;
        private int orderCount;
        private int itemCount;
    }

    @Getter
    @Builder(toBuilder = true)
    private static class PolicyWindow {
        private LocalDate windowStartDate;
        private LocalDate windowEndDate;
        private DealerSettlementPolicyHistory referenceHistory;
        private SettlementCycle cycle;
        private SettlementBasis basis;
        private BigDecimal commissionRate;
        private Long sellerMemberId;
        private String memberUsername;
        private String memberName;
        private String memberEmail;
        private String memberMobile;
        private String companyName;
        private String shopName;
        private String supplierCode;
    }

    @Getter
    @EqualsAndHashCode
    @RequiredArgsConstructor
    private static class PeriodKey {
        private final LocalDate startDate;
        private final LocalDate endDate;
    }
}