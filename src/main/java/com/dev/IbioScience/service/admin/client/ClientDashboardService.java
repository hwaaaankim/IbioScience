package com.dev.IbioScience.service.admin.client;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.admin.client.ClientDashboardLogRowDto;
import com.dev.IbioScience.dto.admin.client.ClientDashboardSummaryResponse;
import com.dev.IbioScience.dto.admin.client.ClientDashboardSummaryResponse.Item;
import com.dev.IbioScience.enums.logging.MemberAuditAction;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.MemberAuditLog;
import com.dev.IbioScience.model.logging.VisitDaily;
import com.dev.IbioScience.repository.auth.MemberAuditLogRepository;
import com.dev.IbioScience.repository.logging.VisitDailyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientDashboardService {

	private final VisitDailyRepository visitDailyRepository;
	private final MemberAuditLogRepository memberAuditLogRepository;

	private static final DateTimeFormatter LOG_DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public ClientDashboardSummaryResponse getSummary(LocalDate date) {
		LocalDateTime start = date.atStartOfDay();
		LocalDateTime end = date.plusDays(1).atStartOfDay();

		// ✅ VisitDaily (없을 수 있음)
		long pv = 0L;
		long uv = 0L;

		VisitDaily vd = visitDailyRepository.findById(date).orElse(null);
		if (vd != null) {
			pv = vd.getPageViewCount();
			uv = vd.getUniqueCount();
		}

		List<Item> items = new ArrayList<>();

		// ✅ 방문 집계(상세 목록 없음)
		items.add(Item.builder()
				.key("VISIT_PV")
				.label("방문 PV")
				.count(pv)
				.drilldown(false)
				.build());

		items.add(Item.builder()
				.key("VISIT_UV")
				.label("방문 UV")
				.count(uv)
				.drilldown(false)
				.build());

		// ✅ 멤버 로깅(상세 목록 가능)
		addActionItem(items, date, start, end, MemberAuditAction.PERSONAL_SIGNUP, "일반회원 가입");
		addActionItem(items, date, start, end, MemberAuditAction.COMPANY_SIGNUP, "기업회원 가입");

		addActionItem(items, date, start, end, MemberAuditAction.PERSONAL_LOGIN, "일반회원 로그인");
		addActionItem(items, date, start, end, MemberAuditAction.COMPANY_LOGIN, "기업회원 로그인");
		addActionItem(items, date, start, end, MemberAuditAction.STAFF_LOGIN, "직원 로그인");

		addActionItem(items, date, start, end, MemberAuditAction.PERSONAL_WITHDRAW_REQUEST, "일반회원 탈퇴요청");
		addActionItem(items, date, start, end, MemberAuditAction.PERSONAL_TO_COMPANY_CONVERSION, "일반→기업 전환");
		addActionItem(items, date, start, end, MemberAuditAction.BUYER_DEALER_TO_SELLER_REQUEST, "판매자 전환 신청");

		addActionItem(items, date, start, end, MemberAuditAction.PERSONAL_INFO_UPDATE, "일반회원 정보수정");
		addActionItem(items, date, start, end, MemberAuditAction.COMPANY_INFO_UPDATE, "기업회원 정보수정");

		return ClientDashboardSummaryResponse.builder()
				.date(date.toString())
				.visitPv(pv)
				.visitUv(uv)
				.items(items)
				.build();
	}

	private void addActionItem(List<Item> items, LocalDate date, LocalDateTime start, LocalDateTime end,
			MemberAuditAction action, String label) {

		long cnt = memberAuditLogRepository.countByActionAndCreatedAtBetween(action, start, end);

		items.add(Item.builder()
				.key(action.name())
				.label(label)
				.count(cnt)
				.drilldown(true)
				.build());
	}

	public Page<ClientDashboardLogRowDto> getLogs(LocalDate date, MemberAuditAction action, Pageable pageable) {
		LocalDateTime start = date.atStartOfDay();
		LocalDateTime end = date.plusDays(1).atStartOfDay();

		Page<MemberAuditLog> page = memberAuditLogRepository
				.findByActionAndCreatedAtBetweenOrderByCreatedAtDesc(action, start, end, pageable);

		return page.map(this::toRow);
	}

	private ClientDashboardLogRowDto toRow(MemberAuditLog log) {
		Member m = log.getMember();

		String customerType = (m != null && m.getCustomerType() != null) ? m.getCustomerType().name() : "-";
		String role = (m != null && m.getRole() != null) ? m.getRole().name() : "-";
		String dealerType = (m != null && m.getDealerType() != null) ? m.getDealerType().name() : "-";
		String domain = (m != null && m.getDomain() != null) ? m.getDomain().name() : "-";

		String loggedAt = (log.getCreatedAt() != null) ? log.getCreatedAt().format(LOG_DT_FMT) : "-";

		return ClientDashboardLogRowDto.builder()
				.loggedAt(loggedAt)
				.username(m != null ? m.getUsername() : "-")
				.name(m != null ? m.getName() : "-")
				.customerType(customerType)
				.role(role)
				.dealerType(dealerType)
				.domain(domain)
				.build();
	}
}