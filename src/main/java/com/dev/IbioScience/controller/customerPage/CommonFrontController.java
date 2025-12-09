package com.dev.IbioScience.controller.customerPage;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dev.IbioScience.dto.customer.auth.CompanySignUpRequest;
import com.dev.IbioScience.dto.customer.auth.PersonalSignUpRequest;
import com.dev.IbioScience.service.auth.customer.common.CustomerSignUpService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CommonFrontController {

	private final CustomerSignUpService signUpService;
	private static final Logger log = LoggerFactory.getLogger(CommonFrontController.class);

	@GetMapping("/signIn")
    public String signIn(HttpServletRequest request) {

        HttpSession session = request.getSession();

        // 로그인 페이지로 들어오기 직전 페이지 (Referer) 저장
        String referrer = request.getHeader("Referer");
        if (referrer != null &&
                !referrer.contains("/signIn") &&
                !referrer.contains("/signOut") &&
                !referrer.contains("/logout")) {

            session.setAttribute("prevPage", referrer);
        }

        return "front/common/signIn";
    }

	@GetMapping("/personalSignUp")
	public String personalSignUp() {

		return "front/common/personalSignUp";
	}

	@GetMapping("/companySignUp")
	public String companySignUp() {

		return "front/common/companySignUp";
	}

	/** 개인회원 가입 처리 */
	@PostMapping("/signUpProcess/personal")
	public String signUpProcessPersonal(@Valid @ModelAttribute PersonalSignUpRequest form, BindingResult bindingResult,
			Model model, RedirectAttributes ra, HttpServletRequest request) {

		if (bindingResult.hasErrors()) {
			ra.addFlashAttribute("alertMessage", "입력값을 다시 확인해 주세요.");
			return "redirect:" + backTo(request, "/personalSignUp");
		}

		try {
			var member = signUpService.registerPersonal(form);
			model.addAttribute("name", member.getName());
			return "front/common/signUpSuccess";
		} catch (IllegalArgumentException dup) {
			ra.addFlashAttribute("alertMessage", "이미 사용 중인 아이디입니다.");
			return "redirect:" + backTo(request, "/personalSignUp");
		} catch (Exception e) {
			e.printStackTrace();
			ra.addFlashAttribute("alertMessage", "회원가입 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
			return "redirect:" + backTo(request, "/personalSignUp");
		}
	}

	/** 기업회원 가입 처리(폼 제출 + DTO 바인딩) */
	@PostMapping("/signUpProcess/company")
	public String signUpProcessCompany(@Valid @ModelAttribute CompanySignUpRequest form,
	                                   BindingResult bindingResult,
	                                   Model model,
	                                   RedirectAttributes ra,
	                                   HttpServletRequest request) {

		if (bindingResult.hasErrors()) {
			// 🔎 바인딩 에러 상세 로깅
			String errors = bindingResult.getAllErrors().stream()
					.map(e -> e.getObjectName() + ":" + e.getDefaultMessage())
					.collect(Collectors.joining(" | "));
			log.warn("[CompanySignUp] Binding errors -> {}", errors);
			ra.addFlashAttribute("alertMessage", "입력값을 다시 확인해 주세요.");
			return "redirect:" + backTo(request, "/companySignUp");
		}

		try {
			var member = signUpService.registerCompany(form);
			model.addAttribute("name", member.getName());
			return "front/common/signUpSuccess";
		} catch (IllegalArgumentException dup) {
			log.warn("[CompanySignUp] Business validation failed: {}", dup.getMessage());
			ra.addFlashAttribute("alertMessage", dup.getMessage());
			return "redirect:" + backTo(request, "/companySignUp");
		} catch (Exception e) {
			log.error("[CompanySignUp] Unexpected error", e);
			ra.addFlashAttribute("alertMessage", "회원가입 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
			return "redirect:" + backTo(request, "/companySignUp");
		}
	}

	/** 뒤로 보낼 경로 계산: Referer 우선, 없으면 폴백 */
	private String backTo(HttpServletRequest request, String fallback) {
		String ref = request.getHeader("Referer");
		return (ref != null && !ref.isBlank()) ? ref : fallback;
	}
}
