package com.dev.IbioScience.repository.settlement;

import java.time.LocalDateTime;
import java.util.List;

import com.dev.IbioScience.dto.settlement.SettlementOrderSummarySourceDto;
import com.dev.IbioScience.enums.product.SettlementBasis;

public interface SettlementOrderSourceQueryRepository {

    List<Long> findSellerDealerProfileIdsHavingDealerOrders(
        LocalDateTime fromDateTime,
        LocalDateTime toDateTimeExclusive,
        List<SettlementBasis> bases,
        String keyword
    );

    List<SettlementOrderSummarySourceDto> findDealerOrderSummaries(
        Long sellerDealerProfileId,
        SettlementBasis basis,
        LocalDateTime fromDateTime,
        LocalDateTime toDateTimeExclusive
    );
}