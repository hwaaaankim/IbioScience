package com.dev.IbioScience.controller.admin.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminEntryCommonController {

	/** /admin → /admin/common/main */
    @GetMapping({"/admin", "/admin/"})
    public String admin() {
        return "redirect:/admin/common/main";
    }

    /** /admin/main → /admin/common/main */
    @GetMapping("/admin/main")
    public String adminMain() {
        return "redirect:/admin/common/main";
    }
}
