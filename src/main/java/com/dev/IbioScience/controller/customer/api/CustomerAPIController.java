package com.dev.IbioScience.controller.customer.api;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.model.auth.PrincipalDetails;
import com.dev.IbioScience.model.auth.utils.DealerApplyRequest;
import com.dev.IbioScience.model.auth.utils.DealerApplyResponse;
import com.dev.IbioScience.model.auth.utils.DealerConversionApplication;
import com.dev.IbioScience.service.auth.AdminUserService;
import com.dev.IbioScience.service.auth.utils.DealerApplicationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customer")
public class CustomerAPIController {

	private final AdminUserService adminUserService;
	private final DealerApplicationService dealerApplicationService;

	@GetMapping("/username-exists")
	public ResponseEntity<Map<String, Boolean>> usernameExists(@RequestParam("username") String username) {
		boolean exists = adminUserService.existsUsername(username.trim());
		return ResponseEntity.ok(Map.of("exists", exists));
	}
	
	/** 딜러 전환 신청 (JSON) */
    @PostMapping(
        value = "/apply",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DealerApplyResponse> apply(
            @Valid @RequestBody DealerApplyRequest req,
            @AuthenticationPrincipal PrincipalDetails principal) {

        if (principal == null) {
            return ResponseEntity.status(401).body(
                DealerApplyResponse.builder()
                    .success(false)
                    .message("로그인이 필요합니다.")
                    .build()
            );
        }

        try {
            DealerConversionApplication app = dealerApplicationService.apply(
                principal.getMember().getId(), req);

            return ResponseEntity.ok(
                DealerApplyResponse.builder()
                    .success(true)
                    .message("신청이 접수되었습니다. (상태: " + app.getStatus() + ")")
                    .applicationId(app.getId())
                    .status(app.getStatus().name())
                    .build()
            );

        } catch (IllegalArgumentException iae) {
            // 비즈니스 룰 위반: 400 (예: 중복 대기, 자격 미달 등)
            return ResponseEntity.badRequest().body(
                DealerApplyResponse.builder()
                    .success(false)
                    .message(iae.getMessage())
                    .build()
            );
        } catch (Exception e) {
            // 서버 오류: 500
            return ResponseEntity.status(500).body(
                DealerApplyResponse.builder()
                    .success(false)
                    .message("신청 처리 중 서버 오류가 발생했습니다.")
                    .build()
            );
        }
    }
}
