package com.dev.IbioScience.repository.auth.admin.client;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.dev.IbioScience.dto.customer.auth.WithdrawMemberRowDto;
import com.dev.IbioScience.dto.customer.auth.WithdrawSearchCondition;
import com.dev.IbioScience.dto.customer.auth.WithdrawSearchCondition.ApplyType;
import com.dev.IbioScience.dto.customer.auth.WithdrawSearchCondition.SearchField;
import com.dev.IbioScience.enums.auth.CustomerType;
import com.dev.IbioScience.enums.auth.MemberStatus;
import com.dev.IbioScience.model.auth.CompanyProfile;
import com.dev.IbioScience.model.auth.Member;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Repository
public class ClientWithdrawMemberQueryRepository {

	@PersistenceContext
	private EntityManager em;

	// ✅ 여기만 바꾸면 “탈퇴신청 상태” 기준 변경 가능합니다.
	private static final MemberStatus TARGET_STATUS = MemberStatus.WITHDRAWN;

	public Page<WithdrawMemberRowDto> search(WithdrawSearchCondition cond, Pageable pageable, String sortKey, String sortDir) {

		CriteriaBuilder cb = em.getCriteriaBuilder();

		// ====== 본문 쿼리 ======
		CriteriaQuery<Tuple> cq = cb.createTupleQuery();
		Root<Member> m = cq.from(Member.class);
		Join<Member, CompanyProfile> cp = m.join("companyProfile", JoinType.LEFT);

		Expression<String> companyNameExpr = cb.coalesce(cp.get("companyName"), "- 없음 -");
		Expression<String> mobileNullIfEmpty = cb.nullif(m.get("mobile"), "");
		Expression<String> contactExpr = cb.coalesce(mobileNullIfEmpty, m.get("tel"));

		List<Predicate> predicates = buildPredicates(cond, cb, m, cp);

		cq.multiselect(
			m.get("id").alias("memberId"),
			m.get("username").alias("username"),
			companyNameExpr.alias("companyName"),
			m.get("name").alias("name"),
			contactExpr.alias("contact"),
			m.get("withdrewAt").alias("requestedAt")
		);

		cq.where(predicates.toArray(new Predicate[0]));
		cq.orderBy(buildOrders(sortKey, sortDir, cb, m, cp, companyNameExpr, contactExpr));

		TypedQuery<Tuple> query = em.createQuery(cq);
		query.setFirstResult((int) pageable.getOffset());
		query.setMaxResults(pageable.getPageSize());

		List<Tuple> tuples = query.getResultList();
		List<WithdrawMemberRowDto> content = new ArrayList<>();

		for (Tuple t : tuples) {
			content.add(new WithdrawMemberRowDto(
				t.get("memberId", Long.class),
				t.get("username", String.class),
				t.get("companyName", String.class),
				t.get("name", String.class),
				t.get("contact", String.class),
				t.get("requestedAt", LocalDateTime.class)
			));
		}

		// ====== count 쿼리 ======
		CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
		Root<Member> m2 = countCq.from(Member.class);
		Join<Member, CompanyProfile> cp2 = m2.join("companyProfile", JoinType.LEFT);

		List<Predicate> countPredicates = buildPredicates(cond, cb, m2, cp2);

		countCq.select(cb.count(m2));
		countCq.where(countPredicates.toArray(new Predicate[0]));

		Long total = em.createQuery(countCq).getSingleResult();

		return new PageImpl<>(content, pageable, total == null ? 0 : total);
	}

	private List<Predicate> buildPredicates(WithdrawSearchCondition cond, CriteriaBuilder cb,
			Root<Member> m, Join<Member, CompanyProfile> cp) {

		List<Predicate> predicates = new ArrayList<>();

		// (1) 상태 고정: 탈퇴신청 대상
		predicates.add(cb.equal(m.get("status"), TARGET_STATUS));

		// (2) 회원 구분: 전체/개인/기업
		ApplyType applyType = cond == null ? null : cond.getApplyType();
		if (applyType != null && applyType != ApplyType.ALL) {
			if (applyType == ApplyType.PERSONAL) {
				predicates.add(cb.equal(m.get("customerType"), CustomerType.PERSONAL));
			} else if (applyType == ApplyType.BUSINESS) {
				predicates.add(cb.equal(m.get("customerType"), CustomerType.BUSINESS));
				predicates.add(cb.isNotNull(m.get("companyProfile")));
			}
		}

		// (3) 기간: withdrewAt 기준
		if (cond != null && cond.getFromDate() != null) {
			LocalDateTime from = cond.getFromDate().atStartOfDay();
			predicates.add(cb.greaterThanOrEqualTo(m.get("withdrewAt"), from));
		}
		if (cond != null && cond.getToDate() != null) {
			LocalDateTime to = cond.getToDate().atTime(LocalTime.MAX);
			predicates.add(cb.lessThanOrEqualTo(m.get("withdrewAt"), to));
		}

		// (4) 키워드 검색
		if (cond != null && StringUtils.hasText(cond.getKeyword()) && cond.getSearchField() != null) {
			String like = "%" + cond.getKeyword().trim() + "%";
			SearchField sf = cond.getSearchField();

			if (sf == SearchField.USERNAME) {
				predicates.add(cb.like(m.get("username"), like));
			} else if (sf == SearchField.NAME) {
				predicates.add(cb.like(m.get("name"), like));
			} else if (sf == SearchField.CONTACT) {
				Predicate p1 = cb.like(m.get("mobile"), like);
				Predicate p2 = cb.like(m.get("tel"), like);
				predicates.add(cb.or(p1, p2));
			}
		}

		return predicates;
	}

	private List<Order> buildOrders(String sortKey, String sortDir, CriteriaBuilder cb,
			Root<Member> m, Join<Member, CompanyProfile> cp,
			Expression<String> companyNameExpr,
			Expression<String> contactExpr) {

		boolean desc = !"asc".equalsIgnoreCase(sortDir);
		String key = (sortKey == null ? "" : sortKey.trim());

		Expression<?> expr;
		switch (key) {
			case "username":
				expr = m.get("username");
				break;
			case "companyName":
				expr = companyNameExpr;
				break;
			case "name":
				expr = m.get("name");
				break;
			case "contact":
				expr = contactExpr;
				break;
			case "requestedAt":
			default:
				expr = m.get("withdrewAt");
				break;
		}

		List<Order> orders = new ArrayList<>();
		orders.add(desc ? cb.desc(expr) : cb.asc(expr));
		// 안정 정렬(동일값일 때 id asc)
		orders.add(cb.asc(m.get("id")));
		return orders;
	}
}