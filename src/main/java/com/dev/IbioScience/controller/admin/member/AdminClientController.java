package com.dev.IbioScience.controller.admin.member;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dev.IbioScience.dto.admin.client.ClientListRowDto;
import com.dev.IbioScience.dto.admin.client.ClientSearchCondition;
import com.dev.IbioScience.dto.common.CommonAPIResponse;
import com.dev.IbioScience.dto.customer.auth.ClientApplyPaginationDto;
import com.dev.IbioScience.dto.customer.auth.ClientApplyRowDto;
import com.dev.IbioScience.dto.customer.auth.ClientApplySearchCondition;
import com.dev.IbioScience.dto.customer.auth.WithdrawApproveBulkRequest;
import com.dev.IbioScience.dto.customer.auth.WithdrawApproveResultDto;
import com.dev.IbioScience.dto.customer.auth.WithdrawMemberDetailDto;
import com.dev.IbioScience.dto.customer.auth.WithdrawMemberRowDto;
import com.dev.IbioScience.dto.customer.auth.WithdrawSearchCondition;
import com.dev.IbioScience.dto.customer.auth.crm.ClientDetailHomeDto;
import com.dev.IbioScience.enums.auth.MemberStatus;
import com.dev.IbioScience.service.admin.client.ClientSearchService;
import com.dev.IbioScience.service.auth.admin.client.ClientApplyManagerService;
import com.dev.IbioScience.service.auth.admin.common.ClientWithdrawManagerService;
import com.dev.IbioScience.service.auth.crm.ClientDetailHomeService;
import com.dev.IbioScience.utils.PaginationUtils;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/root")
@RequiredArgsConstructor
public class AdminClientController {

	private final ClientSearchService clientSearchService;
	private final ClientApplyManagerService clientApplyManagerService;
	private final ClientWithdrawManagerService service;
	private final ClientDetailHomeService clientDetailHomeService;


	@GetMapping("/clientSearch")
	public String clientSearch(
			ClientSearchCondition cond,
			@RequestParam(name = "sortKey", required = false) String sortKey,
			@RequestParam(name = "sortDir", required = false) String sortDir,
			Model model
	) {
		// size/page 기본값 방어
		if (cond.getSize() == null) cond.setSize(10);
		if (cond.getPage() == null) cond.setPage(0);

		// 날짜 기준 기본: 가입일
		if (cond.getDateField() == null) {
			cond.setDateField(ClientSearchCondition.DateField.JOINED);
		}

		// ✅ 정렬값을 cond에 주입 (페이징 링크에서 sort 유지에 필수)
		cond.setSortKey(sortKey);
		cond.setSortDir(sortDir);

		// ✅ 딜러등급 사용 가능 조건(서버 방어)
		// - 일반회원(GENERAL) 포함이면 grade 무조건 무시(null)
		// - 기업(구매/판매) 선택이 없으면 grade 무조건 무시(null)
		boolean generalSelected = false;
		boolean companySelected = false;

		if (cond.getMemberTypes() != null && !cond.getMemberTypes().isEmpty()) {
			generalSelected = cond.getMemberTypes().contains(ClientSearchCondition.MemberType.GENERAL);

			companySelected =
					cond.getMemberTypes().contains(ClientSearchCondition.MemberType.COMPANY_BUYER)
					|| cond.getMemberTypes().contains(ClientSearchCondition.MemberType.COMPANY_SELLER);
		}

		// ✅ grade=ALL 은 Converter에서 null로 들어오므로 별도 비교 불필요
		// ✅ 일반회원 체크 시 어떤 경우든 grade 적용 금지
		boolean gradeAllowed = companySelected && !generalSelected;
		if (!gradeAllowed) {
			cond.setGrade(null);
		}

		Page<ClientListRowDto> page = clientSearchService.search(cond, sortKey, sortDir);

		model.addAttribute("page", page);
		model.addAttribute("totalCount", page.getTotalElements());
		model.addAttribute("cond", cond);
		model.addAttribute("sortKey", sortKey);
		model.addAttribute("sortDir", sortDir);

		// ✅ memberTypes 표시용(템플릿에서 Enum contains 문제 방지)
		List<String> selectedMemberTypes = (cond.getMemberTypes() == null)
				? List.of()
				: cond.getMemberTypes().stream().map(Enum::name).collect(Collectors.toList());
		model.addAttribute("selectedMemberTypes", selectedMemberTypes);

		// 페이지 번호(최대 5개)
		model.addAttribute("pageNumbers", PaginationUtils.makePageNumbers(page));

		// select option 데이터
		model.addAttribute("memberStatusList", java.util.Arrays.asList(MemberStatus.values()));
		model.addAttribute("dealerGradeOptions", java.util.List.of("ALL", "A", "B", "C", "D", "EXCEPTION", "CUSTOM"));
		model.addAttribute("searchFieldOptions", ClientSearchCondition.searchFieldOptions());

		return "administration/clientManager/clientSearch";
	}
	
	@GetMapping("/clientDashBoard")
	public String clientDashBoard(Model model) {
		// ✅ 기본: 오늘 날짜
		LocalDate today = LocalDate.now();
		model.addAttribute("selectedDate", today.toString()); // yyyy-MM-dd
		return "administration/clientManager/clientDashBoard";
	}

	@GetMapping("/clientApplyManager")
    public String clientApplyManager(
            @ModelAttribute ClientApplySearchCondition cond,
            @RequestParam(name = "sortKey", required = false) String sortKey,
            @RequestParam(name = "sortDir", required = false) String sortDir,
            Model model) {

        Page<ClientApplyRowDto> page = clientApplyManagerService.searchPending(cond, sortKey, sortDir);

        model.addAttribute("cond", cond);
        model.addAttribute("page", page);
        model.addAttribute("pagination", ClientApplyPaginationDto.of(page));

        model.addAttribute("sortKey", (sortKey == null || sortKey.isBlank()) ? "joinedAt" : sortKey);
        model.addAttribute("sortDir", (sortDir == null || sortDir.isBlank()) ? "desc" : sortDir);

        return "administration/clientManager/clientApplyManager";
    }

	@GetMapping("/clientWithDrawManager")
	public String clientWithDrawManager(
			@ModelAttribute WithdrawSearchCondition cond,
			@RequestParam(name = "sortKey", required = false) String sortKey,
			@RequestParam(name = "sortDir", required = false) String sortDir,
			Model model) {

		// 기본값
		if (cond.getSize() == null) cond.setSize(10);
		if (cond.getPage() == null) cond.setPage(0);
		if (cond.getApplyType() == null) cond.setApplyType(WithdrawSearchCondition.ApplyType.ALL);

		// 정렬 기본값: 신청일 desc
		if (sortKey == null || sortKey.isBlank()) sortKey = "requestedAt";
		if (sortDir == null || sortDir.isBlank()) sortDir = "desc";

		Page<WithdrawMemberRowDto> page = service.search(cond, sortKey, sortDir);

		int totalPages = page.getTotalPages();
		if (totalPages <= 0) totalPages = 1;

		int current = page.getNumber(); // 0-based
		int group = current / 5;
		int startPage = group * 5;
		int endPage = Math.min(startPage + 4, totalPages - 1);

		model.addAttribute("cond", cond);
		model.addAttribute("page", page);
		model.addAttribute("sortKey", sortKey);
		model.addAttribute("sortDir", sortDir);

		model.addAttribute("startPage", startPage);
		model.addAttribute("endPage", endPage);
		model.addAttribute("totalPages", totalPages);

		// 링크 유지를 위한 query string
		String qsBase = buildQueryStringBase(cond); // page/sort 제외
		String qsWithSort = qsBase + "&sortKey=" + enc(sortKey) + "&sortDir=" + enc(sortDir);

		model.addAttribute("qsBase", qsBase);
		model.addAttribute("qsWithSort", qsWithSort);

		return "administration/clientManager/clientWithDrawManager";
	}

	@ResponseBody
	@GetMapping(value = "/api/clientWithdraw/{memberId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public CommonAPIResponse<WithdrawMemberDetailDto> apiDetail(@PathVariable Long memberId) {
		return CommonAPIResponse.ok(service.getDetail(memberId));
	}

	@ResponseBody
	@PostMapping(value = "/api/clientWithdraw/approve/{memberId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public CommonAPIResponse<Void> apiApproveOne(@PathVariable Long memberId) {
		service.approveOne(memberId);
		return CommonAPIResponse.ok("승인 처리되었습니다.", null);
	}

	@ResponseBody
	@PostMapping(value = "/api/clientWithdraw/approve-bulk",
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public CommonAPIResponse<WithdrawApproveResultDto> apiApproveBulk(@RequestBody WithdrawApproveBulkRequest req) {
		WithdrawApproveResultDto result = service.approveBulk(req == null ? null : req.getMemberIds());
		return CommonAPIResponse.ok("일괄 승인 처리되었습니다.", result);
	}

	private String buildQueryStringBase(WithdrawSearchCondition cond) {
		StringJoiner sj = new StringJoiner("&");
		// 항상 size 포함
		sj.add("size=" + enc(String.valueOf(cond.getSize())));

		if (cond.getFromDate() != null) sj.add("fromDate=" + enc(cond.getFromDate().toString()));
		if (cond.getToDate() != null) sj.add("toDate=" + enc(cond.getToDate().toString()));

		if (cond.getSearchField() != null) sj.add("searchField=" + enc(cond.getSearchField().name()));
		if (cond.getKeyword() != null && !cond.getKeyword().isBlank()) sj.add("keyword=" + enc(cond.getKeyword().trim()));

		if (cond.getApplyType() != null) sj.add("applyType=" + enc(cond.getApplyType().name()));

		// page는 링크에서 별도 부여
		return "?" + sj.toString();
	}

	private String enc(String v) {
		return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
	}
	
	@GetMapping("/clientSellerTransferManager")
	public String clientSellerTransferManager() {

		return "administration/clientManager/clientSellerTransferManager";
	}
	
	@GetMapping("/clientCompanyTransferManager")
	public String clientCompanyTransferManager() {

		return "administration/clientManager/clientCompanyTransferManager";
	}
	
	@GetMapping("/clientDetail/{memberId}/home")
    public String home(@PathVariable Long memberId, Model model) {
        ClientDetailHomeDto dto = clientDetailHomeService.getHomeDto(memberId);
        model.addAttribute("dto", dto);
        model.addAttribute("memberId", memberId);
        model.addAttribute("activeTab", "home");
        return "administration/clientManager/detail/clientDetailHome";
    }

    // 나머지 6개 페이지는 “라우팅/active 메뉴”만 먼저 잡아둡니다(내용은 다음 작업에서 채우면 됩니다).
    @GetMapping("/clientDetail/{memberId}/information")
    public String information(@PathVariable Long memberId, Model model) {
        model.addAttribute("memberId", memberId);
        model.addAttribute("activeTab", "information");
        return "administration/clientManager/detail/clientDetailInformation";
    }

    @GetMapping("/clientDetail/{memberId}/orderList")
    public String orderList(@PathVariable Long memberId, Model model) {
        model.addAttribute("memberId", memberId);
        model.addAttribute("activeTab", "orderList");
        return "administration/clientManager/detail/clientDetailOrderList";
    }

    @GetMapping("/clientDetail/{memberId}/boardList")
    public String boardList(@PathVariable Long memberId, Model model) {
        model.addAttribute("memberId", memberId);
        model.addAttribute("activeTab", "boardList");
        return "administration/clientManager/detail/clientDetailBoardList";
    }

    @GetMapping("/clientDetail/{memberId}/benefit")
    public String benefit(@PathVariable Long memberId, Model model) {
        model.addAttribute("memberId", memberId);
        model.addAttribute("activeTab", "benefit");
        return "administration/clientManager/detail/clientDetailBenefit";
    }

    @GetMapping("/clientDetail/{memberId}/memo")
    public String memo(@PathVariable Long memberId, Model model) {
        model.addAttribute("memberId", memberId);
        model.addAttribute("activeTab", "memo");
        return "administration/clientManager/detail/clientDetailMemo";
    }

    @GetMapping("/clientDetail/{memberId}/wishList")
    public String wishList(@PathVariable Long memberId, Model model) {
        model.addAttribute("memberId", memberId);
        model.addAttribute("activeTab", "wishList");
        return "administration/clientManager/detail/clientDetailWishList";
    }
}
