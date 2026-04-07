package com.dev.IbioScience.service.settlement;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.dev.IbioScience.dto.settlement.SettlementManagerPageResponse;
import com.dev.IbioScience.dto.settlement.SettlementManagerRowDto;
import com.dev.IbioScience.dto.settlement.SettlementManagerSearchRequest;
import com.dev.IbioScience.dto.settlement.SettlementOrderModalDto;
import com.dev.IbioScience.dto.settlement.SettlementOrderUpdateRequest;
import com.dev.IbioScience.dto.settlement.SettlementStatusUpdateRequest;
import com.dev.IbioScience.enums.settlement.SettlementOrderInclusionStatus;
import com.dev.IbioScience.model.settlement.DealerSettlement;
import com.dev.IbioScience.model.settlement.DealerSettlementOrder;
import com.dev.IbioScience.repository.settlement.DealerSettlementOrderRepository;
import com.dev.IbioScience.repository.settlement.DealerSettlementRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettlementManagerService {

    private final DealerSettlementRepository settlementRepository;
    private final DealerSettlementOrderRepository settlementOrderRepository;

    @Transactional(readOnly = true)
    public SettlementManagerPageResponse search(SettlementManagerSearchRequest request) {
        int page = request.getPage() == null || request.getPage() < 0 ? 0 : request.getPage();
        int size = normalizeSize(request.getSize());

        Page<DealerSettlement> result = settlementRepository.findAll(
            buildSpec(request),
            PageRequest.of(page, size)
        );

        List<SettlementManagerRowDto> content = result.getContent().stream()
            .map(settlement -> SettlementManagerRowDto.builder()
                .id(settlement.getId())
                .memberUsername(settlement.getMemberUsernameSnapshot())
                .memberName(settlement.getMemberNameSnapshot())
                .companyName(settlement.getCompanyNameSnapshot())
                .shopName(settlement.getShopNameSnapshot())
                .cycle(settlement.getSettlementCycle())
                .basis(settlement.getSettlementBasis())
                .periodStartDate(settlement.getPeriodStartDate())
                .periodEndDate(settlement.getPeriodEndDate())
                .orderCount(settlement.getOrderCount())
                .itemCount(settlement.getItemCount())
                .grossAmount(settlement.getGrossAmount())
                .commissionAmount(settlement.getCommissionAmount())
                .settlementAmount(settlement.getSettlementAmount())
                .executedAt(settlement.getExecutedAt())
                .paidAt(settlement.getPaidAt())
                .payStatus(settlement.getPayStatus())
                .build())
            .toList();

        return SettlementManagerPageResponse.builder()
            .content(content)
            .page(result.getNumber())
            .size(result.getSize())
            .totalElements(result.getTotalElements())
            .totalPages(result.getTotalPages())
            .build();
    }

    @Transactional(readOnly = true)
    public List<SettlementOrderModalDto> getSettlementOrders(Long settlementId) {
        List<DealerSettlementOrder> rows =
            settlementOrderRepository.findBySettlement_IdOrderByBasisDateSnapshotAscOrderIdSnapshotAsc(settlementId);

        return rows.stream()
            .map(row -> SettlementOrderModalDto.builder()
                .settlementOrderId(row.getId())
                .settlementId(row.getSettlement().getId())
                .orderId(row.getOrderIdSnapshot())
                .orderNo(row.getOrderNoSnapshot())
                .ordererName(row.getOrdererNameSnapshot())
                .basisDate(row.getBasisDateSnapshot())
                .dealerItemCount(row.getDealerItemCount())
                .unitAmount(calculateAverageUnitAmount(row.getDealerItemAmount(), row.getDealerItemCount()))
                .dealerItemAmount(nullSafeLong(row.getDealerItemAmount()))
                .commissionRate(row.getSettlement().getCommissionRate())
                .commissionAmount(nullSafeLong(row.getCommissionAmount()))
                .settlementAmount(resolveSettlementAmount(row))
                .inclusionStatus(resolveInclusionStatus(row))
                .included(resolveInclusionStatus(row).isIncluded())
                .memo(row.getMemo())
                .build())
            .toList();
    }

    @Transactional
    public void updateSettlementOrders(Long settlementId, SettlementOrderUpdateRequest request) {
        if (settlementId == null) {
            throw new IllegalArgumentException("정산 ID가 없습니다.");
        }

        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return;
        }

        Map<Long, SettlementOrderUpdateRequest.Item> requestItemMap = new LinkedHashMap<>();
        for (SettlementOrderUpdateRequest.Item item : request.getItems()) {
            if (item == null || item.getSettlementOrderId() == null) {
                continue;
            }
            requestItemMap.put(item.getSettlementOrderId(), item);
        }

        if (requestItemMap.isEmpty()) {
            return;
        }

        List<DealerSettlementOrder> orderRows = settlementOrderRepository.findAllById(requestItemMap.keySet());

        if (orderRows.size() != requestItemMap.size()) {
            throw new IllegalArgumentException("일부 정산 주문을 찾을 수 없습니다.");
        }

        for (DealerSettlementOrder orderRow : orderRows) {
            if (orderRow.getSettlement() == null || !Objects.equals(orderRow.getSettlement().getId(), settlementId)) {
                throw new IllegalArgumentException("다른 정산에 속한 주문이 포함되어 있습니다.");
            }

            SettlementOrderUpdateRequest.Item reqItem = requestItemMap.get(orderRow.getId());
            if (reqItem == null) {
                continue;
            }

            if (reqItem.getInclusionStatus() != null) {
                orderRow.setInclusionStatus(reqItem.getInclusionStatus());
            }

            orderRow.setMemo(normalizeMemo(reqItem.getMemo()));
        }

        recalculateSettlement(settlementId);
    }

    @Transactional
    public void updateStatuses(SettlementStatusUpdateRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return;
        }

        Map<Long, SettlementStatusUpdateRequest.Item> requestItemMap = new LinkedHashMap<>();
        for (SettlementStatusUpdateRequest.Item item : request.getItems()) {
            if (item == null || item.getSettlementId() == null) {
                continue;
            }
            requestItemMap.put(item.getSettlementId(), item);
        }

        if (requestItemMap.isEmpty()) {
            return;
        }

        List<DealerSettlement> settlements = settlementRepository.findByIdIn(requestItemMap.keySet());

        for (DealerSettlement settlement : settlements) {
            SettlementStatusUpdateRequest.Item item = requestItemMap.get(settlement.getId());
            if (item == null || item.getPayStatus() == null) {
                continue;
            }

            settlement.setPayStatus(item.getPayStatus());

            if ("PAID".equals(item.getPayStatus().name())) {
                settlement.setPaidAt(LocalDateTime.now());
            } else {
                settlement.setPaidAt(null);
            }
        }
    }

    private void recalculateSettlement(Long settlementId) {
        DealerSettlement settlement = settlementRepository.findById(settlementId)
            .orElseThrow(() -> new IllegalArgumentException("정산 정보를 찾을 수 없습니다. settlementId=" + settlementId));

        List<DealerSettlementOrder> rows =
            settlementOrderRepository.findBySettlement_IdOrderByBasisDateSnapshotAscOrderIdSnapshotAsc(settlementId);

        long grossAmount = 0L;
        long commissionAmount = 0L;
        long settlementAmount = 0L;
        int orderCount = 0;
        int itemCount = 0;

        for (DealerSettlementOrder row : rows) {
            SettlementOrderInclusionStatus status = resolveInclusionStatus(row);
            if (!status.isIncluded()) {
                continue;
            }

            grossAmount += nullSafeLong(row.getDealerItemAmount());
            commissionAmount += nullSafeLong(row.getCommissionAmount());
            settlementAmount += resolveSettlementAmount(row);
            orderCount += 1;
            itemCount += nullSafeInt(row.getDealerItemCount());
        }

        settlement.setGrossAmount(grossAmount);
        settlement.setCommissionAmount(commissionAmount);
        settlement.setSettlementAmount(settlementAmount);
        settlement.setOrderCount(orderCount);
        settlement.setItemCount(itemCount);
    }

    private Specification<DealerSettlement> buildSpec(SettlementManagerSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getBases() != null && !request.getBases().isEmpty()) {
                predicates.add(root.get("settlementBasis").in(request.getBases()));
            }

            if (request.getCycles() != null && !request.getCycles().isEmpty()) {
                predicates.add(root.get("settlementCycle").in(request.getCycles()));
            }

            if (request.getPayStatus() != null) {
                predicates.add(cb.equal(root.get("payStatus"), request.getPayStatus()));
            }

            if (StringUtils.hasText(request.getKeyword())) {
                String likeKeyword = "%" + request.getKeyword().trim().toLowerCase() + "%";
                predicates.add(
                    cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("companyNameSnapshot"), "")), likeKeyword),
                        cb.like(cb.lower(cb.coalesce(root.get("memberNameSnapshot"), "")), likeKeyword),
                        cb.like(cb.lower(cb.coalesce(root.get("memberUsernameSnapshot"), "")), likeKeyword),
                        cb.like(cb.lower(cb.coalesce(root.get("shopNameSnapshot"), "")), likeKeyword),
                        cb.like(cb.lower(cb.coalesce(root.get("memberMobileSnapshot"), "")), likeKeyword),
                        cb.like(cb.lower(cb.coalesce(root.get("memberEmailSnapshot"), "")), likeKeyword)
                    )
                );
            }

            query.orderBy(cb.desc(root.get("id")));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return 10;
        }

        return switch (size) {
            case 10, 30, 50, 100 -> size;
            default -> 10;
        };
    }

    private SettlementOrderInclusionStatus resolveInclusionStatus(DealerSettlementOrder row) {
        return row.getInclusionStatus() == null ? SettlementOrderInclusionStatus.NORMAL : row.getInclusionStatus();
    }

    private Long resolveSettlementAmount(DealerSettlementOrder row) {
        if (row.getSettlementAmount() != null) {
            return row.getSettlementAmount();
        }
        return nullSafeLong(row.getDealerItemAmount()) - nullSafeLong(row.getCommissionAmount());
    }

    private Long calculateAverageUnitAmount(Long totalAmount, Integer quantity) {
        long amount = nullSafeLong(totalAmount);
        int qty = nullSafeInt(quantity);

        if (qty <= 0) {
            return 0L;
        }

        return amount / qty;
    }

    private String normalizeMemo(String memo) {
        if (!StringUtils.hasText(memo)) {
            return null;
        }

        String normalized = memo.trim();
        if (normalized.length() > 1000) {
            throw new IllegalArgumentException("주문 메모는 1000자 이하여야 합니다.");
        }

        return normalized;
    }

    private long nullSafeLong(Long value) {
        return value == null ? 0L : value;
    }

    private int nullSafeInt(Integer value) {
        return value == null ? 0 : value;
    }
}