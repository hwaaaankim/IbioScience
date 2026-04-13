package com.dev.IbioScience.controller.admin.common;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/root")
@PreAuthorize("hasRole('ROOT')")
public class AdminRoleManagerController {

    @GetMapping("/roleManager")
    public String roleManagerPage() {
        return "administration/shopManager/roleManager";
    }
}