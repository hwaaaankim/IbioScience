package com.dev.IbioScience.repository.auth;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.dev.IbioScience.dto.admin.client.ClientListRowDto;
import com.dev.IbioScience.dto.admin.client.ClientSearchCondition;
import com.dev.IbioScience.enums.auth.CustomerType;
import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.enums.auth.MemberRole;
import com.dev.IbioScience.model.auth.BuyerDealerProfile;
import com.dev.IbioScience.model.auth.CompanyProfile;
import com.dev.IbioScience.model.auth.Member;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Repository
public class MemberClientSearchRepositoryImpl implements MemberClientSearchRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<ClientListRowDto> searchClients(ClientSearchCondition cond, Pageable pageable, String sortKey, String sortDir) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        // =========================
        // 1) content query
        // =========================
        CriteriaQuery<ClientListRowDto> cq = cb.createQuery(ClientListRowDto.class);
        Root<Member> m = cq.from(Member.class);

        // LEFT JOIN companyProfile
        Join<Member, CompanyProfile> cp = m.join("companyProfile", JoinType.LEFT);

        // LEFT JOIN orders
        Join<Member, com.dev.IbioScience.model.order.Order> o = m.join("orders", JoinType.LEFT);

        // ✅ 주문 집계 기간 조건(LEFT JOIN 유지 위해 ON 적용)
        Predicate orderDateOn = buildOrderDatePredicate(cb, o, cond);
        o.on(orderDateOn);

        // SUM / COUNT
        Expression<Long> sumGrandTotal = cb.coalesce(cb.sum(o.<Long>get("grandTotal")), 0L);
        Expression<Long> countOrders = cb.coalesce(cb.countDistinct(o.get("id")), 0L);

        // 최근 주문번호(기간 내 최신) 서브쿼리
        Subquery<String> recentOrderNoSub = cq.subquery(String.class);
        Root<com.dev.IbioScience.model.order.Order> o2 = recentOrderNoSub.from(com.dev.IbioScience.model.order.Order.class);

        // max(createdAt) 서브쿼리
        Subquery<LocalDateTime> maxCreatedSub = cq.subquery(LocalDateTime.class);
        Root<com.dev.IbioScience.model.order.Order> o3 = maxCreatedSub.from(com.dev.IbioScience.model.order.Order.class);

        Predicate o3MemberEq = cb.equal(o3.get("member"), m);
        Predicate o3Date = buildOrderDatePredicate(cb, o3, cond);

        maxCreatedSub.select(cb.greatest(o3.<LocalDateTime>get("createdAt")));
        maxCreatedSub.where(andAll(cb, o3MemberEq, o3Date));

        Predicate o2MemberEq = cb.equal(o2.get("member"), m);
        Predicate o2Date = buildOrderDatePredicate(cb, o2, cond);
        Predicate o2MaxCreatedEq = cb.equal(o2.get("createdAt"), maxCreatedSub);

        recentOrderNoSub.select(o2.get("orderNo"));
        recentOrderNoSub.where(andAll(cb, o2MemberEq, o2Date, o2MaxCreatedEq));

        // =========================
        // ✅ 표시용 텍스트 (요구사항 반영)
        // - 등급: CustomerType 기준 (개인/기업)
        // - 회원구분: DealerType 기준 (일반소비자/구매딜러/판매딜러)
        // =========================
        Expression<String> memberGradeTextExp = buildMemberGradeTextExpression(cb, m); // CustomerType 기준
        Expression<String> memberTypeTextExp  = buildMemberTypeTextExpression(cb, m);  // DealerType 기준

        // select dto
        cq.select(cb.construct(
                ClientListRowDto.class,
                m.get("id"),
                m.get("joinedAt"),
                m.get("name"),
                m.get("username"),
                memberGradeTextExp,
                memberTypeTextExp,
                cp.get("companyName"),
                m.get("tel"),
                m.get("mobile"),
                recentOrderNoSub,
                sumGrandTotal,
                countOrders
        ));

        // where predicates (AND)
        List<Predicate> predicates = new ArrayList<>();

        // ✅ 고객만 조회 + 직원(STAFF) 제외
        predicates.add(buildCustomerOnlyPredicate(cb, m));

        // 회원상태
        if (cond.getStatus() != null) {
            predicates.add(cb.equal(m.get("status"), cond.getStatus()));
        }

        // ✅ 회원분류(체크박스) 조건
        Predicate memberTypePredicate = buildMemberTypePredicate(cb, m, cond);
        if (memberTypePredicate != null) {
            predicates.add(memberTypePredicate);
        }

        // 기업 선택 시 딜러등급 필터(BuyerDealerProfile)
        if (cond.getGrade() != null) {
            Subquery<Long> bdpExists = cq.subquery(Long.class);
            Root<BuyerDealerProfile> bdp = bdpExists.from(BuyerDealerProfile.class);
            bdpExists.select(cb.literal(1L));
            bdpExists.where(
                    cb.equal(bdp.get("member"), m),
                    cb.equal(bdp.get("grade"), cond.getGrade())
            );
            predicates.add(cb.exists(bdpExists));
        }

        // 텍스트 검색
        Predicate textPredicate = buildTextSearchPredicate(cb, m, cp, cond);
        if (textPredicate != null) {
            predicates.add(textPredicate);
        }

        // 가입일/탈퇴일 기간(회원 테이블 기준)
        Predicate memberDatePredicate = buildMemberDatePredicate(cb, m, cond);
        if (memberDatePredicate != null) {
            predicates.add(memberDatePredicate);
        }

        cq.where(predicates.toArray(new Predicate[0]));

        // group by (집계)
        cq.groupBy(
                m.get("id"),
                m.get("joinedAt"),
                m.get("name"),
                m.get("username"),
                cp.get("companyName"),
                m.get("tel"),
                m.get("mobile"),
                m.get("dealerType"),
                m.get("customerType")
        );

        // order by
        applySort(cb, cq, m, cp, sumGrandTotal, countOrders, sortKey, sortDir);

        TypedQuery<ClientListRowDto> query = em.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<ClientListRowDto> content = query.getResultList();

        // =========================
        // 2) count query
        // =========================
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<Member> mCount = countCq.from(Member.class);
        Join<Member, CompanyProfile> cpCount = mCount.join("companyProfile", JoinType.LEFT);

        List<Predicate> countPreds = new ArrayList<>();

        // ✅ 고객만 조회 + 직원(STAFF) 제외 (count에도 동일 적용)
        countPreds.add(buildCustomerOnlyPredicate(cb, mCount));

        if (cond.getStatus() != null) {
            countPreds.add(cb.equal(mCount.get("status"), cond.getStatus()));
        }

        Predicate memberTypePred2 = buildMemberTypePredicate(cb, mCount, cond);
        if (memberTypePred2 != null) {
            countPreds.add(memberTypePred2);
        }

        if (cond.getGrade() != null) {
            Subquery<Long> bdpExists2 = countCq.subquery(Long.class);
            Root<BuyerDealerProfile> bdp2 = bdpExists2.from(BuyerDealerProfile.class);
            bdpExists2.select(cb.literal(1L));
            bdpExists2.where(
                    cb.equal(bdp2.get("member"), mCount),
                    cb.equal(bdp2.get("grade"), cond.getGrade())
            );
            countPreds.add(cb.exists(bdpExists2));
        }

        Predicate textPred2 = buildTextSearchPredicate(cb, mCount, cpCount, cond);
        if (textPred2 != null) {
            countPreds.add(textPred2);
        }

        Predicate memberDatePred2 = buildMemberDatePredicate(cb, mCount, cond);
        if (memberDatePred2 != null) {
            countPreds.add(memberDatePred2);
        }

        countCq.select(cb.countDistinct(mCount.get("id")));
        countCq.where(countPreds.toArray(new Predicate[0]));

        Long total = em.createQuery(countCq).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    // =========================
    // ✅ 고객만 + 직원 제외
    // =========================
    private Predicate buildCustomerOnlyPredicate(CriteriaBuilder cb, Root<Member> m) {
        Predicate allowedRoles = m.get("role").in(
                MemberRole.USER,
                MemberRole.BUYER_DEALER,
                MemberRole.SELLER_DEALER
        );

        // STAFF 절대 제외 (null-safe)
        Predicate notStaff = cb.or(
                cb.isNull(m.get("customerType")),
                cb.notEqual(m.get("customerType"), CustomerType.STAFF)
        );

        return cb.and(allowedRoles, notStaff);
    }

    // =========================
    // Predicate builders
    // =========================

    private Predicate buildOrderDatePredicate(CriteriaBuilder cb, Path<?> orderRootOrJoin, ClientSearchCondition cond) {
        if (cond.getFrom() == null && cond.getTo() == null) {
            return cb.conjunction();
        }

        Path<LocalDateTime> createdAt = orderRootOrJoin.get("createdAt");

        if (cond.getFrom() != null && cond.getTo() != null) {
            LocalDateTime fromDt = cond.getFrom().atStartOfDay();
            LocalDateTime toDt = cond.getTo().plusDays(1).atStartOfDay(); // inclusive
            return cb.and(
                    cb.greaterThanOrEqualTo(createdAt, fromDt),
                    cb.lessThan(createdAt, toDt)
            );
        }

        if (cond.getFrom() != null) {
            LocalDateTime fromDt = cond.getFrom().atStartOfDay();
            return cb.greaterThanOrEqualTo(createdAt, fromDt);
        }

        LocalDateTime toDt = cond.getTo().plusDays(1).atStartOfDay();
        return cb.lessThan(createdAt, toDt);
    }

    /**
     * ✅ 회원분류 체크박스 조건 (기존 요구사항 유지)
     * - GENERAL        : customerType=PERSONAL  + dealerType=NONE
     * - COMPANY_BUYER  : customerType=BUSINESS  + dealerType=BUYER
     * - COMPANY_SELLER : customerType=BUSINESS  + dealerType=SELLER
     */
    private Predicate buildMemberTypePredicate(CriteriaBuilder cb, Root<Member> m, ClientSearchCondition cond) {
        if (cond.getMemberTypes() == null || cond.getMemberTypes().isEmpty()) {
            return null; // 전체
        }

        List<Predicate> ors = new ArrayList<>();

        for (ClientSearchCondition.MemberType t : cond.getMemberTypes()) {
            if (t == ClientSearchCondition.MemberType.GENERAL) {
                ors.add(cb.and(
                        cb.equal(m.get("customerType"), CustomerType.PERSONAL),
                        cb.equal(m.get("dealerType"), DealerType.NONE)
                ));
            } else if (t == ClientSearchCondition.MemberType.COMPANY_BUYER) {
                ors.add(cb.and(
                        cb.equal(m.get("customerType"), CustomerType.BUSINESS),
                        cb.equal(m.get("dealerType"), DealerType.BUYER)
                ));
            } else if (t == ClientSearchCondition.MemberType.COMPANY_SELLER) {
                ors.add(cb.and(
                        cb.equal(m.get("customerType"), CustomerType.BUSINESS),
                        cb.equal(m.get("dealerType"), DealerType.SELLER)
                ));
            }
        }

        if (ors.isEmpty()) return null;
        return cb.or(ors.toArray(new Predicate[0]));
    }

    private Predicate buildTextSearchPredicate(CriteriaBuilder cb, Root<Member> m, Join<Member, CompanyProfile> cp, ClientSearchCondition cond) {
        if (cond.getKeyword() == null || cond.getKeyword().isBlank()) return null;

        String kw = "%" + cond.getKeyword().trim() + "%";
        String field = cond.getSearchField();

        if (field == null || field.isBlank()) {
            return cb.or(
                    cb.like(m.get("mobile"), kw),
                    cb.like(m.get("username"), kw),
                    cb.like(m.get("name"), kw),
                    cb.like(m.get("tel"), kw),
                    cb.like(m.get("email"), kw),
                    cb.like(cp.get("businessRegistrationNumber"), kw)
            );
        }

        return switch (field) {
            case "mobile" -> cb.like(m.get("mobile"), kw);
            case "username" -> cb.like(m.get("username"), kw);
            case "name" -> cb.like(m.get("name"), kw);
            case "tel" -> cb.like(m.get("tel"), kw);
            case "email" -> cb.like(m.get("email"), kw);
            case "bizNo" -> cb.like(cp.get("businessRegistrationNumber"), kw);
            default -> cb.like(m.get("username"), kw);
        };
    }

    private Predicate buildMemberDatePredicate(CriteriaBuilder cb, Root<Member> m, ClientSearchCondition cond) {
        if (cond.getFrom() == null && cond.getTo() == null) return null;

        Path<LocalDateTime> target =
                (cond.getDateField() == ClientSearchCondition.DateField.WITHDREW)
                        ? m.get("withdrewAt")
                        : m.get("joinedAt");

        if (cond.getFrom() != null && cond.getTo() != null) {
            LocalDateTime fromDt = cond.getFrom().atStartOfDay();
            LocalDateTime toDt = cond.getTo().plusDays(1).atStartOfDay();
            return cb.and(
                    cb.greaterThanOrEqualTo(target, fromDt),
                    cb.lessThan(target, toDt)
            );
        }

        if (cond.getFrom() != null) {
            LocalDateTime fromDt = cond.getFrom().atStartOfDay();
            return cb.greaterThanOrEqualTo(target, fromDt);
        }

        LocalDateTime toDt = cond.getTo().plusDays(1).atStartOfDay();
        return cb.lessThan(target, toDt);
    }

    // =========================
    // ✅ 텍스트 표현식 (요구사항 기준)
    // =========================

    /**
     * ✅ 등급: CustomerType 기준 (개인/기업)
     * PERSONAL=개인회원 / BUSINESS=기업회원
     */
    private Expression<String> buildMemberGradeTextExpression(CriteriaBuilder cb, Root<Member> m) {
        return cb.<String>selectCase()
                .when(cb.equal(m.get("customerType"), CustomerType.PERSONAL), "개인회원")
                .when(cb.equal(m.get("customerType"), CustomerType.BUSINESS), "기업회원")
                // STAFF는 조회 자체가 막혀있지만 방어적으로 표기
                .when(cb.equal(m.get("customerType"), CustomerType.STAFF), "직원")
                .otherwise("-");
    }

    /**
     * ✅ 회원구분: DealerType 기준
     * NONE=일반소비자 / BUYER=구매 딜러 / SELLER=판매 딜러
     */
    private Expression<String> buildMemberTypeTextExpression(CriteriaBuilder cb, Root<Member> m) {
        return cb.<String>selectCase()
                .when(cb.equal(m.get("dealerType"), DealerType.NONE), "일반소비자")
                .when(cb.equal(m.get("dealerType"), DealerType.BUYER), "구매 딜러")
                .when(cb.equal(m.get("dealerType"), DealerType.SELLER), "판매 딜러")
                .otherwise("-");
    }

    // =========================
    // Sort
    // =========================

    private void applySort(
            CriteriaBuilder cb,
            CriteriaQuery<?> cq,
            Root<Member> m,
            Join<Member, CompanyProfile> cp,
            Expression<Long> sumGrandTotal,
            Expression<Long> countOrders,
            String sortKey,
            String sortDir
    ) {
        String key = (sortKey == null) ? "" : sortKey.trim();
        boolean asc = "asc".equalsIgnoreCase(sortDir);

        if ("pageRank".equals(key)) key = "totalOrderAmount";

        if (key.isEmpty()) {
            cq.orderBy(cb.desc(m.get("joinedAt")));
            return;
        }

        Expression<?> exp = switch (key) {
            case "joinedAt" -> m.get("joinedAt");
            case "name" -> m.get("name");
            case "username" -> m.get("username");
            case "companyName" -> cp.get("companyName");

            // ✅ 요구사항 반영: 등급 정렬 = CustomerType
            case "memberGrade" -> m.get("customerType");

            // ✅ 요구사항 반영: 회원구분 정렬 = DealerType
            case "memberType" -> m.get("dealerType");

            case "totalOrderAmount" -> sumGrandTotal;
            case "totalOrderCount" -> countOrders;
            default -> m.get("joinedAt");
        };

        List<jakarta.persistence.criteria.Order> orders = new ArrayList<>();
        orders.add(asc ? cb.asc(exp) : cb.desc(exp));
        orders.add(cb.desc(m.get("joinedAt")));

        cq.orderBy(orders);
    }

    // =========================
    // utils
    // =========================

    private Predicate andAll(CriteriaBuilder cb, Predicate... preds) {
        List<Predicate> list = new ArrayList<>();
        if (preds != null) {
            for (Predicate p : preds) {
                if (p != null) list.add(p);
            }
        }
        if (list.isEmpty()) return cb.conjunction();
        return cb.and(list.toArray(new Predicate[0]));
    }
}