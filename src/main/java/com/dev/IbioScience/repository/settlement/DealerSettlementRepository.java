package com.dev.IbioScience.repository.settlement;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.enums.product.SettlementBasis;
import com.dev.IbioScience.model.settlement.DealerSettlement;

public interface DealerSettlementRepository extends JpaRepository<DealerSettlement, Long>, JpaSpecificationExecutor<DealerSettlement> {

    Optional<DealerSettlement> findBySellerDealerProfile_IdAndPeriodStartDateAndPeriodEndDateAndSettlementBasis(
        Long sellerDealerProfileId,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        SettlementBasis settlementBasis
    );

    List<DealerSettlement> findByIdIn(Collection<Long> ids);

    @Query("""
        select case when count(ds) > 0 then true else false end
        from DealerSettlement ds
        where ds.sellerDealerProfile.id = :sellerDealerProfileId
          and ds.settlementBasis = :settlementBasis
          and ds.periodStartDate <= :periodEndDate
          and ds.periodEndDate >= :periodStartDate
    """)
    boolean existsOverlappingPeriod(
        @Param("sellerDealerProfileId") Long sellerDealerProfileId,
        @Param("settlementBasis") SettlementBasis settlementBasis,
        @Param("periodStartDate") LocalDate periodStartDate,
        @Param("periodEndDate") LocalDate periodEndDate
    );

    @Query("""
        select ds
        from DealerSettlement ds
        where ds.sellerDealerProfile.id = :sellerDealerProfileId
          and ds.settlementBasis = :basis
          and ds.periodStartDate <= :toDate
          and ds.periodEndDate >= :fromDate
        order by ds.periodStartDate asc, ds.periodEndDate asc, ds.id asc
    """)
    List<DealerSettlement> findOverlappingSettlements(
        @Param("sellerDealerProfileId") Long sellerDealerProfileId,
        @Param("basis") SettlementBasis basis,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );

    /**
     * basis 변경 이력이 있어도, 이미 지급된 기간 자체는 다시 정산되면 안 되므로
     * basis 구분 없이 겹치는 정산 기간을 조회합니다.
     */
    @Query("""
        select ds
        from DealerSettlement ds
        where ds.sellerDealerProfile.id = :sellerDealerProfileId
          and ds.periodStartDate <= :toDate
          and ds.periodEndDate >= :fromDate
        order by ds.periodStartDate asc, ds.periodEndDate asc, ds.id asc
    """)
    List<DealerSettlement> findOverlappingSettlementsAnyBasis(
        @Param("sellerDealerProfileId") Long sellerDealerProfileId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );
}