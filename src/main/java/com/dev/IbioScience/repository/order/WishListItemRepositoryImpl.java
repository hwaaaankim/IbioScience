package com.dev.IbioScience.repository.order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.dev.IbioScience.dto.admin.wishList.AdminClientWishListSearchCondition;
import com.dev.IbioScience.dto.admin.wishList.WishListProductCategoryPathRow;
import com.dev.IbioScience.model.order.WishListItem;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.category.CategoryLarge;
import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;
import com.dev.IbioScience.model.product.relation.MediumSmallProductCategory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Repository
public class WishListItemRepositoryImpl implements WishListItemRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Long> searchWishListItemIds(Long memberId, AdminClientWishListSearchCondition condition, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> idQuery = cb.createQuery(Long.class);
        Root<WishListItem> root = idQuery.from(WishListItem.class);
        Join<WishListItem, Product> productJoin = root.join("product", JoinType.INNER);

        List<Predicate> predicates = buildPredicates(memberId, condition, idQuery, cb, root, productJoin);

        idQuery.select(root.get("id"))
               .where(predicates.toArray(new Predicate[0]))
               .distinct(true)
               .orderBy(
                   cb.desc(root.get("createdAt")),
                   cb.desc(root.get("id"))
               );

        TypedQuery<Long> typedIdQuery = entityManager.createQuery(idQuery);
        typedIdQuery.setFirstResult((int) pageable.getOffset());
        typedIdQuery.setMaxResults(pageable.getPageSize());

        List<Long> ids = typedIdQuery.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<WishListItem> countRoot = countQuery.from(WishListItem.class);
        Join<WishListItem, Product> countProductJoin = countRoot.join("product", JoinType.INNER);

        List<Predicate> countPredicates = buildPredicates(memberId, condition, countQuery, cb, countRoot, countProductJoin);

        countQuery.select(cb.countDistinct(countRoot.get("id")))
                  .where(countPredicates.toArray(new Predicate[0]));

        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(ids, pageable, total);
    }

    @Override
    public List<WishListProductCategoryPathRow> findCategoryPathRowsByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }

        return entityManager.createQuery("""
            select new WishListProductCategoryPathRow(
                msp.product.id,
                l.name,
                m.name,
                s.name
            )
            from MediumSmallProductCategory msp
            join msp.medium m
            join m.large l
            join msp.small s
            where msp.product.id in :productIds
            order by msp.product.id asc, l.name asc, m.name asc, s.name asc
        """, WishListProductCategoryPathRow.class)
        .setParameter("productIds", productIds)
        .getResultList();
    }

    private List<Predicate> buildPredicates(
            Long memberId,
            AdminClientWishListSearchCondition condition,
            CommonAbstractCriteria criteria,
            CriteriaBuilder cb,
            Root<WishListItem> root,
            Join<WishListItem, Product> productJoin) {

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(root.get("member").get("id"), memberId));

        LocalDateTime fromDateTime = null;
        LocalDateTime toExclusiveDateTime = null;

        if (condition.getFromDate() != null) {
            fromDateTime = condition.getFromDate().atStartOfDay();
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDateTime));
        }

        if (condition.getToDate() != null) {
            toExclusiveDateTime = condition.getToDate().plusDays(1).atStartOfDay();
            predicates.add(cb.lessThan(root.get("createdAt"), toExclusiveDateTime));
        }

        if (StringUtils.hasText(condition.getProductName())) {
            String keyword = "%" + condition.getProductName().trim().toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(productJoin.get("name")), keyword));
        }

        if (condition.getLargeId() != null || condition.getMediumId() != null || condition.getSmallId() != null) {
            Subquery<Long> categorySubquery = criteria.subquery(Long.class);
            Root<MediumSmallProductCategory> mappingRoot = categorySubquery.from(MediumSmallProductCategory.class);
            Join<MediumSmallProductCategory, CategoryMedium> mediumJoin = mappingRoot.join("medium", JoinType.INNER);
            Join<CategoryMedium, CategoryLarge> largeJoin = mediumJoin.join("large", JoinType.INNER);
            Join<MediumSmallProductCategory, CategorySmall> smallJoin = mappingRoot.join("small", JoinType.INNER);

            List<Predicate> categoryPredicates = new ArrayList<>();
            categoryPredicates.add(cb.equal(mappingRoot.get("product").get("id"), productJoin.get("id")));

            if (condition.getLargeId() != null) {
                categoryPredicates.add(cb.equal(largeJoin.get("id"), condition.getLargeId()));
            }

            if (condition.getMediumId() != null) {
                categoryPredicates.add(cb.equal(mediumJoin.get("id"), condition.getMediumId()));
            }

            if (condition.getSmallId() != null) {
                categoryPredicates.add(cb.equal(smallJoin.get("id"), condition.getSmallId()));
            }

            categorySubquery.select(cb.literal(1L))
                            .where(categoryPredicates.toArray(new Predicate[0]));

            predicates.add(cb.exists(categorySubquery));
        }

        return predicates;
    }
}