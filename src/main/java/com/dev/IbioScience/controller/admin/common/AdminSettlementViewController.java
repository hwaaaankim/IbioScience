package com.dev.IbioScience.controller.admin.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/root")
public class AdminSettlementViewController {

    @GetMapping("/settlement")
    public String settlementExecutePage() {
        return "administration/shopManager/settlement/settlement";
    }

    @GetMapping("/settlementManager")
    public String settlementManagerPage() {
        return "administration/shopManager/settlement/settlementManager";
    }
}