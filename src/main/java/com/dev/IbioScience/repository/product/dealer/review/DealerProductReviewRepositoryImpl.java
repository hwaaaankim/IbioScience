package com.dev.IbioScience.repository.product.dealer.review;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.dev.IbioScience.dto.seller.product.review.DealerProductReviewAdminRowDto;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

public class DealerProductReviewRepositoryImpl implements DealerProductReviewRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<DealerProductReviewAdminRowDto> searchAdminReviewPage(
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable,
            String sortField,
            String sortDir
    ) {
        StringBuilder fromWhere = new StringBuilder();
        fromWhere.append(" from DealerProductReview r ");
        fromWhere.append(" join r.dealerProduct dp ");
        fromWhere.append(" left join Member m on m.id = r.memberId ");
        fromWhere.append(" where 1 = 1 ");

        if (fromDate != null) {
            fromWhere.append(" and r.createdAt >= :fromDateTime ");
        }

        if (toDate != null) {
            fromWhere.append(" and r.createdAt < :toDateTime ");
        }

        StringBuilder select = new StringBuilder();
        select.append("select new com.dev.IbioScience.dto.seller.product.review.DealerProductReviewAdminRowDto(");
        select.append(" r.id, ");
        select.append(" dp.id, ");
        select.append(" m.username, ");
        select.append(" r.memberId, ");
        select.append(" r.memberDisplayName, ");
        select.append(" r.rating, ");
        select.append(" r.content, ");
        select.append(" r.createdAt, ");
        select.append(" r.updatedAt ");
        select.append(") ");
        select.append(fromWhere);
        select.append(" order by ");
        select.append(resolveSortExpression(sortField));
        select.append(" ");
        select.append(resolveSortDirection(sortDir));
        select.append(", r.id desc ");

        TypedQuery<DealerProductReviewAdminRowDto> contentQuery =
                em.createQuery(select.toString(), DealerProductReviewAdminRowDto.class);

        applyParameters(contentQuery, fromDate, toDate);
        contentQuery.setFirstResult((int) pageable.getOffset());
        contentQuery.setMaxResults(pageable.getPageSize());

        List<DealerProductReviewAdminRowDto> content = contentQuery.getResultList();

        String countJpql = "select count(r.id) " + fromWhere;
        TypedQuery<Long> countQuery = em.createQuery(countJpql, Long.class);
        applyParameters(countQuery, fromDate, toDate);

        Long total = countQuery.getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    private void applyParameters(TypedQuery<?> query, LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null) {
            LocalDateTime fromDateTime = fromDate.atStartOfDay();
            query.setParameter("fromDateTime", fromDateTime);
        }

        if (toDate != null) {
            LocalDateTime toDateTime = toDate.plusDays(1).atStartOfDay();
            query.setParameter("toDateTime", toDateTime);
        }
    }

    private String resolveSortExpression(String sortField) {
        if ("reviewerId".equalsIgnoreCase(sortField)) {
            return "m.username";
        }
        if ("rating".equalsIgnoreCase(sortField)) {
            return "r.rating";
        }
        return "r.createdAt";
    }

    private String resolveSortDirection(String sortDir) {
        return "asc".equalsIgnoreCase(sortDir) ? "asc" : "desc";
    }
}