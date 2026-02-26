package com.dev.IbioScience.repository.auth.admin.client;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.dev.IbioScience.dto.customer.auth.ClientApplyDetailDto;
import com.dev.IbioScience.dto.customer.auth.ClientApplyRowDto;
import com.dev.IbioScience.dto.customer.auth.ClientApplySearchCondition;
import com.dev.IbioScience.dto.customer.auth.ClientApplySearchCondition.ApplyType;
import com.dev.IbioScience.dto.customer.auth.ClientApplySearchCondition.SearchField;
import com.dev.IbioScience.enums.auth.CustomerType;
import com.dev.IbioScience.enums.auth.MemberDomain;
import com.dev.IbioScience.enums.auth.MemberStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Repository
public class ClientApplyManagerRepository {

    @PersistenceContext
    private EntityManager em;

    public Page<ClientApplyRowDto> searchPending(ClientApplySearchCondition cond, String sortKey, String sortDir, Pageable pageable) {

        StringBuilder jpql = new StringBuilder();
        jpql.append("select new com.dev.IbioScience.dto.customer.auth.ClientApplyRowDto(")
            .append(" m.id, m.username, cp.companyName, m.name, m.mobile, m.tel, m.joinedAt, m.customerType")
            .append(") ")
            .append("from Member m ")
            .append("left join m.companyProfile cp ")
            .append("where m.status = :pending ")
            .append("and m.domain = :domain ");

        Map<String, Object> params = new HashMap<>();
        params.put("pending", MemberStatus.PENDING);
        params.put("domain", MemberDomain.CUSTOMER);

        // applyType: ALL / PERSONAL / BUSINESS
        if (cond != null && cond.getApplyType() != null && cond.getApplyType() != ApplyType.ALL) {
            if (cond.getApplyType() == ApplyType.PERSONAL) {
                jpql.append("and m.customerType = :ct ");
                params.put("ct", CustomerType.PERSONAL);
            } else if (cond.getApplyType() == ApplyType.BUSINESS) {
                jpql.append("and m.customerType = :ct ");
                params.put("ct", CustomerType.BUSINESS);
            }
        } else {
            // 기본: 신청관리에서는 STAFF 제외, PERSONAL/BUSINESS만 대상으로 제한(안전)
            jpql.append("and m.customerType in (:cts) ");
            params.put("cts", List.of(CustomerType.PERSONAL, CustomerType.BUSINESS));
        }

        // date range (joinedAt)
        if (cond != null) {
            LocalDate from = cond.getFromDate();
            LocalDate to = cond.getToDate();

            if (from != null) {
                jpql.append("and m.joinedAt >= :fromDt ");
                params.put("fromDt", from.atStartOfDay());
            }
            if (to != null) {
                jpql.append("and m.joinedAt <= :toDt ");
                params.put("toDt", LocalDateTime.of(to, LocalTime.MAX));
            }
        }

        // keyword
        if (cond != null && StringUtils.hasText(cond.getKeyword())) {
            String kw = cond.getKeyword().trim();

            SearchField sf = cond.getSearchField() == null ? SearchField.USERNAME : cond.getSearchField();
            if (sf == SearchField.USERNAME) {
                jpql.append("and lower(m.username) like :kw ");
                params.put("kw", "%" + kw.toLowerCase() + "%");
            } else if (sf == SearchField.MOBILE) {
                jpql.append("and (m.mobile like :kw2 or m.tel like :kw2) ");
                params.put("kw2", "%" + kw + "%");
            } else if (sf == SearchField.NAME) {
                jpql.append("and lower(m.name) like :kw ");
                params.put("kw", "%" + kw.toLowerCase() + "%");
            }
        }

        // order by (화이트리스트)
        String orderExpr = resolveOrderExpr(sortKey);
        String direction = "asc".equalsIgnoreCase(sortDir) ? "asc" : "desc";

        jpql.append("order by ").append(orderExpr).append(" ").append(direction).append(", m.id asc ");

        TypedQuery<ClientApplyRowDto> query = em.createQuery(jpql.toString(), ClientApplyRowDto.class);
        applyParams(query, params);

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<ClientApplyRowDto> content = query.getResultList();

        // count
        StringBuilder countJpql = new StringBuilder();
        countJpql.append("select count(m) from Member m left join m.companyProfile cp ")
                 .append("where m.status = :pending and m.domain = :domain ");

        // 동일 조건 재사용을 위해 where절만 다시 붙여줍니다.
        // (위 jpql의 where 이후 부분을 그대로 재사용하기 어렵기 때문에 동일 로직으로 구성)
        Map<String, Object> countParams = new HashMap<>();
        countParams.put("pending", MemberStatus.PENDING);
        countParams.put("domain", MemberDomain.CUSTOMER);

        if (cond != null && cond.getApplyType() != null && cond.getApplyType() != ApplyType.ALL) {
            if (cond.getApplyType() == ApplyType.PERSONAL) {
                countJpql.append("and m.customerType = :ct ");
                countParams.put("ct", CustomerType.PERSONAL);
            } else if (cond.getApplyType() == ApplyType.BUSINESS) {
                countJpql.append("and m.customerType = :ct ");
                countParams.put("ct", CustomerType.BUSINESS);
            }
        } else {
            countJpql.append("and m.customerType in (:cts) ");
            countParams.put("cts", List.of(CustomerType.PERSONAL, CustomerType.BUSINESS));
        }

        if (cond != null) {
            LocalDate from = cond.getFromDate();
            LocalDate to = cond.getToDate();

            if (from != null) {
                countJpql.append("and m.joinedAt >= :fromDt ");
                countParams.put("fromDt", from.atStartOfDay());
            }
            if (to != null) {
                countJpql.append("and m.joinedAt <= :toDt ");
                countParams.put("toDt", LocalDateTime.of(to, LocalTime.MAX));
            }
        }

        if (cond != null && StringUtils.hasText(cond.getKeyword())) {
            String kw = cond.getKeyword().trim();
            SearchField sf = cond.getSearchField() == null ? SearchField.USERNAME : cond.getSearchField();

            if (sf == SearchField.USERNAME) {
                countJpql.append("and lower(m.username) like :kw ");
                countParams.put("kw", "%" + kw.toLowerCase() + "%");
            } else if (sf == SearchField.MOBILE) {
                countJpql.append("and (m.mobile like :kw2 or m.tel like :kw2) ");
                countParams.put("kw2", "%" + kw + "%");
            } else if (sf == SearchField.NAME) {
                countJpql.append("and lower(m.name) like :kw ");
                countParams.put("kw", "%" + kw.toLowerCase() + "%");
            }
        }

        TypedQuery<Long> countQuery = em.createQuery(countJpql.toString(), Long.class);
        applyParams(countQuery, countParams);
        long total = countQuery.getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    public ClientApplyDetailDto findPendingDetail(Long memberId) {
        String jpql =
            "select new com.dev.IbioScience.dto.customer.auth.ClientApplyDetailDto(" +
            " m.id, m.username, m.name, m.mobile, m.tel, m.email, m.organizationName, " +
            " m.address.postcode, m.address.roadAddress, m.address.detailAddress, " +
            " m.joinedAt, m.customerType, " +
            " cp.companyName, cp.department, cp.ceoName, cp.businessType, cp.businessItem, " +
            " cp.representativeTel, cp.fax, cp.invoiceEmail, cp.businessRegistrationNumber, " +
            " cp.companyAddress.postcode, cp.companyAddress.roadAddress, cp.companyAddress.detailAddress, " +
            " cp.businessRegImageRoad " +
            ") " +
            "from Member m " +
            "left join m.companyProfile cp " +
            "where m.id = :id and m.status = :pending and m.domain = :domain";

        List<ClientApplyDetailDto> list = em.createQuery(jpql, ClientApplyDetailDto.class)
            .setParameter("id", memberId)
            .setParameter("pending", MemberStatus.PENDING)
            .setParameter("domain", MemberDomain.CUSTOMER)
            .getResultList();

        return list.isEmpty() ? null : list.get(0);
    }

    public int approveOne(Long memberId) {
        String jpql = "update Member m set m.status = :active where m.id = :id and m.status = :pending";
        return em.createQuery(jpql)
            .setParameter("active", MemberStatus.ACTIVE)
            .setParameter("pending", MemberStatus.PENDING)
            .setParameter("id", memberId)
            .executeUpdate();
    }

    public int approveBulk(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) return 0;
        String jpql = "update Member m set m.status = :active where m.id in :ids and m.status = :pending";
        return em.createQuery(jpql)
            .setParameter("active", MemberStatus.ACTIVE)
            .setParameter("pending", MemberStatus.PENDING)
            .setParameter("ids", memberIds)
            .executeUpdate();
    }

    private String resolveOrderExpr(String sortKey) {
        if (!StringUtils.hasText(sortKey)) return "m.joinedAt"; // 기본 최신순
        switch (sortKey) {
            case "username":
                return "m.username";
            case "companyName":
                return "cp.companyName";
            case "name":
                return "m.name";
            case "contact":
                // mobile 우선 (없으면 tel) - JPQL coalesce 사용
                return "coalesce(m.mobile, m.tel)";
            case "joinedAt":
                return "m.joinedAt";
            default:
                return "m.joinedAt";
        }
    }

    private void applyParams(TypedQuery<?> q, Map<String, Object> params) {
        for (Map.Entry<String, Object> e : params.entrySet()) {
            q.setParameter(e.getKey(), e.getValue());
        }
    }
}