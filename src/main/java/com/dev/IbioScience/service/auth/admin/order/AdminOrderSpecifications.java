package com.dev.IbioScience.service.auth.admin.order;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.dev.IbioScience.dto.admin.order.AdminOrderSearchRequest;
import com.dev.IbioScience.model.auth.CompanyProfile;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.SellerDealerProfile;
import com.dev.IbioScience.model.order.Order;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public final class AdminOrderSpecifications {

    private AdminOrderSpecifications() {
    }

    public static Specification<Order> search(AdminOrderSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<Order, Member> memberJoin = root.join("member", JoinType.INNER);
            Join<Member, CompanyProfile> companyJoin = memberJoin.join("companyProfile", JoinType.LEFT);
            Join<Member, SellerDealerProfile> sellerJoin = memberJoin.join("sellerDealerProfile", JoinType.LEFT);

            // 주문일
            if (req.getFromDate() != null) {
                LocalDateTime from = req.getFromDate().atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }

            if (req.getToDate() != null) {
                LocalDateTime to = LocalDateTime.of(req.getToDate(), LocalTime.MAX);
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            // 주문자 분류
            if (req.hasAnyDealerType()) {
                predicates.add(memberJoin.get("dealerType").in(req.getDealerTypes()));
            } else {
                predicates.add(cb.disjunction());
            }

            // 주문상태
            if (req.hasAnyOrderStatus()) {
                predicates.add(root.get("status").in(req.getOrderStatuses()));
            } else {
                predicates.add(cb.disjunction());
            }

            // 결제수단
            if (req.hasAnyPaymentMethod()) {
                predicates.add(root.get("paymentMethod").in(req.getPaymentMethods()));
            } else {
                predicates.add(cb.disjunction());
            }

            // 배송방법
            if (req.hasAnyShippingMethod()) {
                predicates.add(root.get("shippingMethod").in(req.getShippingMethods()));
            } else {
                predicates.add(cb.disjunction());
            }

            // 배송비 구분
            if (req.hasAnyShippingPayType()) {
                predicates.add(root.get("shippingPayType").in(req.getShippingPayTypes()));
            } else {
                predicates.add(cb.disjunction());
            }

            // 검색어
            if (StringUtils.hasText(req.getKeyword()) && StringUtils.hasText(req.getKeywordType())) {
                String keyword = "%" + req.getKeyword().trim() + "%";

                switch (req.getKeywordType()) {
                    case "MEMBER_NAME" -> predicates.add(cb.like(memberJoin.get("name"), keyword));
                    case "COMPANY_NAME" -> predicates.add(cb.like(companyJoin.get("companyName"), keyword));
                    case "SHOP_NAME" -> predicates.add(cb.like(sellerJoin.get("shopName"), keyword));
                    case "MOBILE" -> predicates.add(cb.like(memberJoin.get("mobile"), keyword));
                    case "EMAIL" -> predicates.add(cb.like(memberJoin.get("email"), keyword));
                    default -> {
                    }
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}