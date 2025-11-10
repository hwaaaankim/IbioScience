package com.dev.IbioScience.controller.admin.common.member;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dev.IbioScience.dto.member.auth.StaffUpdateRequest;
import com.dev.IbioScience.enums.auth.MemberRole;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.PrincipalDetails;
import com.dev.IbioScience.service.auth.admin.common.AdminMemberCommonService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/common")
@RequiredArgsConstructor
public class AdminMemberCommonController {

	private final AdminMemberCommonService commonService;

	@GetMapping("/memberDetail/{id}")
	public String memberDetail(@PathVariable("id") Long id, @AuthenticationPrincipal PrincipalDetails principal,
			Model model) {

		Member target = commonService.getMemberOrThrow(id);

		boolean isSelf = principal != null && target.getId().equals(principal.getMember().getId());

		// 1차: DB에 저장된 현재 로그인 사용자의 enum 역할을 신뢰
		boolean isRoot = principal != null && principal.getMember().getRole() == MemberRole.ROOT;
		boolean isMaster = principal != null && principal.getMember().getRole() == MemberRole.MASTER;

		// 2차: GrantedAuthority로 보강(환경에 따라 ROLE_ 접두 유무 혼재 방지)
		if (principal != null) {
			for (GrantedAuthority auth : principal.getAuthorities()) {
				String a = auth.getAuthority(); // e.g. ROLE_ROOT / ROOT
				if (a != null) {
					String normalized = a.startsWith("ROLE_") ? a.substring(5) : a;
					if ("ROOT".equalsIgnoreCase(normalized))
						isRoot = true;
					if ("MASTER".equalsIgnoreCase(normalized))
						isMaster = true;
				}
			}
		}

		boolean canManage = isRoot || isMaster; // 사용여부/대표여부 변경
		boolean canChangeRole = (isRoot || isMaster) && !(isRoot && isSelf); // 루트 본인은 권한 변경 불가
		System.out.println(isSelf);
		System.out.println(isRoot);
		model.addAttribute("member", target);
		model.addAttribute("isSelf", isSelf);
		model.addAttribute("isRoot", isRoot);
		model.addAttribute("canManage", canManage);
		model.addAttribute("canChangeRole", canChangeRole);

		// 폼 초기값
		StaffUpdateRequest form = new StaffUpdateRequest();
		form.setId(target.getId());
		form.setUsername(target.getUsername());
		form.setName(target.getName());
		form.setPosition(target.getPosition());
		form.setTel(target.getTel());
		form.setMobile(target.getMobile());
		form.setEmail(target.getEmail());
		form.setRole(target.getRole());
		form.setUseYn(target.isUseYn());
		form.setIsPrimary(target.isPrimary());
		model.addAttribute("form", form);

		return "administration/shopManager/memberDetail";
	}

	@PostMapping("/memberUpdate")
	@ResponseBody
	public String memberUpdate(@ModelAttribute("form") @jakarta.validation.Valid StaffUpdateRequest request,
	                           BindingResult bindingResult,
	                           @AuthenticationPrincipal PrincipalDetails principal) {

	    if (bindingResult.hasErrors()) {
	        return """
	            <script>alert('입력값을 다시 확인해 주세요.');history.back();</script>
	            """;
	    }

	    // 로그인 정보 없을 경우 방어
	    if (principal == null || principal.getMember() == null) {
	        return """
	            <script>alert('로그인 정보가 유효하지 않습니다.');history.back();</script>
	            """;
	    }

	    Long currentUserId = principal.getMember().getId();

	    // 1차: DB enum 기준으로 신뢰
	    boolean isRoot   = principal.getMember().getRole() == MemberRole.ROOT;
	    boolean isMaster = principal.getMember().getRole() == MemberRole.MASTER;

	    // 2차: GrantedAuthority 로 보강 (ROLE_ 접두 혼재 대응)
	    for (GrantedAuthority auth : principal.getAuthorities()) {
	        String a = auth.getAuthority(); // e.g. "ROLE_ROOT" / "ROOT"
	        if (a != null) {
	            String normalized = a.startsWith("ROLE_") ? a.substring(5) : a;
	            if ("ROOT".equalsIgnoreCase(normalized))   isRoot = true;
	            if ("MASTER".equalsIgnoreCase(normalized)) isMaster = true;
	        }
	    }

	    boolean canManage = isRoot || isMaster; // 사용여부/대표여부 변경 가능
	    boolean isSelf = request.getId() != null && request.getId().equals(currentUserId);

	    // 루트 본인은 권한 변경 불가, 그 외 루트/마스터는 권한 변경 가능
	    boolean canChangeRole = (isRoot || isMaster) && !(isRoot && isSelf);

	    try {
	        commonService.updateStaff(request, currentUserId, canManage, canChangeRole);
	    } catch (IllegalStateException dup) {
	        return """
	            <script>alert('이미 사용 중인 아이디입니다.');history.back();</script>
	            """;
	    } catch (IllegalArgumentException notFoundOrInvalid) {
	        return """
	            <script>alert('요청을 처리할 수 없습니다.');history.back();</script>
	            """;
	    } catch (Exception e) {
	        return """
	            <script>alert('저장 중 오류가 발생했습니다.');history.back();</script>
	            """;
	    }

	    return String.format("""
	        <script>
	          alert('정보수정이 완료되었습니다.');
	          window.location.replace('/admin/common/memberDetail/%d');
	        </script>
	        """, request.getId());
	}
}
