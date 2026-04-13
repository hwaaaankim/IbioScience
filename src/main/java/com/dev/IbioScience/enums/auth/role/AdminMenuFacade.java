package com.dev.IbioScience.enums.auth.role;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.PrincipalDetails;
import com.dev.IbioScience.service.auth.role.AdminRoleManagerService;

import lombok.RequiredArgsConstructor;

@Component("adminMenuFacade")
@RequiredArgsConstructor
public class AdminMenuFacade {

    private final AdminRoleManagerService adminRoleManagerService;

    public List<AdminRoleManagerService.AdminSidebarMenuGroup> getVisibleAdminMenuGroups() {
        Member member = getCurrentMember();

        if (member == null) {
            return List.of();
        }

        return adminRoleManagerService.getVisibleSidebarMenuGroups(member);
    }

    public boolean canView(String requestPath) {
        Member member = getCurrentMember();

        if (member == null || requestPath == null || requestPath.isBlank()) {
            return false;
        }

        return adminRoleManagerService.isAllowed(member, requestPath, HttpMethod.GET.name());
    }

    public boolean hasPagePermission(String pageCode, AdminPermissionAction action) {
        Member member = getCurrentMember();

        if (member == null || pageCode == null || pageCode.isBlank() || action == null) {
            return false;
        }

        return adminRoleManagerService.hasPagePermission(member, pageCode, action);
    }

    public boolean canViewByPageCode(String pageCode) {
        Member member = getCurrentMember();

        if (member == null || pageCode == null || pageCode.isBlank()) {
            return false;
        }

        return adminRoleManagerService.canViewByPageCode(member, pageCode);
    }

    public boolean canCreateByPageCode(String pageCode) {
        Member member = getCurrentMember();

        if (member == null || pageCode == null || pageCode.isBlank()) {
            return false;
        }

        return adminRoleManagerService.canCreateByPageCode(member, pageCode);
    }

    public boolean canUpdateByPageCode(String pageCode) {
        Member member = getCurrentMember();

        if (member == null || pageCode == null || pageCode.isBlank()) {
            return false;
        }

        return adminRoleManagerService.canUpdateByPageCode(member, pageCode);
    }

    public boolean canDeleteByPageCode(String pageCode) {
        Member member = getCurrentMember();

        if (member == null || pageCode == null || pageCode.isBlank()) {
            return false;
        }

        return adminRoleManagerService.canDeleteByPageCode(member, pageCode);
    }

    public boolean canCreateOrUpdateByPageCode(String pageCode) {
        Member member = getCurrentMember();

        if (member == null || pageCode == null || pageCode.isBlank()) {
            return false;
        }

        return adminRoleManagerService.canCreateByPageCode(member, pageCode)
                || adminRoleManagerService.canUpdateByPageCode(member, pageCode);
    }

    public boolean canAnyByPageCode(String pageCode, AdminPermissionAction... actions) {
        Member member = getCurrentMember();

        if (member == null || pageCode == null || pageCode.isBlank() || actions == null || actions.length == 0) {
            return false;
        }

        return Arrays.stream(actions)
                .anyMatch(action -> adminRoleManagerService.hasPagePermission(member, pageCode, action));
    }

    private Member getCurrentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof PrincipalDetails principalDetails) {
            return principalDetails.getMember();
        }

        return null;
    }
}