package com.dev.IbioScience.repository.product.dealer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.dev.IbioScience.dto.seller.product.SellerProductManagerSearchRequest;
import com.dev.IbioScience.enums.product.ProductState;
import com.dev.IbioScience.enums.product.dealer.SellerProductManagerDateType;
import com.dev.IbioScience.enums.product.dealer.SellerProductManagerSearchType;
import com.dev.IbioScience.model.auth.DealerCategoryPermission;
import com.dev.IbioScience.model.product.dealer.DealerMediumSmallProductCategory;
import com.dev.IbioScience.model.product.dealer.DealerProduct;
import com.dev.IbioScience.model.product.dealer.DealerProductKeyword;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Repository
public class DealerProductRepositoryImpl implements DealerProductRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<DealerProduct> searchSellerProductPage(
            Long sellerDealerProfileId,
            SellerProductManagerSearchRequest condition,
            Pageable pageable
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DealerProduct> contentQuery = cb.createQuery(DealerProduct.class);
        Root<DealerProduct> contentRoot = contentQuery.from(DealerProduct.class);

        List<Predicate> contentPredicates = buildPredicates(
                sellerDealerProfileId,
                condition,
                cb,
                contentQuery,
                contentRoot
        );

        contentQuery.select(contentRoot)
                    .where(contentPredicates.toArray(new Predicate[0]));

        applySort(pageable, cb, contentQuery, contentRoot);

        TypedQuery<DealerProduct> typedQuery = em.createQuery(contentQuery);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<DealerProduct> content = typedQuery.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<DealerProduct> countRoot = countQuery.from(DealerProduct.class);

        List<Predicate> countPredicates = buildPredicates(
                sellerDealerProfileId,
                condition,
                cb,
                countQuery,
                countRoot
        );

        countQuery.select(cb.countDistinct(countRoot))
                  .where(countPredicates.toArray(new Predicate[0]));

        Long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    private List<Predicate> buildPredicates(
            Long sellerDealerProfileId,
            SellerProductManagerSearchRequest condition,
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<DealerProduct> root
    ) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(root.get("sellerDealerProfile").get("id"), sellerDealerProfileId));
        predicates.add(cb.equal(root.get("state"), ProductState.NORMAL));

        addDatePredicate(condition, cb, predicates, root);
        addDisplayStatusPredicate(condition, predicates, root);
        addSaleStatusPredicate(condition, predicates, root);
        addTextSearchPredicate(condition, cb, query, predicates, root);

        predicates.add(buildAuthorizedCategoryExistsPredicate(
                sellerDealerProfileId,
                condition,
                cb,
                query,
                root
        ));

        return predicates;
    }

    private void addDatePredicate(
            SellerProductManagerSearchRequest condition,
            CriteriaBuilder cb,
            List<Predicate> predicates,
            Root<DealerProduct> root
    ) {
        SellerProductManagerDateType dateType =
                condition.getDateType() == null ? SellerProductManagerDateType.CREATED_AT : condition.getDateType();

        Path<LocalDateTime> datePath =
                dateType == SellerProductManagerDateType.UPDATED_AT
                        ? root.get("updatedAt")
                        : root.get("createdAt");

        if (condition.getStartDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(datePath, condition.getStartDate().atStartOfDay()));
        }

        if (condition.getEndDate() != null) {
            predicates.add(cb.lessThan(datePath, condition.getEndDate().plusDays(1).atStartOfDay()));
        }
    }

    private void addDisplayStatusPredicate(
            SellerProductManagerSearchRequest condition,
            List<Predicate> predicates,
            Root<DealerProduct> root
    ) {
        if (condition.getDisplayStatuses() != null && !condition.getDisplayStatuses().isEmpty()) {
            predicates.add(root.get("displayStatus").in(condition.getDisplayStatuses()));
        }
    }

    private void addSaleStatusPredicate(
            SellerProductManagerSearchRequest condition,
            List<Predicate> predicates,
            Root<DealerProduct> root
    ) {
        if (condition.getSaleStatuses() != null && !condition.getSaleStatuses().isEmpty()) {
            predicates.add(root.get("saleStatus").in(condition.getSaleStatuses()));
        }
    }

    private void addTextSearchPredicate(
            SellerProductManagerSearchRequest condition,
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            List<Predicate> predicates,
            Root<DealerProduct> root
    ) {
        String keyword = condition.getTrimmedKeyword();
        if (!StringUtils.hasText(keyword)) {
            return;
        }

        String likeKeyword = "%" + keyword.toUpperCase() + "%";
        SellerProductManagerSearchType searchType =
                condition.getSearchType() == null ? SellerProductManagerSearchType.PRODUCT_NAME : condition.getSearchType();

        if (searchType == SellerProductManagerSearchType.KEYWORD) {
            Subquery<Long> keywordSubquery = query.subquery(Long.class);
            Root<DealerProductKeyword> dpk = keywordSubquery.from(DealerProductKeyword.class);

            keywordSubquery.select(cb.literal(1L))
                    .where(
                            cb.equal(dpk.get("dealerProduct"), root),
                            cb.like(cb.upper(dpk.get("keyword").get("word")), likeKeyword)
                    );

            predicates.add(cb.exists(keywordSubquery));
            return;
        }

        predicates.add(cb.like(cb.upper(root.get("name")), likeKeyword));
    }

    private Predicate buildAuthorizedCategoryExistsPredicate(
            Long sellerDealerProfileId,
            SellerProductManagerSearchRequest condition,
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<DealerProduct> root
    ) {
        Subquery<Long> subquery = query.subquery(Long.class);

        Root<DealerMediumSmallProductCategory> categoryMapping = subquery.from(DealerMediumSmallProductCategory.class);
        Root<DealerCategoryPermission> permission = subquery.from(DealerCategoryPermission.class);

        Path<Long> largeIdPath = categoryMapping.get("medium").get("large").get("id");
        Path<Long> mediumIdPath = categoryMapping.get("medium").get("id");
        Path<Long> smallIdPath = categoryMapping.get("small").get("id");

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(categoryMapping.get("dealerProduct"), root));

        predicates.add(cb.equal(permission.get("sellerDealerProfile").get("id"), sellerDealerProfileId));
        predicates.add(cb.equal(permission.get("large").get("id"), largeIdPath));
        predicates.add(
                cb.or(
                        cb.isNull(permission.get("medium")),
                        cb.equal(permission.get("medium").get("id"), mediumIdPath)
                )
        );
        predicates.add(
                cb.or(
                        cb.isNull(permission.get("small")),
                        cb.equal(permission.get("small").get("id"), smallIdPath)
                )
        );

        if (condition.getLargeId() != null) {
            predicates.add(cb.equal(largeIdPath, condition.getLargeId()));
        }

        if (condition.getMediumId() != null) {
            predicates.add(cb.equal(mediumIdPath, condition.getMediumId()));
        }

        if (condition.getSmallId() != null) {
            predicates.add(cb.equal(smallIdPath, condition.getSmallId()));
        }

        subquery.select(cb.literal(1L))
                .where(predicates.toArray(new Predicate[0]));

        return cb.exists(subquery);
    }

    private void applySort(
            Pageable pageable,
            CriteriaBuilder cb,
            CriteriaQuery<DealerProduct> query,
            Root<DealerProduct> root
    ) {
        List<Order> orders = new ArrayList<>();

        if (pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
                if (order.isAscending()) {
                    orders.add(cb.asc(root.get(order.getProperty())));
                } else {
                    orders.add(cb.desc(root.get(order.getProperty())));
                }
            }
        } else {
            orders.add(cb.desc(root.get("id")));
        }

        query.orderBy(orders);
    }
}