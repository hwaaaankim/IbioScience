package com.dev.IbioScience.repository.settlement;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.dev.IbioScience.dto.settlement.SettlementOrderSummarySourceDto;
import com.dev.IbioScience.enums.product.SettlementBasis;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class SettlementOrderSourceQueryRepositoryImpl implements SettlementOrderSourceQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Long> findSellerDealerProfileIdsHavingDealerOrders(
        LocalDateTime fromDateTime,
        LocalDateTime toDateTimeExclusive,
        List<SettlementBasis> bases,
        String keyword
    ) {
        List<String> basisFields = resolveBasisFields(bases);
        if (basisFields.isEmpty()) {
            System.out.println("[SETTLEMENT][ORDER-SELLERS] basisFields empty -> return []");
            return List.of();
        }

        String basisCondition = basisFields.stream()
            .map(field -> "("
                + field + " is not null "
                + "and (:fromDateTime is null or " + field + " >= :fromDateTime) "
                + "and " + field + " < :toDateTimeExclusive"
                + ")")
            .collect(Collectors.joining(" or "));

        String jpql = """
            select distinct sdp.id
            from OrderItem oi
            join oi.order o
            join oi.dealerProduct dp
            join dp.sellerDealerProfile sdp
            join sdp.member m
            left join sdp.companyProfile cp
            where oi.itemProductType = 'DEALER'
              and (%s)
              and (
                    :keyword is null
                    or lower(coalesce(cp.companyName, '')) like concat('%%', :keyword, '%%')
                    or lower(coalesce(m.name, '')) like concat('%%', :keyword, '%%')
                    or lower(coalesce(m.username, '')) like concat('%%', :keyword, '%%')
                    or lower(coalesce(sdp.shopName, '')) like concat('%%', :keyword, '%%')
                    or lower(coalesce(m.mobile, '')) like concat('%%', :keyword, '%%')
                    or lower(coalesce(m.email, '')) like concat('%%', :keyword, '%%')
              )
            order by sdp.id asc
        """.formatted(basisCondition);

        List<Long> result = em.createQuery(jpql, Long.class)
            .setParameter("fromDateTime", fromDateTime)
            .setParameter("toDateTimeExclusive", toDateTimeExclusive)
            .setParameter("keyword", normalizeKeyword(keyword))
            .getResultList();

        System.out.println("[SETTLEMENT][ORDER-SELLERS] fromDateTime=" + fromDateTime
            + ", toDateTimeExclusive=" + toDateTimeExclusive
            + ", bases=" + bases
            + ", keyword=" + normalizeKeyword(keyword)
            + ", sellerDealerProfileIds=" + result);

        return result;
    }

    @Override
    public List<SettlementOrderSummarySourceDto> findDealerOrderSummaries(
        Long sellerDealerProfileId,
        SettlementBasis basis,
        LocalDateTime fromDateTime,
        LocalDateTime toDateTimeExclusive
    ) {
        String basisExpr = switch (basis) {
            case PAYMENT_COMPLETED -> "o.paidAt";
            case DELIVERY_COMPLETED -> "o.deliveredAt";
            case PURCHASE_CONFIRMED -> "o.purchaseConfirmedAt";
        };

        String jpql = """
            select new com.dev.IbioScience.dto.settlement.SettlementOrderSummarySourceDto(
                o.id,
                o.orderNo,
                %s,
                coalesce(sum(coalesce(oi.linePrice, 0)), 0),
                count(oi.id),
                max(o.ordererName)
            )
            from OrderItem oi
            join oi.order o
            join oi.dealerProduct dp
            join dp.sellerDealerProfile sdp
            where oi.itemProductType = 'DEALER'
              and sdp.id = :sellerDealerProfileId
              and %s is not null
              and (:fromDateTime is null or %s >= :fromDateTime)
              and %s < :toDateTimeExclusive
            group by o.id, o.orderNo, %s
            order by %s asc, o.id asc
        """.formatted(
            basisExpr,
            basisExpr,
            basisExpr,
            basisExpr,
            basisExpr,
            basisExpr
        );

        List<SettlementOrderSummarySourceDto> result = em.createQuery(jpql, SettlementOrderSummarySourceDto.class)
            .setParameter("sellerDealerProfileId", sellerDealerProfileId)
            .setParameter("fromDateTime", fromDateTime)
            .setParameter("toDateTimeExclusive", toDateTimeExclusive)
            .getResultList();

        System.out.println("[SETTLEMENT][ORDER-SUMMARY] sellerDealerProfileId=" + sellerDealerProfileId
            + ", basis=" + basis
            + ", fromDateTime=" + fromDateTime
            + ", toDateTimeExclusive=" + toDateTimeExclusive
            + ", resultCount=" + result.size());

        for (SettlementOrderSummarySourceDto row : result) {
            System.out.println("[SETTLEMENT][ORDER-SUMMARY-ROW] sellerDealerProfileId=" + sellerDealerProfileId
                + ", orderId=" + row.getOrderId()
                + ", orderNo=" + row.getOrderNo()
                + ", basisDate=" + row.getBasisDate()
                + ", dealerAmount=" + row.getDealerAmount()
                + ", dealerItemCount=" + row.getDealerItemCount()
                + ", ordererName=" + row.getOrdererName());
        }

        return result;
    }

    private List<String> resolveBasisFields(List<SettlementBasis> bases) {
        if (bases == null || bases.isEmpty()) {
            return List.of("o.paidAt", "o.deliveredAt", "o.purchaseConfirmedAt");
        }

        List<String> fields = new ArrayList<>();
        for (SettlementBasis basis : bases) {
            if (basis == null) {
                continue;
            }

            switch (basis) {
                case PAYMENT_COMPLETED -> fields.add("o.paidAt");
                case DELIVERY_COMPLETED -> fields.add("o.deliveredAt");
                case PURCHASE_CONFIRMED -> fields.add("o.purchaseConfirmedAt");
            }
        }
        return fields;
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
    }
}