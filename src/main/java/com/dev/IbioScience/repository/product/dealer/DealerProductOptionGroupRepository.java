package com.dev.IbioScience.repository.product.dealer;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.model.product.dealer.DealerProductOptionGroup;

public interface DealerProductOptionGroupRepository extends JpaRepository<DealerProductOptionGroup, Long> {

    @Query("""
        select distinct og
        from DealerProductOptionGroup og
        join fetch og.dealerProduct dp
        left join fetch og.options opt
        where dp.id in :dealerProductIds
        order by dp.id asc,
                 case when og.sortOrder is null then 2147483647 else og.sortOrder end asc,
                 og.id asc,
                 case when opt.sortOrder is null then 2147483647 else opt.sortOrder end asc,
                 opt.id asc
        """)
    List<DealerProductOptionGroup> findWithOptionsByDealerProductIds(
            @Param("dealerProductIds") List<Long> dealerProductIds
    );
}