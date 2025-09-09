package com.dev.IbioScience.controller.customer.api;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.service.auth.AdminUserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customer")
public class CustomerAPIController {

	private final AdminUserService adminUserService;
	
	@GetMapping("/username-exists")
	public ResponseEntity<Map<String, Boolean>> usernameExists(@RequestParam("username") String username) {
		boolean exists = adminUserService.existsUsername(username.trim());
		return ResponseEntity.ok(Map.of("exists", exists));
	}
}
