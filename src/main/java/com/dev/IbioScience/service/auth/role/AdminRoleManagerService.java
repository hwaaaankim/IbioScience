package com.dev.IbioScience.service.auth.role;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import com.dev.IbioScience.dto.auth.role.AdminRoleManagerPageListResponse;
import com.dev.IbioScience.dto.auth.role.AdminRoleManagerSaveRequest;
import com.dev.IbioScience.enums.auth.MemberRole;
import com.dev.IbioScience.enums.auth.role.AdminPageCodes;
import com.dev.IbioScience.enums.auth.role.AdminPermissionAction;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.role.AdminManagedPage;
import com.dev.IbioScience.model.auth.role.AdminManagedPageEndpoint;
import com.dev.IbioScience.model.auth.role.AdminRolePagePermission;
import com.dev.IbioScience.repository.auth.role.AdminManagedPageEndpointRepository;
import com.dev.IbioScience.repository.auth.role.AdminManagedPageRepository;
import com.dev.IbioScience.repository.auth.role.AdminRolePagePermissionRepository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminRoleManagerService {

    private static final String COMMON_MENU_GROUP_NAME = "공통페이지";

    private final AdminManagedPageRepository pageRepository;
    private final AdminManagedPageEndpointRepository endpointRepository;
    private final AdminRolePagePermissionRepository permissionRepository;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    private static final Set<MemberRole> MANAGEABLE_ROLES = new LinkedHashSet<>(
            Arrays.asList(MemberRole.MASTER, MemberRole.OPERATOR, MemberRole.EMPLOY));

    private static final Set<String> STANDALONE_MANAGED_PAGE_PATHS = Set.of(
            "/brandManager",
            "/categoryManager",
            "/displayManager",
            "/couponManager",
            "/productPromotionManager",
            "/internalCategoryManager");

    @Transactional
    public void syncDefaultPagesAndPermissions() {
        for (SeedPage seedPage : getSeedPages()) {
            AdminManagedPage page = upsertPage(seedPage);
            upsertEndpoint(page, "GET", seedPage.pageUrl(), AdminPermissionAction.VIEW);

            for (MemberRole role : MANAGEABLE_ROLES) {
                ensurePermissionRow(page, role);
            }
        }

        registerDefaultActionEndpoints();
    }

    @Transactional(readOnly = true)
    public AdminRoleManagerPageListResponse getPagePermissions() {
        List<AdminManagedPage> pages = pageRepository.findAllByUseYnTrueOrderByMenuOrderAscPageOrderAscIdAsc();
        Map<Long, Map<MemberRole, AdminRolePagePermission>> permissionMap = buildPermissionMap(pages);

        List<AdminRoleManagerPageListResponse.PageRow> rows = pages.stream().map(page -> {
            Map<MemberRole, AdminRolePagePermission> roleMap =
                    permissionMap.getOrDefault(page.getId(), new EnumMap<>(MemberRole.class));

            return AdminRoleManagerPageListResponse.PageRow.builder()
                    .pageId(page.getId())
                    .pageCode(page.getPageCode())
                    .pageName(page.getPageName())
                    .pageUrl(page.getPageUrl())
                    .menuGroupName(page.getMenuGroupName())
                    .menuOrder(page.getMenuOrder())
                    .pageOrder(page.getPageOrder())
                    .master(toPermissionState(roleMap.get(MemberRole.MASTER)))
                    .operator(toPermissionState(roleMap.get(MemberRole.OPERATOR)))
                    .employ(toPermissionState(roleMap.get(MemberRole.EMPLOY)))
                    .build();
        }).toList();

        return AdminRoleManagerPageListResponse.builder()
                .pages(rows)
                .build();
    }

    @Transactional
    public void savePermissions(AdminRoleManagerSaveRequest request) {
        if (request == null || request.getPages() == null || request.getPages().isEmpty()) {
            throw new IllegalArgumentException("저장할 권한 정보가 없습니다.");
        }

        syncDefaultPagesAndPermissions();

        for (AdminRoleManagerSaveRequest.PagePermissionItem item : request.getPages()) {
            if (item.getPageId() == null) {
                throw new IllegalArgumentException("페이지 ID가 누락되었습니다.");
            }

            AdminManagedPage page = pageRepository.findById(item.getPageId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 페이지입니다. pageId=" + item.getPageId()));

            applyPermission(page, MemberRole.MASTER, item.getMaster());
            applyPermission(page, MemberRole.OPERATOR, item.getOperator());
            applyPermission(page, MemberRole.EMPLOY, item.getEmploy());
        }
    }

    @Transactional(readOnly = true)
    public boolean isAllowed(Member member, String requestPath, String httpMethod) {
        if (member == null || member.getRole() == null) {
            return false;
        }

        MemberRole role = member.getRole();

        if (role == MemberRole.ROOT) {
            return true;
        }

        Optional<ResolvedEndpoint> resolvedEndpoint = resolveMappedEndpoint(requestPath, httpMethod);

        if (resolvedEndpoint.isPresent()) {
            ResolvedEndpoint endpoint = resolvedEndpoint.get();
            return hasPermission(endpoint.page(), role, endpoint.action());
        }

        return legacyFallback(role, requestPath);
    }

    @Transactional(readOnly = true)
    public boolean hasPagePermission(Member member, String pageCode, AdminPermissionAction action) {
        if (member == null || member.getRole() == null) {
            return false;
        }

        return hasPagePermission(member.getRole(), pageCode, action);
    }

    @Transactional(readOnly = true)
    public boolean hasPagePermission(MemberRole role, String pageCode, AdminPermissionAction action) {
        if (role == null || !StringUtils.hasText(pageCode) || action == null) {
            return false;
        }

        if (role == MemberRole.ROOT) {
            return true;
        }

        AdminManagedPage page = pageRepository.findByPageCode(pageCode).orElse(null);

        if (page == null || !page.isUseYn()) {
            return false;
        }

        return hasPermission(page, role, action);
    }

    @Transactional(readOnly = true)
    public boolean canViewByPageCode(Member member, String pageCode) {
        return hasPagePermission(member, pageCode, AdminPermissionAction.VIEW);
    }

    @Transactional(readOnly = true)
    public boolean canCreateByPageCode(Member member, String pageCode) {
        return hasPagePermission(member, pageCode, AdminPermissionAction.CREATE);
    }

    @Transactional(readOnly = true)
    public boolean canUpdateByPageCode(Member member, String pageCode) {
        return hasPagePermission(member, pageCode, AdminPermissionAction.UPDATE);
    }

    @Transactional(readOnly = true)
    public boolean canDeleteByPageCode(Member member, String pageCode) {
        return hasPagePermission(member, pageCode, AdminPermissionAction.DELETE);
    }

    @Transactional(readOnly = true)
    public List<AdminSidebarMenuGroup> getVisibleSidebarMenuGroups(Member member) {
        if (member == null || member.getRole() == null) {
            return List.of();
        }

        List<AdminManagedPage> pages = pageRepository.findAllByUseYnTrueOrderByMenuOrderAscPageOrderAscIdAsc();
        Map<Long, Map<MemberRole, AdminRolePagePermission>> permissionMap = buildPermissionMap(pages);

        Map<String, SidebarGroupAccumulator> grouped = new LinkedHashMap<>();

        for (AdminManagedPage page : pages) {
            if (!isSidebarPage(page)) {
                continue;
            }

            if (!canViewPage(member.getRole(), page, permissionMap)) {
                continue;
            }

            SidebarGroupAccumulator group = grouped.computeIfAbsent(
                    page.getMenuGroupName(),
                    key -> new SidebarGroupAccumulator(
                            page.getMenuGroupName(),
                            page.getMenuOrder(),
                            buildCollapseId(page.getMenuOrder()),
                            resolveMenuGroupIcon(page.getMenuGroupName()),
                            new ArrayList<>()));

            group.items().add(new AdminSidebarMenuItem(
                    page.getPageCode(),
                    page.getPageName(),
                    page.getPageUrl(),
                    page.getPageOrder()));
        }

        return grouped.values().stream()
                .sorted(Comparator.comparing(
                        SidebarGroupAccumulator::menuOrder,
                        Comparator.nullsLast(Integer::compareTo)))
                .map(group -> new AdminSidebarMenuGroup(
                        group.groupName(),
                        group.menuOrder(),
                        group.collapseId(),
                        group.iconClass(),
                        group.items().stream()
                                .sorted(Comparator.comparing(
                                        AdminSidebarMenuItem::getPageOrder,
                                        Comparator.nullsLast(Integer::compareTo)))
                                .toList()))
                .toList();
    }

    @Transactional
    public void registerActionEndpoint(String pageCode, String httpMethod, String pathPattern,
            AdminPermissionAction action) {
        AdminManagedPage page = pageRepository.findByPageCode(pageCode)
                .orElseThrow(() -> new IllegalArgumentException("페이지 코드가 존재하지 않습니다. pageCode=" + pageCode));

        upsertEndpoint(page, httpMethod, pathPattern, action);
    }

    private void registerDefaultActionEndpoints() {
        registerEventManagerEndpoints();
        registerNoticeManagerEndpoints();
        registerQnaCategoryManagerEndpoints();
        registerQnaManagerEndpoints();

        registerOrderManagerEndpoints();
        registerMemberInsertEndpoints();
        registerMemberCommonEndpoints();
        registerClientDashboardEndpoints();
        registerClientCrmHomeEndpoints();
        registerClientBenefitEndpoints();
        registerClientReviewEndpoints();
        registerClientWishListEndpoints();
        registerEstimateEndpoints();
        registerClientWithdrawManagerEndpoints();
        registerClientCompanyTransferManagerEndpoints();
        registerClientSellerTransferManagerEndpoints();
        registerSellerInsertEndpoints();
        registerSettlementExecuteEndpoints();
        registerSettlementManagerEndpoints();
    }

    private void registerEventManagerEndpoints() {
        AdminManagedPage page = pageRepository.findByPageCode(AdminPageCodes.SITE_EVENT_MANAGER).orElse(null);

        if (page == null) {
            return;
        }

        upsertEndpoint(page, "GET", "/admin/manager/eventManager", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "GET", "/admin/manager/eventDetail/**", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "GET", "/admin/manager/eventInsertForm", AdminPermissionAction.CREATE);
        upsertEndpoint(page, "POST", "/admin/manager/event/create", AdminPermissionAction.CREATE);
        upsertEndpoint(page, "POST", "/admin/manager/event/update/**", AdminPermissionAction.UPDATE);
        upsertEndpoint(page, "DELETE", "/api/manager/event/**", AdminPermissionAction.DELETE);
    }

    private void registerNoticeManagerEndpoints() {
        AdminManagedPage page = pageRepository.findByPageCode(AdminPageCodes.SITE_NOTICE_MANAGER).orElse(null);

        if (page == null) {
            return;
        }

        upsertEndpoint(page, "GET", "/admin/manager/noticeManager", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "GET", "/admin/manager/noticeDetail/**", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "GET", "/admin/manager/noticeInsertForm", AdminPermissionAction.CREATE);
        upsertEndpoint(page, "POST", "/api/manager/notice", AdminPermissionAction.CREATE);
        upsertEndpoint(page, "PUT", "/api/manager/notice/**", AdminPermissionAction.UPDATE);
        upsertEndpoint(page, "DELETE", "/api/manager/notice/**", AdminPermissionAction.DELETE);
    }

    private void registerQnaCategoryManagerEndpoints() {
        AdminManagedPage page = pageRepository.findByPageCode(AdminPageCodes.SITE_QNA_CATEGORY_MANAGER).orElse(null);

        if (page == null) {
            return;
        }

        upsertEndpoint(page, "GET", "/admin/manager/qnaCategoryManager", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "POST", "/api/manager/qna-category", AdminPermissionAction.CREATE);
        upsertEndpoint(page, "PUT", "/api/manager/qna-category/**", AdminPermissionAction.UPDATE);
        upsertEndpoint(page, "DELETE", "/api/manager/qna-category/**", AdminPermissionAction.DELETE);
    }

    private void registerQnaManagerEndpoints() {
        AdminManagedPage page = pageRepository.findByPageCode(AdminPageCodes.SITE_QNA_MANAGER).orElse(null);

        if (page == null) {
            return;
        }

        upsertEndpoint(page, "GET", "/admin/manager/qnaManager", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "GET", "/admin/manager/qnaDetail/**", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "GET", "/admin/manager/qnaInsertForm", AdminPermissionAction.CREATE);
        upsertEndpoint(page, "POST", "/api/manager/qna", AdminPermissionAction.CREATE);
        upsertEndpoint(page, "PUT", "/api/manager/qna/**", AdminPermissionAction.UPDATE);
        upsertEndpoint(page, "DELETE", "/api/manager/qna/**", AdminPermissionAction.DELETE);
    }

    private void registerOrderManagerEndpoints() {
        AdminManagedPage page = pageRepository.findByPageCode(AdminPageCodes.ORDER_MANAGER).orElse(null);

        if (page == null) {
            return;
        }

        upsertEndpoint(page, "GET", "/admin/root/orderDetail/{id}", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "POST", "/admin/root/api/orders/status/bulk", AdminPermissionAction.UPDATE);
        upsertEndpoint(page, "POST", "/admin/root/api/orders/{orderId}/status-detail", AdminPermissionAction.UPDATE);
    }

    private void registerMemberInsertEndpoints() {
        AdminManagedPage page = pageRepository.findByPageCode(AdminPageCodes.SHOP_MEMBER_INSERT_FORM).orElse(null);

        if (page == null) {
            return;
        }

        upsertEndpoint(page, "POST", "/admin/root/memberInsert", AdminPermissionAction.CREATE);
    }

    private void registerMemberCommonEndpoints() {
        AdminManagedPage page = pageRepository.findByPageCode(AdminPageCodes.COMMON_MY_INFO).orElse(null);

        if (page == null) {
            return;
        }

        upsertEndpoint(page, "POST", "/admin/common/memberUpdate", AdminPermissionAction.UPDATE);
    }

    private void registerClientDashboardEndpoints() {
        AdminManagedPage page = pageRepository.findByPageCode(AdminPageCodes.MEM_CLIENT_DASHBOARD).orElse(null);

        if (page == null) {
            return;
        }

        upsertEndpoint(page, "GET", "/administration/api/client-dashboard/summary", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "GET", "/administration/api/client-dashboard/logs", AdminPermissionAction.VIEW);
    }

    private void registerClientCrmHomeEndpoints() {
        AdminManagedPage page = pageRepository.findByPageCode(AdminPageCodes.CRM_HOME).orElse(null);

        if (page == null) {
            return;
        }

        upsertEndpoint(page, "POST", "/admin/root/api/clientDetail/{memberId}/resetPassword", AdminPermissionAction.UPDATE);
        upsertEndpoint(page, "POST", "/admin/root/api/clientDetail/{memberId}/memos/save", AdminPermissionAction.UPDATE);
        upsertEndpoint(page, "GET", "/admin/root/api/clientDetail/{memberId}/memos", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "POST", "/admin/root/api/clientDetail/{memberId}/buyer/grade", AdminPermissionAction.UPDATE);
        upsertEndpoint(page, "POST", "/admin/root/api/clientDetail/{memberId}/member/address", AdminPermissionAction.UPDATE);
        upsertEndpoint(page, "POST", "/admin/root/api/clientDetail/{memberId}/company/address", AdminPermissionAction.UPDATE);
        upsertEndpoint(page, "POST", "/admin/root/api/clientDetail/{memberId}/seller/saveAll", AdminPermissionAction.UPDATE);
    }

    private void registerClientBenefitEndpoints() {
        AdminManagedPage page = pageRepository.findByPageCode(AdminPageCodes.CRM_BENEFIT).orElse(null);

        if (page == null) {
            return;
        }

        upsertEndpoint(page, "GET", "/admin/root/api/clientDetail/{memberId}/benefit/point-summary", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "GET", "/admin/root/api/clientDetail/{memberId}/benefit/point-histories", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "POST", "/admin/root/api/clientDetail/{memberId}/benefit/point/grant", AdminPermissionAction.UPDATE);
        upsertEndpoint(page, "POST", "/admin/root/api/clientDetail/{memberId}/benefit/point/deduct", AdminPermissionAction.UPDATE);
        upsertEndpoint(page, "GET", "/admin/root/api/clientDetail/{memberId}/benefit/coupons", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "GET", "/admin/root/api/clientDetail/{memberId}/benefit/coupons/{memberCouponId}/source", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "POST", "/admin/root/api/clientDetail/{memberId}/benefit/coupons/grant", AdminPermissionAction.CREATE);
        upsertEndpoint(page, "DELETE", "/admin/root/api/clientDetail/{memberId}/benefit/coupons/{memberCouponId}", AdminPermissionAction.DELETE);
    }

    private void registerClientReviewEndpoints() {
        AdminManagedPage page = pageRepository.findByPageCode(AdminPageCodes.CRM_REVIEW_LIST).orElse(null);

        if (page == null) {
            return;
        }

        upsertEndpoint(page, "GET", "/admin/root/api/clientDetail/{memberId}/reviewList", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "DELETE", "/admin/root/api/clientDetail/{memberId}/reviewList", AdminPermissionAction.DELETE);
    }

    private void registerClientWishListEndpoints() {
        AdminManagedPage page = pageRepository.findByPageCode(AdminPageCodes.CRM_WISH_LIST).orElse(null);

        if (page == null) {
            return;
        }

        upsertEndpoint(page, "GET", "/admin/root/api/clientDetail/{memberId}/wishList", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "DELETE", "/admin/root/api/clientDetail/{memberId}/wishList", AdminPermissionAction.DELETE);
    }

    private void registerEstimateEndpoints() {
        AdminManagedPage page = pageRepository.findByPageCode(AdminPageCodes.CRM_ESTIMATE_LIST).orElse(null);

        if (page == null) {
            return;
        }

        upsertEndpoint(page, "GET", "/admin/root/api/client/{memberId}/estimates", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "GET", "/admin/root/api/client/{memberId}/estimates/{estimateId}", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "POST", "/admin/root/api/client/{memberId}/estimates/{estimateId}/send-email", AdminPermissionAction.UPDATE);
    }

    private void registerClientWithdrawManagerEndpoints() {
        AdminManagedPage page = pageRepository.findByPageCode(AdminPageCodes.MEM_CLIENT_WITHDRAW_MANAGER).orElse(null);

        if (page == null) {
            return;
        }

        upsertEndpoint(page, "GET", "/admin/root/api/clientWithdraw/{memberId}", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "POST", "/admin/root/api/clientWithdraw/approve/{memberId}", AdminPermissionAction.UPDATE);
        upsertEndpoint(page, "POST", "/admin/root/api/clientWithdraw/approve-bulk", AdminPermissionAction.UPDATE);
    }

    private void registerClientCompanyTransferManagerEndpoints() {
        AdminManagedPage page = pageRepository.findByPageCode(AdminPageCodes.MEM_CLIENT_COMPANY_TRANSFER_MANAGER).orElse(null);

        if (page == null) {
            return;
        }

        upsertEndpoint(page, "GET", "/api/admin/root/clientTransfer/company-applications", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "GET", "/api/admin/root/clientTransfer/company-applications/{applicationId}", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "POST", "/api/admin/root/clientTransfer/company-applications/{applicationId}/approve", AdminPermissionAction.UPDATE);
        upsertEndpoint(page, "POST", "/api/admin/root/clientTransfer/company-applications/bulk-approve", AdminPermissionAction.UPDATE);
    }

    private void registerClientSellerTransferManagerEndpoints() {
        AdminManagedPage page = pageRepository.findByPageCode(AdminPageCodes.MEM_CLIENT_SELLER_TRANSFER_MANAGER).orElse(null);

        if (page == null) {
            return;
        }

        upsertEndpoint(page, "GET", "/api/admin/root/clientTransfer/seller-applications", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "GET", "/api/admin/root/clientTransfer/seller-applications/{applicationId}", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "POST", "/api/admin/root/clientTransfer/seller-applications/{applicationId}/approve", AdminPermissionAction.UPDATE);
        upsertEndpoint(page, "GET", "/api/admin/root/clientTransfer/categories/larges", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "GET", "/api/admin/root/clientTransfer/categories/mediums", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "GET", "/api/admin/root/clientTransfer/categories/smalls", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "GET", "/api/admin/root/clientTransfer/seller-enums", AdminPermissionAction.VIEW);
    }

    private void registerSellerInsertEndpoints() {
        AdminManagedPage page = pageRepository.findByPageCode(AdminPageCodes.MEM_SELLER_INSERT_FORM).orElse(null);

        if (page == null) {
            return;
        }

        upsertEndpoint(page, "POST", "/api/admin/root/sellers", AdminPermissionAction.CREATE);
    }

    private void registerSettlementExecuteEndpoints() {
        AdminManagedPage page = pageRepository.findByPageCode(AdminPageCodes.SHOP_SETTLEMENT_EXECUTE).orElse(null);

        if (page == null) {
            return;
        }

        upsertEndpoint(page, "POST", "/admin/root/api/settlement-execute/preview", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "POST", "/admin/root/api/settlement-execute/run", AdminPermissionAction.CREATE);
    }

    private void registerSettlementManagerEndpoints() {
        AdminManagedPage page = pageRepository.findByPageCode(AdminPageCodes.SHOP_SETTLEMENT_MANAGER).orElse(null);

        if (page == null) {
            return;
        }

        upsertEndpoint(page, "GET", "/admin/root/api/settlement-manager/search", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "GET", "/admin/root/api/settlement-manager/{settlementId}/orders", AdminPermissionAction.VIEW);
        upsertEndpoint(page, "PATCH", "/admin/root/api/settlement-manager/{settlementId}/orders", AdminPermissionAction.UPDATE);
        upsertEndpoint(page, "PATCH", "/admin/root/api/settlement-manager/status", AdminPermissionAction.UPDATE);
    }

    private Map<Long, Map<MemberRole, AdminRolePagePermission>> buildPermissionMap(List<AdminManagedPage> pages) {
        List<AdminRolePagePermission> permissions = permissionRepository.findAllByPageIn(pages);
        Map<Long, Map<MemberRole, AdminRolePagePermission>> permissionMap = new HashMap<>();

        for (AdminRolePagePermission permission : permissions) {
            permissionMap.computeIfAbsent(permission.getPage().getId(), k -> new EnumMap<>(MemberRole.class))
                    .put(permission.getRole(), permission);
        }

        return permissionMap;
    }

    private boolean isSidebarPage(AdminManagedPage page) {
        if (page == null) {
            return false;
        }

        if (!page.isUseYn()) {
            return false;
        }

        if (!StringUtils.hasText(page.getPageUrl())) {
            return false;
        }

        if (!StringUtils.hasText(page.getMenuGroupName())) {
            return false;
        }

        return !COMMON_MENU_GROUP_NAME.equals(page.getMenuGroupName());
    }

    private boolean canViewPage(MemberRole role,
            AdminManagedPage page,
            Map<Long, Map<MemberRole, AdminRolePagePermission>> permissionMap) {

        if (role == null || page == null) {
            return false;
        }

        if (role == MemberRole.ROOT) {
            return true;
        }

        Map<MemberRole, AdminRolePagePermission> roleMap =
                permissionMap.getOrDefault(page.getId(), new EnumMap<>(MemberRole.class));

        AdminRolePagePermission permission = roleMap.get(role);

        if (permission != null) {
            return permission.isCanView();
        }

        return legacyFallback(role, page.getPageUrl());
    }

    private String buildCollapseId(Integer menuOrder) {
        return "adminSidebarMenuGroup" + (menuOrder == null ? 0 : menuOrder);
    }

    private String resolveMenuGroupIcon(String menuGroupName) {
        if ("상품관리".equals(menuGroupName)) {
            return "ri-dashboard-2-line";
        }
        if ("회원관리".equals(menuGroupName)) {
            return "ri-team-line";
        }
        if ("사이트관리".equals(menuGroupName)) {
            return "ri-global-line";
        }
        if ("주문관리".equals(menuGroupName)) {
            return "ri-file-list-3-line";
        }
        if ("쇼핑몰관리".equals(menuGroupName)) {
            return "ri-settings-3-line";
        }

        return "ri-dashboard-2-line";
    }

    private AdminManagedPage upsertPage(SeedPage seed) {
        AdminManagedPage page = pageRepository.findByPageCode(seed.pageCode()).orElseGet(AdminManagedPage::new);

        page.setPageCode(seed.pageCode());
        page.setPageName(seed.pageName());
        page.setPageUrl(seed.pageUrl());
        page.setMenuGroupName(seed.menuGroupName());
        page.setMenuOrder(seed.menuOrder());
        page.setPageOrder(seed.pageOrder());
        page.setDescription(seed.description());
        page.setUseYn(true);

        return pageRepository.save(page);
    }

    private void upsertEndpoint(AdminManagedPage page, String httpMethod, String pathPattern,
            AdminPermissionAction action) {
        Optional<AdminManagedPageEndpoint> existing = endpointRepository
                .findByPageAndHttpMethodAndPathPatternAndAction(page, httpMethod, pathPattern, action);

        AdminManagedPageEndpoint endpoint = existing.orElseGet(AdminManagedPageEndpoint::new);
        endpoint.setPage(page);
        endpoint.setHttpMethod(httpMethod);
        endpoint.setPathPattern(pathPattern);
        endpoint.setAction(action);
        endpoint.setUseYn(true);

        endpointRepository.save(endpoint);
    }

    private void ensurePermissionRow(AdminManagedPage page, MemberRole role) {
        permissionRepository.findByPageAndRole(page, role).orElseGet(() -> {
            boolean allow = legacyFallback(role, page.getPageUrl());

            AdminRolePagePermission permission = AdminRolePagePermission.builder()
                    .page(page)
                    .role(role)
                    .canView(allow)
                    .canCreate(allow)
                    .canUpdate(allow)
                    .canDelete(allow)
                    .build();

            return permissionRepository.save(permission);
        });
    }

    private void applyPermission(AdminManagedPage page, MemberRole role,
            AdminRoleManagerSaveRequest.PermissionState state) {
        if (!MANAGEABLE_ROLES.contains(role)) {
            throw new IllegalArgumentException("관리할 수 없는 역할입니다. role=" + role);
        }

        NormalizedPermissionState normalized = normalize(state);

        AdminRolePagePermission permission = permissionRepository.findByPageAndRole(page, role)
                .orElseGet(() -> AdminRolePagePermission.builder().page(page).role(role).build());

        permission.setCanView(normalized.view());
        permission.setCanCreate(normalized.create());
        permission.setCanUpdate(normalized.update());
        permission.setCanDelete(normalized.delete());

        permissionRepository.save(permission);
    }

    private NormalizedPermissionState normalize(AdminRoleManagerSaveRequest.PermissionState state) {
        boolean view = state != null && state.isView();
        boolean create = state != null && state.isCreate();
        boolean update = state != null && state.isUpdate();
        boolean delete = state != null && state.isDelete();

        if (create || update || delete) {
            view = true;
        }

        return new NormalizedPermissionState(view, create, update, delete);
    }

    private AdminRoleManagerPageListResponse.PermissionState toPermissionState(AdminRolePagePermission permission) {
        if (permission == null) {
            return AdminRoleManagerPageListResponse.PermissionState.builder()
                    .view(false)
                    .create(false)
                    .update(false)
                    .delete(false)
                    .build();
        }

        return AdminRoleManagerPageListResponse.PermissionState.builder()
                .view(permission.isCanView())
                .create(permission.isCanCreate())
                .update(permission.isCanUpdate())
                .delete(permission.isCanDelete())
                .build();
    }

    private Optional<ResolvedEndpoint> resolveMappedEndpoint(String requestPath, String httpMethod) {
        List<AdminManagedPageEndpoint> endpoints = endpointRepository.findAllByUseYnTrueOrderByIdAsc();
        Comparator<String> patternComparator = antPathMatcher.getPatternComparator(requestPath);

        return endpoints.stream()
                .filter(endpoint -> endpoint.getHttpMethod() != null
                        && endpoint.getHttpMethod().equalsIgnoreCase(httpMethod))
                .filter(endpoint -> antPathMatcher.match(endpoint.getPathPattern(), requestPath))
                .sorted((a, b) -> patternComparator.compare(a.getPathPattern(), b.getPathPattern()))
                .findFirst()
                .map(endpoint -> new ResolvedEndpoint(endpoint.getPage(), endpoint.getAction()));
    }

    private boolean hasPermission(AdminManagedPage page, MemberRole role, AdminPermissionAction action) {
        Optional<AdminRolePagePermission> permissionOpt = permissionRepository.findByPageAndRole(page, role);

        if (permissionOpt.isEmpty()) {
            return false;
        }

        AdminRolePagePermission permission = permissionOpt.get();

        return switch (action) {
            case VIEW -> permission.isCanView();
            case CREATE -> permission.isCanCreate();
            case UPDATE -> permission.isCanUpdate();
            case DELETE -> permission.isCanDelete();
        };
    }

    private boolean legacyFallback(MemberRole role, String requestPath) {
        if (role == null) {
            return false;
        }

        if (role == MemberRole.ROOT) {
            return true;
        }

        if ("/admin".equals(requestPath) || "/admin/".equals(requestPath) || "/admin/main".equals(requestPath)) {
            return hasAnyRole(role, MemberRole.EMPLOY, MemberRole.OPERATOR, MemberRole.MASTER);
        }

        if (requestPath.startsWith("/admin/common/")) {
            return hasAnyRole(role, MemberRole.EMPLOY, MemberRole.OPERATOR, MemberRole.MASTER);
        }

        if (requestPath.startsWith("/admin/operator/")) {
            return hasAnyRole(role, MemberRole.OPERATOR, MemberRole.MASTER);
        }

        if (requestPath.startsWith("/admin/manager/") || requestPath.startsWith("/api/manager/")) {
            return hasAnyRole(role, MemberRole.EMPLOY, MemberRole.OPERATOR, MemberRole.MASTER);
        }

        if (requestPath.startsWith("/admin/master/")) {
            return hasAnyRole(role, MemberRole.MASTER);
        }

        if (requestPath.startsWith("/admin/root/")) {
            return false;
        }

        if (requestPath.startsWith("/admin/")) {
            return hasAnyRole(role, MemberRole.EMPLOY, MemberRole.OPERATOR, MemberRole.MASTER);
        }

        if (STANDALONE_MANAGED_PAGE_PATHS.contains(requestPath)) {
            return hasAnyRole(role, MemberRole.EMPLOY, MemberRole.OPERATOR, MemberRole.MASTER);
        }

        return false;
    }

    private boolean hasAnyRole(MemberRole currentRole, MemberRole... roles) {
        return Arrays.stream(roles).anyMatch(r -> r == currentRole);
    }

    private List<SeedPage> getSeedPages() {
        return List.of(
                new SeedPage(AdminPageCodes.PROD_BRAND_MANAGER, "브랜드관리", "/brandManager", "상품관리", 1, 1, "브랜드 관리 페이지"),
                new SeedPage(AdminPageCodes.PROD_CATEGORY_MANAGER, "분류관리", "/categoryManager", "상품관리", 1, 2, "분류 관리 페이지"),
                new SeedPage(AdminPageCodes.PROD_DISPLAY_MANAGER, "공통표시요소관리", "/displayManager", "상품관리", 1, 3, "공통 표시 요소 관리"),
                new SeedPage(AdminPageCodes.PROD_COUPON_MANAGER, "쿠폰관리", "/couponManager", "상품관리", 1, 4, "쿠폰 관리 페이지"),
                new SeedPage(AdminPageCodes.PROD_PROMOTION_MANAGER, "프로모션관리", "/productPromotionManager", "상품관리", 1, 5, "프로모션 관리 페이지"),
                new SeedPage(AdminPageCodes.PROD_INTERNAL_CATEGORY_MANAGER, "내부분류관리", "/internalCategoryManager", "상품관리", 1, 6, "내부분류 관리 페이지"),
                new SeedPage(AdminPageCodes.PROD_PRODUCT_INSERT, "상품등록", "/admin/productInsertForm", "상품관리", 1, 7, "상품 등록 페이지"),
                new SeedPage(AdminPageCodes.PROD_PRODUCT_LIST, "상품리스트", "/admin/productManager", "상품관리", 1, 8, "상품 리스트 페이지"),

                new SeedPage(AdminPageCodes.MEM_CLIENT_DASHBOARD, "회원대시보드", "/admin/root/clientDashBoard", "회원관리", 2, 1, "회원 대시보드"),
                new SeedPage(AdminPageCodes.MEM_CLIENT_SEARCH, "회원조회", "/admin/root/clientSearch", "회원관리", 2, 2, "회원 조회 페이지"),
                new SeedPage(AdminPageCodes.MEM_CLIENT_APPLY_MANAGER, "회원신청관리", "/admin/root/clientApplyManager", "회원관리", 2, 3, "회원 신청 관리"),
                new SeedPage(AdminPageCodes.MEM_CLIENT_COMPANY_TRANSFER_MANAGER, "기업회원전환관리", "/admin/root/clientCompanyTransferManager", "회원관리", 2, 4, "기업회원 전환 관리"),
                new SeedPage(AdminPageCodes.MEM_CLIENT_SELLER_TRANSFER_MANAGER, "판매딜러전환관리", "/admin/root/clientSellerTransferManager", "회원관리", 2, 5, "판매딜러 전환 관리"),
                new SeedPage(AdminPageCodes.MEM_CLIENT_WITHDRAW_MANAGER, "회원탈퇴관리", "/admin/root/clientWithDrawManager", "회원관리", 2, 6, "회원 탈퇴 관리"),
                new SeedPage(AdminPageCodes.MEM_SELLER_INSERT_FORM, "입점몰등록", "/admin/root/sellerInsertForm", "회원관리", 2, 7, "입점몰 등록 페이지"),
                
                new SeedPage(AdminPageCodes.SITE_EVENT_MANAGER, "이벤트관리", "/admin/manager/eventManager", "사이트관리", 3, 1, "이벤트 관리 페이지"),
                new SeedPage(AdminPageCodes.SITE_NOTICE_MANAGER, "공지사항관리", "/admin/manager/noticeManager", "사이트관리", 3, 2, "공지사항 관리 페이지"),
                new SeedPage(AdminPageCodes.SITE_QNA_CATEGORY_MANAGER, "QNA카테고리관리", "/admin/manager/qnaCategoryManager", "사이트관리", 3, 3, "QNA 카테고리 관리"),
                new SeedPage(AdminPageCodes.SITE_QNA_MANAGER, "QNA관리", "/admin/manager/qnaManager", "사이트관리", 3, 4, "QNA 관리 페이지"),

                new SeedPage(AdminPageCodes.ORDER_MANAGER, "주문관리", "/admin/root/orderManager", "주문관리", 4, 1, "주문 관리 페이지"),

                new SeedPage(AdminPageCodes.SHOP_MEMBER_INSERT_FORM, "직원등록", "/admin/root/memberInsertForm", "쇼핑몰관리", 5, 1, "직원 등록 페이지"),
                new SeedPage(AdminPageCodes.SHOP_SETTLEMENT_MANAGER, "정산관리", "/admin/root/settlementManager", "쇼핑몰관리", 5, 2, "정산 관리 페이지"),
                new SeedPage(AdminPageCodes.SHOP_SETTLEMENT_EXECUTE, "정산실행", "/admin/root/settlement", "쇼핑몰관리", 5, 3, "정산 실행 페이지"),
                new SeedPage(AdminPageCodes.SYSTEM_ROLE_MANAGER, "권한관리", "/admin/root/roleManager", "쇼핑몰관리", 5, 4, "권한 관리 페이지"),

                new SeedPage(AdminPageCodes.COMMON_MY_INFO, "내정보", "/admin/common/memberDetail/{id}", "공통페이지", 6, 1, "상단 내정보 페이지"),
                new SeedPage(AdminPageCodes.CRM_HOME, "CRM 홈", "/admin/root/clientDetail/{memberId}/home", "공통페이지", 6, 2, "고객 상세 CRM 홈"),
                new SeedPage(AdminPageCodes.CRM_INFORMATION, "회원기본정보", "/admin/root/clientDetail/{memberId}/information", "공통페이지", 6, 3, "고객 상세 회원기본정보"),
                new SeedPage(AdminPageCodes.CRM_ORDER_LIST, "주문내역", "/admin/root/clientDetail/{memberId}/orderList", "공통페이지", 6, 4, "고객 상세 주문내역"),
                new SeedPage(AdminPageCodes.CRM_REVIEW_LIST, "회원리뷰", "/admin/root/clientDetail/{memberId}/reviewList", "공통페이지", 6, 5, "고객 상세 회원리뷰"),
                new SeedPage(AdminPageCodes.CRM_ESTIMATE_LIST, "견적서문의", "/admin/root/clientDetail/{memberId}/estimateList", "공통페이지", 6, 6, "고객 상세 견적문의"),
                new SeedPage(AdminPageCodes.CRM_BENEFIT, "적립금 및 쿠폰", "/admin/root/clientDetail/{memberId}/benefit", "공통페이지", 6, 7, "고객 상세 적립금 및 쿠폰"),
                new SeedPage(AdminPageCodes.CRM_WISH_LIST, "관심상품열람", "/admin/root/clientDetail/{memberId}/wishList", "공통페이지", 6, 8, "고객 상세 관심상품열람"));
    }

    @Getter
    @AllArgsConstructor
    public static class AdminSidebarMenuGroup {
        private final String groupName;
        private final Integer menuOrder;
        private final String collapseId;
        private final String iconClass;
        private final List<AdminSidebarMenuItem> items;
    }

    @Getter
    @AllArgsConstructor
    public static class AdminSidebarMenuItem {
        private final String pageCode;
        private final String pageName;
        private final String pageUrl;
        private final Integer pageOrder;
    }

    private record SeedPage(String pageCode, String pageName, String pageUrl, String menuGroupName, Integer menuOrder,
            Integer pageOrder, String description) {
    }

    private record ResolvedEndpoint(AdminManagedPage page, AdminPermissionAction action) {
    }

    private record NormalizedPermissionState(boolean view, boolean create, boolean update, boolean delete) {
    }

    private record SidebarGroupAccumulator(
            String groupName,
            Integer menuOrder,
            String collapseId,
            String iconClass,
            List<AdminSidebarMenuItem> items) {
    }
}