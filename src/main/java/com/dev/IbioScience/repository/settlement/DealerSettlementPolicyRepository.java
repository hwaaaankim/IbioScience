package com.dev.IbioScience.repository.settlement;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.enums.product.SettlementBasis;
import com.dev.IbioScience.enums.product.SettlementCycle;
import com.dev.IbioScience.model.auth.DealerSettlementPolicy;
import com.dev.IbioScience.model.auth.SellerDealerProfile;

public interface DealerSettlementPolicyRepository extends JpaRepository<DealerSettlementPolicy, Long> {
	Optional<DealerSettlementPolicy> findBySellerDealerProfile(SellerDealerProfile profile);
	
	 @Query("""
	        select distinct p
	        from DealerSettlementPolicy p
	        join fetch p.sellerDealerProfile s
	        join fetch s.member m
	        left join fetch s.companyProfile cp
	        where (:cyclesEmpty = true or p.cycle in :cycles)
	          and (:basesEmpty = true or p.basis in :bases)
	          and (
	                :keywordBlank = true
	                or lower(coalesce(cp.companyName, '')) like lower(concat('%', :keyword, '%'))
	                or lower(coalesce(m.name, '')) like lower(concat('%', :keyword, '%'))
	                or lower(coalesce(m.username, '')) like lower(concat('%', :keyword, '%'))
	                or lower(coalesce(s.shopName, '')) like lower(concat('%', :keyword, '%'))
	                or lower(coalesce(m.mobile, '')) like lower(concat('%', :keyword, '%'))
	                or lower(coalesce(m.email, '')) like lower(concat('%', :keyword, '%'))
	          )
	        order by s.id asc
	    """)
	    List<DealerSettlementPolicy> searchForBootstrap(
	        @Param("cycles") List<SettlementCycle> cycles,
	        @Param("bases") List<SettlementBasis> bases,
	        @Param("cyclesEmpty") boolean cyclesEmpty,
	        @Param("basesEmpty") boolean basesEmpty,
	        @Param("keywordBlank") boolean keywordBlank,
	        @Param("keyword") String keyword
	    );
	 
	 Optional<DealerSettlementPolicy> findBySellerDealerProfile_Id(Long sellerDealerProfileId);

    @Query("""
        select p
        from DealerSettlementPolicy p
        join fetch p.sellerDealerProfile s
        join fetch s.member m
        left join fetch s.companyProfile cp
        where s.id in :sellerDealerProfileIds
    """)
    List<DealerSettlementPolicy> findAllForExecutionBySellerDealerProfileIds(
        @Param("sellerDealerProfileIds") Collection<Long> sellerDealerProfileIds
    );
}