package com.dev.IbioScience.controller.admin.member;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dev.IbioScience.dto.member.auth.StaffCreateRequest;
import com.dev.IbioScience.service.auth.AdminUserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/root")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminUserService adminUserService;

    @PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).SHOP_MEMBER_INSERT_FORM)")
    @GetMapping("/memberInsertForm")
    public String memberInsertForm() {
        return "administration/shopManager/memberInsertForm";
    }

    @PreAuthorize("@adminMenuFacade.canCreateByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).SHOP_MEMBER_INSERT_FORM)")
    @PostMapping("/memberInsert")
    @ResponseBody
    public String memberInsert(@Valid @ModelAttribute StaffCreateRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return """
                    <script>
                      alert('입력값을 다시 확인해 주세요.');
                      history.back();
                    </script>
                    """;
        }

        if (adminUserService.existsUsername(request.getUsername())) {
            return """
                    <script>
                      alert('이미 사용 중인 아이디입니다.');
                      history.back();
                    </script>
                    """;
        }

        adminUserService.createStaff(request);

        return """
                <script>
                  alert('직원 등록이 완료 되었습니다.');
                  location.href='/admin/common/main';
                </script>
                """;
    }
}