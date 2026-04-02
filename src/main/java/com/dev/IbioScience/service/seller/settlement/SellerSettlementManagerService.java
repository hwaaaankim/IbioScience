package com.dev.IbioScience.service.seller.settlement;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.seller.settlement.SellerSettlementManagerOrderDetailResponse;
import com.dev.IbioScience.dto.seller.settlement.SellerSettlementManagerOrderRowDto;
import com.dev.IbioScience.dto.seller.settlement.SellerSettlementManagerPageResponse;
import com.dev.IbioScience.dto.seller.settlement.SellerSettlementManagerRowDto;
import com.dev.IbioScience.dto.seller.settlement.SellerSettlementManagerSearchRequest;
import com.dev.IbioScience.model.settlement.DealerSettlement;
import com.dev.IbioScience.model.settlement.DealerSettlementOrder;
import com.dev.IbioScience.repository.settlement.DealerSettlementOrderRepository;
import com.dev.IbioScience.repository.settlement.DealerSettlementRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerSettlementManagerService {

    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(10, 30, 50, 100);

    private final DealerSettlementRepository dealerSettlementRepository;
    private final DealerSettlementOrderRepository dealerSettlementOrderRepository;

    public SellerSettlementManagerPageResponse getMySettlementPage(String username,
                                                                   SellerSettlementManagerSearchRequest request) {
        if (request.getFromDate() != null
                && request.getToDate() != null
                && request.getFromDate().isAfter(request.getToDate())) {
            throw new IllegalArgumentException("기간 시작일은 기간 종료일보다 클 수 없습니다.");
        }

        int page = request.getPage() == null || request.getPage() < 1 ? 1 : request.getPage();
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();

        if (!ALLOWED_PAGE_SIZES.contains(pageSize)) {
            pageSize = 10;
        }

        PageRequest pageable = PageRequest.of(
            page - 1,
            pageSize,
            Sort.by(
                Sort.Order.desc("periodEndDate"),
                Sort.Order.desc("periodStartDate"),
                Sort.Order.desc("id")
            )
        );

        Page<DealerSettlement> resultPage =
            dealerSettlementRepository.searchPageForSeller(
                username,
                request.getFromDate(),
                request.getToDate(),
                pageable
            );

        List<SellerSettlementManagerRowDto> content = resultPage.getContent()
            .stream()
            .map(this::toRowDto)
            .collect(Collectors.toList());

        return SellerSettlementManagerPageResponse.builder()
            .content(content)
            .currentPage(resultPage.getNumber() + 1)
            .pageSize(resultPage.getSize())
            .totalElements(resultPage.getTotalElements())
            .totalPages(resultPage.getTotalPages())
            .first(resultPage.isFirst())
            .last(resultPage.isLast())
            .hasPrevious(resultPage.hasPrevious())
            .hasNext(resultPage.hasNext())
            .build();
    }

    public SellerSettlementManagerOrderDetailResponse getMySettlementOrderDetail(String username, Long settlementId) {
        DealerSettlement settlement = dealerSettlementRepository
            .findByIdAndSellerDealerProfile_Member_Username(settlementId, username)
            .orElseThrow(() -> new IllegalArgumentException("해당 정산 정보를 찾을 수 없습니다."));

        List<DealerSettlementOrder> orderEntities =
            dealerSettlementOrderRepository.findBySettlement_IdOrderByBasisDateSnapshotAscOrderIdSnapshotAsc(settlement.getId());

        List<SellerSettlementManagerOrderRowDto> orders = orderEntities.stream()
            .map(order -> SellerSettlementManagerOrderRowDto.builder()
                .orderIdSnapshot(order.getOrderIdSnapshot())
                .orderNoSnapshot(order.getOrderNoSnapshot())
                .ordererNameSnapshot(order.getOrdererNameSnapshot())
                .basisDateSnapshot(order.getBasisDateSnapshot())
                .dealerItemAmount(order.getDealerItemAmount())
                .dealerItemCount(order.getDealerItemCount())
                .build())
            .collect(Collectors.toList());

        return SellerSettlementManagerOrderDetailResponse.builder()
            .settlementId(settlement.getId())
            .periodStartDate(settlement.getPeriodStartDate())
            .periodEndDate(settlement.getPeriodEndDate())
            .settlementCycle(settlement.getSettlementCycle())
            .settlementBasis(settlement.getSettlementBasis())
            .commissionRate(settlement.getCommissionRate())
            .grossAmount(settlement.getGrossAmount())
            .commissionAmount(settlement.getCommissionAmount())
            .settlementAmount(settlement.getSettlementAmount())
            .orderCount(settlement.getOrderCount())
            .itemCount(settlement.getItemCount())
            .payStatus(settlement.getPayStatus())
            .executedAt(settlement.getExecutedAt())
            .paidAt(settlement.getPaidAt())
            .orders(orders)
            .build();
    }

    private SellerSettlementManagerRowDto toRowDto(DealerSettlement settlement) {
        return SellerSettlementManagerRowDto.builder()
            .id(settlement.getId())
            .periodStartDate(settlement.getPeriodStartDate())
            .periodEndDate(settlement.getPeriodEndDate())
            .settlementCycle(settlement.getSettlementCycle())
            .settlementBasis(settlement.getSettlementBasis())
            .commissionRate(settlement.getCommissionRate())
            .grossAmount(settlement.getGrossAmount())
            .commissionAmount(settlement.getCommissionAmount())
            .settlementAmount(settlement.getSettlementAmount())
            .orderCount(settlement.getOrderCount())
            .itemCount(settlement.getItemCount())
            .payStatus(settlement.getPayStatus())
            .executedAt(settlement.getExecutedAt())
            .paidAt(settlement.getPaidAt())
            .build();
    }
}