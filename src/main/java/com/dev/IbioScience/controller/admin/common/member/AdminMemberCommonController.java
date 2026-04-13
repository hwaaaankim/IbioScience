package com.dev.IbioScience.controller.admin.common.member;

import org.springframework.security.access.prepost.PreAuthorize;
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
import com.dev.IbioScience.enums.auth.role.AdminPageCodes;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.PrincipalDetails;
import com.dev.IbioScience.service.auth.admin.common.AdminMemberCommonService;
import com.dev.IbioScience.service.auth.role.AdminRoleManagerService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/common")
@RequiredArgsConstructor
public class AdminMemberCommonController {

    private final AdminMemberCommonService commonService;
    private final AdminRoleManagerService adminRoleManagerService;

    @PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).COMMON_MY_INFO)")
    @GetMapping("/memberDetail/{id}")
    public String memberDetail(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal PrincipalDetails principal,
            Model model
    ) {
        Member target = commonService.getMemberOrThrow(id);

        boolean isSelf = principal != null && principal.getMember() != null
                && target.getId().equals(principal.getMember().getId());

        boolean isRoot = principal != null && principal.getMember() != null
                && principal.getMember().getRole() == MemberRole.ROOT;
        boolean isMaster = principal != null && principal.getMember() != null
                && principal.getMember().getRole() == MemberRole.MASTER;

        if (principal != null) {
            for (GrantedAuthority auth : principal.getAuthorities()) {
                String authority = auth.getAuthority();
                if (authority == null) {
                    continue;
                }

                String normalized = authority.startsWith("ROLE_") ? authority.substring(5) : authority;

                if ("ROOT".equalsIgnoreCase(normalized)) {
                    isRoot = true;
                }
                if ("MASTER".equalsIgnoreCase(normalized)) {
                    isMaster = true;
                }
            }
        }

        boolean canUpdatePage = principal != null
                && principal.getMember() != null
                && adminRoleManagerService.canUpdateByPageCode(principal.getMember(), AdminPageCodes.COMMON_MY_INFO);

        boolean canManage = canUpdatePage && (isRoot || isMaster);
        boolean canChangeRole = canUpdatePage && (isRoot || isMaster) && !(isRoot && isSelf);
        boolean canEditSelf = canUpdatePage && isSelf;

        model.addAttribute("member", target);
        model.addAttribute("isSelf", isSelf);
        model.addAttribute("isRoot", isRoot);
        model.addAttribute("canManage", canManage);
        model.addAttribute("canChangeRole", canChangeRole);
        model.addAttribute("canUpdatePage", canUpdatePage);
        model.addAttribute("canEditSelf", canEditSelf);

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

    @PreAuthorize("@adminMenuFacade.canUpdateByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).COMMON_MY_INFO)")
    @PostMapping("/memberUpdate")
    @ResponseBody
    public String memberUpdate(
            @ModelAttribute("form") @jakarta.validation.Valid StaffUpdateRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        if (bindingResult.hasErrors()) {
            return """
                <script>alert('입력값을 다시 확인해 주세요.');history.back();</script>
                """;
        }

        if (principal == null || principal.getMember() == null) {
            return """
                <script>alert('로그인 정보가 유효하지 않습니다.');history.back();</script>
                """;
        }

        Long currentUserId = principal.getMember().getId();

        boolean isRoot = principal.getMember().getRole() == MemberRole.ROOT;
        boolean isMaster = principal.getMember().getRole() == MemberRole.MASTER;

        for (GrantedAuthority auth : principal.getAuthorities()) {
            String authority = auth.getAuthority();
            if (authority == null) {
                continue;
            }

            String normalized = authority.startsWith("ROLE_") ? authority.substring(5) : authority;

            if ("ROOT".equalsIgnoreCase(normalized)) {
                isRoot = true;
            }
            if ("MASTER".equalsIgnoreCase(normalized)) {
                isMaster = true;
            }
        }

        boolean hasPageUpdatePermission =
                adminRoleManagerService.canUpdateByPageCode(principal.getMember(), AdminPageCodes.COMMON_MY_INFO);

        if (!hasPageUpdatePermission) {
            return """
                <script>alert('수정 권한이 없습니다.');history.back();</script>
                """;
        }

        boolean isSelf = request.getId() != null && request.getId().equals(currentUserId);
        boolean canManage = hasPageUpdatePermission && (isRoot || isMaster);
        boolean canChangeRole = hasPageUpdatePermission && (isRoot || isMaster) && !(isRoot && isSelf);

        try {
            commonService.updateStaff(request, currentUserId, canManage, canChangeRole);
        } catch (IllegalStateException dup) {
            return """
                <script>alert('이미 사용 중인 아이디입니다.');history.back();</script>
                """;
        } catch (IllegalArgumentException ex) {
            return """
                <script>alert('요청을 처리할 수 없습니다.');history.back();</script>
                """;
        } catch (Exception ex) {
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