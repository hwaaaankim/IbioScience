package com.dev.IbioScience.repository.product.dealer;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.model.product.dealer.DealerProductOption;

public interface DealerProductOptionRepository extends JpaRepository<DealerProductOption, Long> {

    @Query("""
        select opt
        from DealerProductOption opt
        join fetch opt.group og
        join fetch og.dealerProduct dp
        where dp.id in :dealerProductIds
        order by dp.id asc,
                 case when og.sortOrder is null then 2147483647 else og.sortOrder end asc,
                 og.id asc,
                 case when opt.sortOrder is null then 2147483647 else opt.sortOrder end asc,
                 opt.id asc
        """)
    List<DealerProductOption> findFrontOptionsByDealerProductIds(
            @Param("dealerProductIds") List<Long> dealerProductIds
    );
}