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
                + ", settlement=" + candidate.getSettlementAmount());
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
                + ", cycle=" + candidate.getCycle());

            for (SettlementOrderSummarySourceDto orderSummary : candidate.getOrderSummaries()) {
                DealerSettlementOrder settlementOrder = DealerSettlementOrder.builder()
                    .settlement(saved)
                    .order(em.getReference(Order.class, orderSummary.getOrderId()))
                    .orderIdSnapshot(orderSummary.getOrderId())
                    .orderNoSnapshot(orderSummary.getOrderNo())
                    .ordererNameSnapshot(orderSummary.getOrdererName())
                    .basisDateSnapshot(orderSummary.getBasisDate())
                    .dealerItemAmount(orderSummary.getDealerAmount())
                    .dealerItemCount(orderSummary.getDealerItemCount().intValue())
                    .build();

                settlementOrderRepository.save(settlementOrder);

                debug("[SETTLEMENT][RUN][SAVE-ORDER] settlementId=" + saved.getId()
                    + ", orderId=" + orderSummary.getOrderId()
                    + ", orderNo=" + orderSummary.getOrderNo()
                    + ", basisDate=" + orderSummary.getBasisDate()
                    + ", dealerAmount=" + orderSummary.getDealerAmount()
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
     * 핵심 흐름
     * 1. 주문이 있는 셀러를 먼저 찾음
     * 2. 현재 policy 의 cycle/basis 로 정산기간을 구성
     * 3. DealerSettlement 로 이미 생성된 기간 제거
     * 4. DealerSettlementOrder 로 이미 포함된 주문 제거
     * 5. PolicyHistory 는 날짜별 수수료 계산용으로만 사용
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

            ExecutionPolicy executionPolicy = resolveExecutionPolicy(currentPolicy, histories, condition.getEffectiveToDate());
            if (executionPolicy == null) {
                debug("[SETTLEMENT][SELLER] executionPolicy is null -> skip sellerDealerProfileId=" + sellerDealerProfileId);
                continue;
            }

            debug("[SETTLEMENT][SELLER] executionPolicy sellerDealerProfileId=" + sellerDealerProfileId
                + ", cycle=" + executionPolicy.getCycle()
                + ", basis=" + executionPolicy.getBasis()
                + ", memberUsername=" + executionPolicy.getMemberUsername());

            if (!matchesRequestedFilters(executionPolicy, request)) {
                debug("[SETTLEMENT][SELLER] requested filter mismatch -> skip sellerDealerProfileId=" + sellerDealerProfileId);
                continue;
            }

            addCandidatesForSeller(
                sellerDealerProfileId,
                currentPolicy,
                histories,
                executionPolicy,
                condition,
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
        Long sellerDealerProfileId,
        DealerSettlementPolicy currentPolicy,
        List<DealerSettlementPolicyHistory> histories,
        ExecutionPolicy executionPolicy,
        ExecutionCondition condition,
        List<Candidate> candidates
    ) {
        SellerDealerProfile sellerDealerProfile = resolveSellerDealerProfile(currentPolicy, histories);
        if (sellerDealerProfile == null || sellerDealerProfileId == null) {
            debug("[SETTLEMENT][SELLER-CANDIDATE] sellerDealerProfile null -> skip sellerDealerProfileId=" + sellerDealerProfileId);
            return;
        }

        List<SettlementOrderSummarySourceDto> allOrderSummaries = settlementOrderSourceQueryRepository.findDealerOrderSummaries(
            sellerDealerProfileId,
            executionPolicy.getBasis(),
            condition.getEffectiveFromDate() != null ? condition.getEffectiveFromDate().atStartOfDay() : null,
            condition.getEffectiveToDate().plusDays(1).atStartOfDay()
        );

        debug("[SETTLEMENT][SELLER-CANDIDATE] sellerDealerProfileId=" + sellerDealerProfileId
            + ", allOrderSummaries.size=" + (allOrderSummaries != null ? allOrderSummaries.size() : 0));

        if (allOrderSummaries == null || allOrderSummaries.isEmpty()) {
            debug("[SETTLEMENT][SELLER-CANDIDATE] allOrderSummaries empty -> skip sellerDealerProfileId=" + sellerDealerProfileId);
            return;
        }

        LocalDate effectiveStart = resolveSellerSettlementStartDate(condition, histories, allOrderSummaries);
        LocalDate effectiveEnd = condition.getEffectiveToDate();

        debug("[SETTLEMENT][SELLER-CANDIDATE] sellerDealerProfileId=" + sellerDealerProfileId
            + ", effectiveStart=" + effectiveStart
            + ", effectiveEnd=" + effectiveEnd);

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

        if (overlappingSettlements != null) {
            for (DealerSettlement ds : overlappingSettlements) {
                debug("[SETTLEMENT][OVERLAP] sellerDealerProfileId=" + sellerDealerProfileId
                    + ", settlementId=" + ds.getId()
                    + ", basis=" + ds.getSettlementBasis()
                    + ", period=" + ds.getPeriodStartDate() + " ~ " + ds.getPeriodEndDate());
            }
        }

        Map<PeriodKey, List<SettlementOrderSummarySourceDto>> ordersByClosedPeriod = new LinkedHashMap<>();

        for (SettlementOrderSummarySourceDto orderSummary : allOrderSummaries) {
            if (orderSummary == null || orderSummary.getOrderId() == null || orderSummary.getBasisDate() == null) {
                debug("[SETTLEMENT][ORDER-SKIP] null summary data sellerDealerProfileId=" + sellerDealerProfileId);
                continue;
            }

            LocalDate basisDate = orderSummary.getBasisDate().toLocalDate();

            if (basisDate.isBefore(effectiveStart) || basisDate.isAfter(effectiveEnd)) {
                debug("[SETTLEMENT][ORDER-SKIP] out of effective range sellerDealerProfileId=" + sellerDealerProfileId
                    + ", orderId=" + orderSummary.getOrderId()
                    + ", basisDate=" + basisDate);
                continue;
            }

            PeriodKey rawPeriod = resolveCyclePeriod(executionPolicy.getCycle(), basisDate);

            debug("[SETTLEMENT][ORDER-PERIOD] sellerDealerProfileId=" + sellerDealerProfileId
                + ", orderId=" + orderSummary.getOrderId()
                + ", basisDate=" + basisDate
                + ", rawPeriod=" + rawPeriod.getStartDate() + " ~ " + rawPeriod.getEndDate());

            // 아직 닫히지 않은 정산기간은 조회/실행 제외
            if (rawPeriod.getEndDate().isAfter(effectiveEnd)) {
                debug("[SETTLEMENT][ORDER-SKIP] raw period not closed sellerDealerProfileId=" + sellerDealerProfileId
                    + ", orderId=" + orderSummary.getOrderId()
                    + ", rawPeriodEnd=" + rawPeriod.getEndDate()
                    + ", effectiveEnd=" + effectiveEnd);
                continue;
            }

            LocalDate periodStart = max(effectiveStart, rawPeriod.getStartDate());
            LocalDate periodEnd = min(effectiveEnd, rawPeriod.getEndDate());

            if (periodEnd.isBefore(periodStart)) {
                debug("[SETTLEMENT][ORDER-SKIP] clipped period invalid sellerDealerProfileId=" + sellerDealerProfileId
                    + ", orderId=" + orderSummary.getOrderId()
                    + ", clippedPeriod=" + periodStart + " ~ " + periodEnd);
                continue;
            }

            PeriodKey closedPeriod = new PeriodKey(periodStart, periodEnd);
            ordersByClosedPeriod.computeIfAbsent(closedPeriod, key -> new ArrayList<>()).add(orderSummary);
        }

        debug("[SETTLEMENT][SELLER-CANDIDATE] sellerDealerProfileId=" + sellerDealerProfileId
            + ", closedPeriodCount=" + ordersByClosedPeriod.size());

        for (Map.Entry<PeriodKey, List<SettlementOrderSummarySourceDto>> entry : ordersByClosedPeriod.entrySet()) {
            PeriodKey closedPeriod = entry.getKey();
            List<SettlementOrderSummarySourceDto> periodOrders = entry.getValue();

            debug("[SETTLEMENT][CLOSED-PERIOD] sellerDealerProfileId=" + sellerDealerProfileId
                + ", closedPeriod=" + closedPeriod.getStartDate() + " ~ " + closedPeriod.getEndDate()
                + ", periodOrders.size=" + (periodOrders != null ? periodOrders.size() : 0));

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
                debug("[SETTLEMENT][UNPAID-SEGMENT] sellerDealerProfileId=" + sellerDealerProfileId
                    + ", unpaidSegment=" + unpaidSegment.getStartDate() + " ~ " + unpaidSegment.getEndDate());

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

                long commissionAmount = calculateCommissionAmount(unsettledOrders, histories, currentPolicy);
                long settlementAmount = grossAmount - commissionAmount;
                BigDecimal effectiveCommissionRate = calculateEffectiveCommissionRate(grossAmount, commissionAmount);

                DealerSettlementPolicyHistory referenceHistory = findEffectiveHistory(histories, unpaidSegment.getEndDate());

                debug("[SETTLEMENT][CANDIDATE-CREATED] sellerDealerProfileId=" + sellerDealerProfileId
                    + ", cycle=" + executionPolicy.getCycle()
                    + ", basis=" + executionPolicy.getBasis()
                    + ", period=" + unpaidSegment.getStartDate() + " ~ " + unpaidSegment.getEndDate()
                    + ", grossAmount=" + grossAmount
                    + ", commissionAmount=" + commissionAmount
                    + ", settlementAmount=" + settlementAmount
                    + ", effectiveCommissionRate=" + effectiveCommissionRate
                    + ", referenceHistoryId=" + (referenceHistory != null ? referenceHistory.getId() : null));

                candidates.add(
                    Candidate.builder()
                        .sellerDealerProfile(sellerDealerProfile)
                        .history(referenceHistory)
                        .currentPolicy(currentPolicy)
                        .cycle(executionPolicy.getCycle())
                        .basis(executionPolicy.getBasis())
                        .commissionRate(effectiveCommissionRate)
                        .sellerMemberIdSnapshot(executionPolicy.getSellerMemberId())
                        .memberUsername(executionPolicy.getMemberUsername())
                        .memberName(executionPolicy.getMemberName())
                        .memberEmail(executionPolicy.getMemberEmail())
                        .memberMobile(executionPolicy.getMemberMobile())
                        .companyName(executionPolicy.getCompanyName())
                        .shopName(executionPolicy.getShopName())
                        .supplierCode(executionPolicy.getSupplierCode())
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

    private ExecutionPolicy resolveExecutionPolicy(
        DealerSettlementPolicy currentPolicy,
        List<DealerSettlementPolicyHistory> histories,
        LocalDate targetDate
    ) {
        if (currentPolicy != null
            && currentPolicy.getCycle() != null
            && currentPolicy.getBasis() != null
            && currentPolicy.getSellerDealerProfile() != null) {

            SellerDealerProfile seller = currentPolicy.getSellerDealerProfile();
            Member sellerMember = seller.getMember();

            return ExecutionPolicy.builder()
                .cycle(currentPolicy.getCycle())
                .basis(currentPolicy.getBasis())
                .sellerMemberId(sellerMember != null ? sellerMember.getId() : null)
                .memberUsername(sellerMember != null ? sellerMember.getUsername() : null)
                .memberName(sellerMember != null ? sellerMember.getName() : null)
                .memberEmail(sellerMember != null ? sellerMember.getEmail() : null)
                .memberMobile(sellerMember != null ? sellerMember.getMobile() : null)
                .companyName(seller.getCompanyProfile() != null ? seller.getCompanyProfile().getCompanyName() : null)
                .shopName(seller.getShopName())
                .supplierCode(seller.getSupplierCode())
                .build();
        }

        DealerSettlementPolicyHistory effectiveHistory = findEffectiveHistory(histories, targetDate);
        if (effectiveHistory == null) {
            return null;
        }

        return ExecutionPolicy.builder()
            .cycle(effectiveHistory.getCycle())
            .basis(effectiveHistory.getBasis())
            .sellerMemberId(effectiveHistory.getSellerMemberIdSnapshot())
            .memberUsername(effectiveHistory.getMemberUsernameSnapshot())
            .memberName(effectiveHistory.getMemberNameSnapshot())
            .memberEmail(effectiveHistory.getMemberEmailSnapshot())
            .memberMobile(effectiveHistory.getMemberMobileSnapshot())
            .companyName(effectiveHistory.getCompanyNameSnapshot())
            .shopName(effectiveHistory.getShopNameSnapshot())
            .supplierCode(effectiveHistory.getSupplierCodeSnapshot())
            .build();
    }

    private SellerDealerProfile resolveSellerDealerProfile(
        DealerSettlementPolicy currentPolicy,
        List<DealerSettlementPolicyHistory> histories
    ) {
        if (currentPolicy != null && currentPolicy.getSellerDealerProfile() != null) {
            return currentPolicy.getSellerDealerProfile();
        }

        if (histories == null || histories.isEmpty()) {
            return null;
        }

        for (DealerSettlementPolicyHistory history : histories) {
            if (history != null && history.getSellerDealerProfile() != null) {
                return history.getSellerDealerProfile();
            }
        }

        return null;
    }

    private LocalDate resolveSellerSettlementStartDate(
        ExecutionCondition condition,
        List<DealerSettlementPolicyHistory> histories,
        List<SettlementOrderSummarySourceDto> orderSummaries
    ) {
        if (condition.getEffectiveFromDate() != null) {
            return condition.getEffectiveFromDate();
        }

        LocalDate earliestHistoryStart = histories == null ? null : histories.stream()
            .map(DealerSettlementPolicyHistory::getApplyStartDate)
            .filter(Objects::nonNull)
            .min(LocalDate::compareTo)
            .orElse(null);

        if (earliestHistoryStart != null) {
            return earliestHistoryStart;
        }

        return orderSummaries.stream()
            .map(SettlementOrderSummarySourceDto::getBasisDate)
            .filter(Objects::nonNull)
            .map(LocalDateTime::toLocalDate)
            .min(LocalDate::compareTo)
            .orElse(null);
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
        List<DealerSettlementPolicyHistory> histories,
        DealerSettlementPolicy currentPolicy
    ) {
        long totalCommissionAmount = 0L;

        for (SettlementOrderSummarySourceDto orderSummary : unsettledOrders) {
            LocalDate basisDate = orderSummary.getBasisDate().toLocalDate();
            BigDecimal commissionRate = resolveCommissionRateAtDate(histories, currentPolicy, basisDate);
            totalCommissionAmount += calculateCommissionAmount(orderSummary.getDealerAmount(), commissionRate);
        }

        return totalCommissionAmount;
    }

    private BigDecimal resolveCommissionRateAtDate(
        List<DealerSettlementPolicyHistory> histories,
        DealerSettlementPolicy currentPolicy,
        LocalDate targetDate
    ) {
        DealerSettlementPolicyHistory effectiveHistory = findEffectiveHistory(histories, targetDate);
        if (effectiveHistory != null && effectiveHistory.getCommissionRate() != null) {
            return effectiveHistory.getCommissionRate();
        }

        if (currentPolicy != null && currentPolicy.getCommissionRate() != null) {
            return currentPolicy.getCommissionRate();
        }

        return BigDecimal.ZERO;
    }

    /**
     * 기간 내 여러 수수료율이 섞일 수 있으므로,
     * settlement.commissionRate 는 가중평균 개념의 표시용 스냅샷으로 저장합니다.
     */
    private BigDecimal calculateEffectiveCommissionRate(long grossAmount, long commissionAmount) {
        if (grossAmount <= 0L || commissionAmount <= 0L) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.DOWN);
        }

        return BigDecimal.valueOf(commissionAmount)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(grossAmount), 2, RoundingMode.DOWN);
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

    private boolean matchesRequestedFilters(ExecutionPolicy executionPolicy, SettlementExecuteSearchRequest request) {
        if (executionPolicy == null) {
            return false;
        }

        if (request.getCycles() != null
            && !request.getCycles().isEmpty()
            && !request.getCycles().contains(executionPolicy.getCycle())) {
            return false;
        }

        if (request.getBases() != null
            && !request.getBases().isEmpty()
            && !request.getBases().contains(executionPolicy.getBasis())) {
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
    private static class ExecutionPolicy {
        private SettlementCycle cycle;
        private SettlementBasis basis;
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
    @EqualsAndHashCode
    @RequiredArgsConstructor
    private static class PeriodKey {
        private final LocalDate startDate;
        private final LocalDate endDate;
    }
}