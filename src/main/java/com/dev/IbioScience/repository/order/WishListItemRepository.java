package com.dev.IbioScience.repository.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.enums.product.SaleStatus;
import com.dev.IbioScience.model.order.WishListItem;

public interface WishListItemRepository extends JpaRepository<WishListItem, Long> , WishListItemRepositoryCustom{

    long countByMember_Id(Long memberId);

    Optional<WishListItem> findByMember_IdAndProduct_Id(Long memberId, Long productId);

    boolean existsByMember_IdAndProduct_Id(Long memberId, Long productId);

    void deleteByMember_IdAndProduct_Id(Long memberId, Long productId);

    Page<WishListItem> findByMember_Id(Long memberId, Pageable pageable);

    void deleteByMember_IdAndProduct_IdIn(Long memberId, List<Long> productIds);

    /**
     * ✅ 기존 로직 유지: 페이징 먼저, fetch join 과도하게 안 함
     * - saleStatusFilterOrNull 이 null이면 전체
     */
    @Query("""
        select w
        from WishListItem w
        join w.product p
        where w.member.id = :memberId
          and (:saleStatus is null or p.saleStatus = :saleStatus)
        order by w.id desc
    """)
    Page<WishListItem> findPageByMemberIdAndSaleStatus(@Param("memberId") Long memberId,
                                                      @Param("saleStatus") SaleStatus saleStatus,
                                                      Pageable pageable);
    
    
    @Query("""
        select distinct w
        from WishListItem w
        join fetch w.product p
        left join fetch p.brand b
        left join fetch p.images i
        where w.id in :ids
    """)
    List<WishListItem> findAllWithProductAndBrandAndImagesByIdIn(@Param("ids") List<Long> ids);

    @Query("""
        select count(w.id)
        from WishListItem w
        where w.member.id = :memberId
          and w.id in :ids
    """)
    long countByMemberIdAndIdIn(@Param("memberId") Long memberId, @Param("ids") List<Long> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from WishListItem w
        where w.member.id = :memberId
          and w.id in :ids
    """)
    int deleteByMemberIdAndIdIn(@Param("memberId") Long memberId, @Param("ids") List<Long> ids);
}