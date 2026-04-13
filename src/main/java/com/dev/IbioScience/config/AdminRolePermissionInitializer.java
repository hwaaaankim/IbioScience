package com.dev.IbioScience.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.dev.IbioScience.service.auth.role.AdminRoleManagerService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminRolePermissionInitializer implements ApplicationRunner {

    private final AdminRoleManagerService adminRoleManagerService;

    @Override
    public void run(ApplicationArguments args) {
        adminRoleManagerService.syncDefaultPagesAndPermissions();
    }
}