package com.dev.IbioScience.repository.settlement;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.model.settlement.DealerSettlementOrder;

public interface DealerSettlementOrderRepository extends JpaRepository<DealerSettlementOrder, Long> {

    List<DealerSettlementOrder> findBySettlement_IdOrderByBasisDateSnapshotAscOrderIdSnapshotAsc(Long settlementId);

    @Query("""
        select distinct dso.orderIdSnapshot
        from DealerSettlementOrder dso
        join dso.settlement ds
        where ds.sellerDealerProfile.id = :sellerDealerProfileId
          and dso.orderIdSnapshot in :orderIds
    """)
    List<Long> findAlreadySettledOrderIds(
        @Param("sellerDealerProfileId") Long sellerDealerProfileId,
        @Param("orderIds") Collection<Long> orderIds
    );
    
}