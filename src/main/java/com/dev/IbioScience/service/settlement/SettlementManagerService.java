package com.dev.IbioScience.service.settlement;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
import com.dev.IbioScience.dto.settlement.SettlementStatusUpdateRequest;
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
        List<DealerSettlementOrder> rows = settlementOrderRepository
            .findBySettlement_IdOrderByBasisDateSnapshotAscOrderIdSnapshotAsc(settlementId);

        return rows.stream()
            .map(row -> SettlementOrderModalDto.builder()
                .orderId(row.getOrderIdSnapshot())
                .orderNo(row.getOrderNoSnapshot())
                .ordererName(row.getOrdererNameSnapshot())
                .basisDate(row.getBasisDateSnapshot())
                .dealerItemCount(row.getDealerItemCount())
                .dealerItemAmount(row.getDealerItemAmount())
                .build())
            .toList();
    }

    @Transactional
    public void updateStatuses(SettlementStatusUpdateRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return;
        }

        List<Long> ids = request.getItems().stream()
            .map(SettlementStatusUpdateRequest.Item::getSettlementId)
            .toList();

        List<DealerSettlement> settlements = settlementRepository.findByIdIn(ids);

        for (DealerSettlement settlement : settlements) {
            SettlementStatusUpdateRequest.Item item = request.getItems().stream()
                .filter(x -> settlement.getId().equals(x.getSettlementId()))
                .findFirst()
                .orElse(null);

            if (item == null || item.getPayStatus() == null) {
                continue;
            }

            settlement.setPayStatus(item.getPayStatus());
            if (item.getPayStatus().name().equals("PAID")) {
                settlement.setPaidAt(LocalDateTime.now());
            } else {
                settlement.setPaidAt(null);
            }
        }
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
}