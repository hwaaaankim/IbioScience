package com.dev.IbioScience.controller.admin.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.auth.role.AdminRoleManagerPageListResponse;
import com.dev.IbioScience.dto.auth.role.AdminRoleManagerSaveRequest;
import com.dev.IbioScience.service.auth.role.AdminRoleManagerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/root/api/role-manager")
@PreAuthorize("hasRole('ROOT')")
public class AdminRoleManagerApiController {

    private final AdminRoleManagerService adminRoleManagerService;

    @GetMapping("/pages")
    public ResponseEntity<AdminRoleManagerPageListResponse> getPages() {
        return ResponseEntity.ok(adminRoleManagerService.getPagePermissions());
    }

    @PostMapping("/permissions")
    public ResponseEntity<Map<String, Object>> savePermissions(@RequestBody AdminRoleManagerSaveRequest request) {
        adminRoleManagerService.savePermissions(request);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", "권한이 정상 저장되었습니다.");

        return ResponseEntity.ok(body);
    }
}