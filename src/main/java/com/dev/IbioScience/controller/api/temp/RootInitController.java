package com.dev.IbioScience.controller.api.temp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.member.auth.RootInitResponse;
import com.dev.IbioScience.service.auth.temp.RootInitService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/temp/api/root")
@RequiredArgsConstructor
public class RootInitController {

	private final RootInitService rootInitService;

	@GetMapping("/init")
	public ResponseEntity<RootInitResponse> initRoot() {
		RootInitResponse res = rootInitService.createRootIfAbsent();
		return ResponseEntity.status(res.created() ? 201 : 200).body(res);
	}
}