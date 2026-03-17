package com.dev.IbioScience.repository.estimate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.dev.IbioScience.dto.estimate.EstimateProductRowDto;
import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.ProductState;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Repository
public class EstimateProductQueryRepositoryImpl implements EstimateProductQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<EstimateProductRowDto> searchProducts(
            Long largeId,
            Long mediumId,
            Long smallId,
            String productKeyword,
            String brandKeyword
    ) {
        StringBuilder jpql = new StringBuilder();
        jpql.append("select new EstimateProductRowDto(")
            .append(" map.id, ")
            .append(" p.id, ")
            .append(" lg.id, lg.name, ")
            .append(" md.id, md.name, ")
            .append(" sm.id, sm.name, ")
            .append(" b.id, coalesce(b.name, ''), ")
            .append(" p.name, p.code ")
            .append(") ")
            .append("from MediumSmallProductCategory map ")
            .append("join map.product p ")
            .append("join map.medium md ")
            .append("join md.large lg ")
            .append("join map.small sm ")
            .append("left join p.brand b ")
            .append("where p.state = :productState ")
            .append("and p.displayStatus = :displayStatus ");

        if (largeId != null) {
            jpql.append("and lg.id = :largeId ");
        }
        if (mediumId != null) {
            jpql.append("and md.id = :mediumId ");
        }
        if (smallId != null) {
            jpql.append("and sm.id = :smallId ");
        }
        if (StringUtils.hasText(productKeyword)) {
            jpql.append("and lower(p.name) like :productKeyword ");
        }
        if (StringUtils.hasText(brandKeyword)) {
            jpql.append("and lower(coalesce(b.name, '')) like :brandKeyword ");
        }

        jpql.append("order by lg.name asc, md.name asc, sm.name asc, p.name asc, map.id asc");

        TypedQuery<EstimateProductRowDto> query = em.createQuery(jpql.toString(), EstimateProductRowDto.class);
        bindCommonConditions(query, largeId, mediumId, smallId, productKeyword, brandKeyword);

        return query.getResultList();
    }

    @Override
    public List<String> searchBrandSuggestions(
            Long largeId,
            Long mediumId,
            Long smallId,
            String productKeyword,
            String brandKeyword,
            int limit
    ) {
        StringBuilder jpql = new StringBuilder();
        jpql.append("select distinct b.name ")
            .append("from Brand b ")
            .append("where trim(coalesce(b.name, '')) <> '' ");

        if (StringUtils.hasText(brandKeyword)) {
            jpql.append("and lower(b.name) like :brandKeyword ");
        }

        jpql.append("order by b.name asc");

        TypedQuery<String> query = em.createQuery(jpql.toString(), String.class);

        if (StringUtils.hasText(brandKeyword)) {
            query.setParameter("brandKeyword", "%" + brandKeyword.trim().toLowerCase(Locale.ROOT) + "%");
        }

        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public List<String> searchProductSuggestions(
            Long largeId,
            Long mediumId,
            Long smallId,
            String productKeyword,
            String brandKeyword,
            int limit
    ) {
        StringBuilder jpql = new StringBuilder();
        jpql.append("select distinct p.name ")
            .append("from MediumSmallProductCategory map ")
            .append("join map.product p ")
            .append("join map.medium md ")
            .append("join md.large lg ")
            .append("join map.small sm ")
            .append("left join p.brand b ")
            .append("where p.state = :productState ")
            .append("and p.displayStatus = :displayStatus ")
            .append("and trim(coalesce(p.name, '')) <> '' ");

        if (largeId != null) {
            jpql.append("and lg.id = :largeId ");
        }
        if (mediumId != null) {
            jpql.append("and md.id = :mediumId ");
        }
        if (smallId != null) {
            jpql.append("and sm.id = :smallId ");
        }
        if (StringUtils.hasText(productKeyword)) {
            jpql.append("and lower(p.name) like :productKeyword ");
        }
        if (StringUtils.hasText(brandKeyword)) {
            jpql.append("and lower(coalesce(b.name, '')) like :brandKeyword ");
        }

        jpql.append("order by p.name asc");

        TypedQuery<String> query = em.createQuery(jpql.toString(), String.class);
        bindCommonConditions(query, largeId, mediumId, smallId, productKeyword, brandKeyword);
        query.setMaxResults(limit);

        return query.getResultList();
    }

    @Override
    public List<EstimateProductRowDto> findInitialSelectedItems(Long productId, Long mappingId) {
        if (productId == null && mappingId == null) {
            return new ArrayList<>();
        }

        StringBuilder jpql = new StringBuilder();
        jpql.append("select new EstimateProductRowDto(")
            .append(" map.id, ")
            .append(" p.id, ")
            .append(" lg.id, lg.name, ")
            .append(" md.id, md.name, ")
            .append(" sm.id, sm.name, ")
            .append(" b.id, coalesce(b.name, ''), ")
            .append(" p.name, p.code ")
            .append(") ")
            .append("from MediumSmallProductCategory map ")
            .append("join map.product p ")
            .append("join map.medium md ")
            .append("join md.large lg ")
            .append("join map.small sm ")
            .append("left join p.brand b ")
            .append("where p.state = :productState ")
            .append("and p.displayStatus = :displayStatus ");

        if (mappingId != null) {
            jpql.append("and map.id = :mappingId ");
        } else if (productId != null) {
            jpql.append("and p.id = :productId ");
        }

        jpql.append("order by map.id asc");

        TypedQuery<EstimateProductRowDto> query = em.createQuery(jpql.toString(), EstimateProductRowDto.class);
        query.setParameter("productState", ProductState.NORMAL);
        query.setParameter("displayStatus", DisplayStatus.ON);

        if (mappingId != null) {
            query.setParameter("mappingId", mappingId);
        } else if (productId != null) {
            query.setParameter("productId", productId);
            query.setMaxResults(1);
        }

        return query.getResultList();
    }

    private void bindCommonConditions(
            TypedQuery<?> query,
            Long largeId,
            Long mediumId,
            Long smallId,
            String productKeyword,
            String brandKeyword
    ) {
        query.setParameter("productState", ProductState.NORMAL);
        query.setParameter("displayStatus", DisplayStatus.ON);

        if (largeId != null) {
            query.setParameter("largeId", largeId);
        }
        if (mediumId != null) {
            query.setParameter("mediumId", mediumId);
        }
        if (smallId != null) {
            query.setParameter("smallId", smallId);
        }
        if (StringUtils.hasText(productKeyword)) {
            query.setParameter("productKeyword", "%" + productKeyword.trim().toLowerCase(Locale.ROOT) + "%");
        }
        if (StringUtils.hasText(brandKeyword)) {
            query.setParameter("brandKeyword", "%" + brandKeyword.trim().toLowerCase(Locale.ROOT) + "%");
        }
    }
}