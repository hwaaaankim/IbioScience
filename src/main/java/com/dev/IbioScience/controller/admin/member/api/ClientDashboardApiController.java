package com.dev.IbioScience.controller.admin.member.api;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.admin.client.ClientDashboardLogRowDto;
import com.dev.IbioScience.dto.admin.client.ClientDashboardSummaryResponse;
import com.dev.IbioScience.enums.logging.MemberAuditAction;
import com.dev.IbioScience.service.admin.client.ClientDashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/administration/api/client-dashboard")
public class ClientDashboardApiController {

	private final ClientDashboardService clientDashboardService;

	@GetMapping("/summary")
	public ClientDashboardSummaryResponse summary(
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return clientDashboardService.getSummary(date);
	}

	@GetMapping("/logs")
	public Page<ClientDashboardLogRowDto> logs(
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam("action") MemberAuditAction action,
			Pageable pageable) {

		// ✅ size 기본 10은 프론트에서 주지만, 서버에서도 그대로 수용
		return clientDashboardService.getLogs(date, action, pageable);
	}
}