package com.dev.IbioScience.controller.admin.member;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.dev.IbioScience.dto.admin.client.ClientListRowDto;
import com.dev.IbioScience.dto.admin.client.ClientSearchCondition;
import com.dev.IbioScience.enums.auth.MemberStatus;
import com.dev.IbioScience.service.admin.client.ClientSearchService;
import com.dev.IbioScience.utils.PaginationUtils;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/root")
@RequiredArgsConstructor
public class AdminClientController {

	private final ClientSearchService clientSearchService;

	@GetMapping("/clientManager")
	public String clientManager() {

		return "administration/clientManager/clientManager";
	}

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

	@GetMapping("/clientGradeManager")
	public String clientGradeManager() {

		return "administration/clientManager/clientGradeManager";
	}

	@GetMapping("/clientDetail/home")
	public String clientDetailHome() {

		return "administration/clientManager/detail/clientDetailHome";
	}

	@GetMapping("/clientDetail/information")
	public String clientDetailInformation() {

		return "administration/clientManager/detail/clientDetailInformation";
	}

	@GetMapping("/clientDetail/orderList")
	public String clientDetailOrderList() {

		return "administration/clientManager/detail/clientDetailOrderList";
	}

	@GetMapping("/clientDetail/boardList")
	public String clientDetailBoardList() {

		return "administration/clientManager/detail/clientDetailBoardList";
	}

	@GetMapping("/clientDetail/benefit")
	public String clientDetailBenefit() {

		return "administration/clientManager/detail/clientDetailBenefit";
	}

	@GetMapping("/clientDetail/memo")
	public String clientDetailMemo() {

		return "administration/clientManager/detail/clientDetailMemo";
	}

	@GetMapping("/clientDetail/wishList")
	public String clientDetailWishList() {

		return "administration/clientManager/detail/clientDetailWishList";
	}
}
