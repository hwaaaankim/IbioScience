package com.dev.IbioScience.repository.estimate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.dev.IbioScience.dto.estimate.admin.AdminEstimateListRowDto;
import com.dev.IbioScience.dto.estimate.admin.AdminEstimateListSearchRequest;
import com.dev.IbioScience.enums.estimate.admin.AdminEstimateDateType;
import com.dev.IbioScience.enums.estimate.admin.AdminEstimateProgressFilter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

@Repository
public class EstimateRepositoryImpl implements EstimateRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<AdminEstimateListRowDto> searchAdminEstimateList(Long memberId, AdminEstimateListSearchRequest request) {
        StringBuilder dataJpql = new StringBuilder();
        StringBuilder countJpql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        dataJpql.append("select new AdminEstimateListRowDto(")
                .append(" e.id, ")
                .append(" m.id, ")
                .append(" m.username, ")
                .append(" m.email, ")
                .append(" m.mobile, ")
                .append(" m.name, ")
                .append(" count(i.id), ")
                .append(" e.checkStatus, ")
                .append(" e.answerStatus, ")
                .append(" e.requestedAt, ")
                .append(" e.checkedAt, ")
                .append(" e.answeredAt, ")
                .append(" e.title ")
                .append(") ")
                .append("from Estimate e ")
                .append("join e.member m ")
                .append("left join e.items i ")
                .append("where m.id = :memberId ");

        countJpql.append("select count(e.id) ")
                .append("from Estimate e ")
                .append("join e.member m ")
                .append("where m.id = :memberId ");

        params.put("memberId", memberId);

        appendSearchCondition(dataJpql, countJpql, params, request);
        appendDateCondition(dataJpql, countJpql, params, request);
        appendProgressCondition(dataJpql, countJpql, request);

        dataJpql.append("group by ")
                .append(" e.id, ")
                .append(" m.id, m.username, m.email, m.mobile, m.name, ")
                .append(" e.checkStatus, e.answerStatus, ")
                .append(" e.requestedAt, e.checkedAt, e.answeredAt, e.title ")
                .append("order by e.requestedAt desc, e.id desc");

        TypedQuery<AdminEstimateListRowDto> dataQuery = em.createQuery(dataJpql.toString(), AdminEstimateListRowDto.class);
        Query totalQuery = em.createQuery(countJpql.toString());

        bindParameters(dataQuery, params);
        bindParameters(totalQuery, params);

        dataQuery.setFirstResult(request.getOffset());
        dataQuery.setMaxResults(request.getSizeValue());

        List<AdminEstimateListRowDto> content = dataQuery.getResultList();
        Long total = (Long) totalQuery.getSingleResult();

        return new PageImpl<>(content, PageRequest.of(request.getPageValue() - 1, request.getSizeValue()), total);
    }

    private void appendSearchCondition(
            StringBuilder dataJpql,
            StringBuilder countJpql,
            Map<String, Object> params,
            AdminEstimateListSearchRequest request
    ) {
        String keyword = request.getNormalizedKeyword();
        if (!StringUtils.hasText(keyword) || request.getSearchType() == null) {
            return;
        }

        String fieldExpression;
        switch (request.getSearchType()) {
            case USER_ID:
                fieldExpression = "m.username";
                break;
            case EMAIL:
                fieldExpression = "m.email";
                break;
            case CONTACT_PHONE:
                fieldExpression = "m.mobile";
                break;
            case NAME:
                fieldExpression = "m.name";
                break;
            default:
                fieldExpression = "m.username";
                break;
        }

        dataJpql.append(" and ").append(fieldExpression).append(" like :keyword ");
        countJpql.append(" and ").append(fieldExpression).append(" like :keyword ");
        params.put("keyword", "%" + keyword + "%");
    }

    private void appendDateCondition(
            StringBuilder dataJpql,
            StringBuilder countJpql,
            Map<String, Object> params,
            AdminEstimateListSearchRequest request
    ) {
        String dateField = resolveDateField(request.getDateType());

        LocalDateTime fromDateTime = request.getFromDateTime();
        LocalDateTime toDateTime = request.getToDateTime();

        if (fromDateTime != null) {
            dataJpql.append(" and ").append(dateField).append(" >= :fromDateTime ");
            countJpql.append(" and ").append(dateField).append(" >= :fromDateTime ");
            params.put("fromDateTime", fromDateTime);
        }

        if (toDateTime != null) {
            dataJpql.append(" and ").append(dateField).append(" <= :toDateTime ");
            countJpql.append(" and ").append(dateField).append(" <= :toDateTime ");
            params.put("toDateTime", toDateTime);
        }
    }

    private void appendProgressCondition(
            StringBuilder dataJpql,
            StringBuilder countJpql,
            AdminEstimateListSearchRequest request
    ) {
        List<AdminEstimateProgressFilter> states = request.getNormalizedStates();
        if (states == null || states.isEmpty()) {
            return;
        }

        dataJpql.append(" and (");
        countJpql.append(" and (");

        for (int i = 0; i < states.size(); i++) {
            AdminEstimateProgressFilter state = states.get(i);

            if (i > 0) {
                dataJpql.append(" or ");
                countJpql.append(" or ");
            }

            String condition = resolveProgressCondition(state);
            dataJpql.append(condition);
            countJpql.append(condition);
        }

        dataJpql.append(") ");
        countJpql.append(") ");
    }

    private String resolveDateField(AdminEstimateDateType dateType) {
        if (dateType == null) {
            return "e.requestedAt";
        }

        switch (dateType) {
            case CHECKED_AT:
                return "e.checkedAt";
            case ANSWERED_AT:
                return "e.answeredAt";
            case REQUESTED_AT:
            default:
                return "e.requestedAt";
        }
    }

    private String resolveProgressCondition(AdminEstimateProgressFilter state) {
        switch (state) {
            case UNCHECKED:
                return "(e.checkStatus = 'UNCHECKED')";
            case CHECKED:
                return "(e.checkStatus = 'CHECKED' and e.answerStatus = 'WAITING')";
            case ANSWERED:
                return "(e.answerStatus = 'ANSWERED')";
            default:
                return "(1 = 1)";
        }
    }

    private void bindParameters(Query query, Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }
}