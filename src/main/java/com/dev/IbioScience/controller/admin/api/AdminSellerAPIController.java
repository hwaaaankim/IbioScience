package com.dev.IbioScience.controller.admin.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.admin.client.AdminSellerCreateRequest;
import com.dev.IbioScience.service.auth.admin.client.AdminSellerCreateService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/root/sellers")
@RequiredArgsConstructor
public class AdminSellerAPIController {

	private final AdminSellerCreateService adminSellerCreateService;

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Map<String, Object>> createSeller(@ModelAttribute AdminSellerCreateRequest request) {
		try {
			Long memberId = adminSellerCreateService.createSeller(request);

			Map<String, Object> body = new LinkedHashMap<>();
			body.put("success", true);
			body.put("message", "판매회원이 정상 등록되었습니다.");
			body.put("memberId", memberId);

			return ResponseEntity.ok(body);
		} catch (IllegalArgumentException e) {
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("success", false);
			body.put("message", e.getMessage());
			return ResponseEntity.badRequest().body(body);
		} catch (Exception e) {
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("success", false);
			body.put("message", "판매회원 등록 중 오류가 발생했습니다.");
			return ResponseEntity.internalServerError().body(body);
		}
	}
}