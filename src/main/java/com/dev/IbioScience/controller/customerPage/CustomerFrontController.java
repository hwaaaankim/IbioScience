package com.dev.IbioScience.controller.customerPage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.customer.auth.CompanyInfoUpdateRequest;
import com.dev.IbioScience.dto.customer.auth.CompanyInfoUpdateResponse;
import com.dev.IbioScience.dto.customer.auth.ConversionToCompanyRequest;
import com.dev.IbioScience.dto.customer.auth.CustomerPersonalInfoUpdateRequest;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.PrincipalDetails;
import com.dev.IbioScience.service.auth.customer.common.CustomerCompanyConversionService;
import com.dev.IbioScience.service.auth.customer.common.CustomerCompanyInfoService;
import com.dev.IbioScience.service.auth.customer.common.CustomerUpdateService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerFrontController {

	private final CustomerUpdateService customerUpdateService;
	private final CustomerCompanyInfoService customerCompanyInfoService;
	private final CustomerCompanyConversionService conversionService;
	private static final Logger log = LoggerFactory.getLogger(CustomerFrontController.class);
	
	@GetMapping("/couponList/{id}")
	public String couponList(@PathVariable Long id) {
		return "front/customer/couponList";
	}

	@GetMapping("/estimateList/{id}")
	public String estimateList(@PathVariable Long id) {
		return "front/customer/estimateList";
	}

	@GetMapping("/estimate/{id}")
	public String estimate(@PathVariable Long id) {
		return "front/customer/estimate";
	}

	@GetMapping("/exchangeReturnList/{id}")
	public String exchangeReturnList(@PathVariable Long id) {
		return "front/customer/exchangeReturnList";
	}

	@GetMapping("/exchangeReturn/{id}")
	public String exchangeReturn(@PathVariable Long id) {
		return "front/customer/exchangeReturn";
	}

	/** 화면: 본인 정보 로딩 및 바인딩 */
	@GetMapping("/personalInfoUpdate/{id}")
	public String personalInfoUpdate(@PathVariable Long id, @AuthenticationPrincipal PrincipalDetails principal,
			Model model) {
		Member member = customerUpdateService.getMemberOrThrow(id);

		if (principal == null || !member.getId().equals(principal.getMember().getId())) {
			throw new SecurityException("접근 권한이 없습니다.");
		}

		model.addAttribute("member", member);
		return "front/customer/personalInfoUpdate";
	}

	/** 처리: 업데이트 */
	@PostMapping("/personalInfoUpdateProcess")
	@ResponseBody
	public String personalInfoUpdateProcess(@Valid @ModelAttribute CustomerPersonalInfoUpdateRequest form,
			BindingResult bindingResult, @AuthenticationPrincipal PrincipalDetails principal) {

		if (bindingResult.hasErrors()) {
			return """
					<script>
					  alert('입력값을 다시 확인해 주세요.');
					  history.back();
					</script>
					""";
		}

		try {
			Member updated = customerUpdateService.updatePersonal(principal.getMember().getId(), form);
			return """
					<script>
					  alert('정보 변경 완료되었습니다.');
					  window.location.replace('/customer/personalInfoUpdate/%d');
					</script>
					""".formatted(updated.getId());
		} catch (IllegalArgumentException e) {
			return """
					<script>
					  alert('%s');
					  history.back();
					</script>
					""".formatted(e.getMessage().replace("'", "\\'"));
		} catch (SecurityException se) {
			return """
					<script>
					  alert('접근 권한이 없습니다.');
					  history.back();
					</script>
					""";
		} catch (Exception ex) {
			ex.printStackTrace();
			return """
					<script>
					  alert('처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.');
					  history.back();
					</script>
					""";
		}
	}

	/** 페이지 진입: id 기준으로 바인딩 */
    @GetMapping("/companyInfoUpdate/{id}")
    public String companyInfoUpdate(@PathVariable("id") Long memberId,
                                    @AuthenticationPrincipal PrincipalDetails principal,
                                    Model model) {

        // 권한 방어: 자기 자신 or ROOT/MASTER 등(필요 시 추가)
        // 본 예제에서는 자기 자신만 수정 가능하다고 가정
        if (principal == null || !principal.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        CompanyInfoUpdateResponse resp = customerCompanyInfoService.loadCompanyInfoOrThrow(memberId);
        model.addAttribute("init", resp);
        model.addAttribute("memberId", memberId);
        return "front/customer/companyInfoUpdate";
    }

    /** 수정 처리: 멀티파트(사업자등록증 신규 업로드 선택) */
    @PostMapping(
        value = "/companyInfoUpdate/{id}",
        produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8"
    )
    @ResponseBody
    public String companyInfoUpdateProcess(@PathVariable("id") Long memberId,
                                           @Valid @ModelAttribute CompanyInfoUpdateRequest form,
                                           BindingResult bindingResult,
                                           @RequestParam(value = "bizRegFile", required = false) MultipartFile bizRegFile,
                                           @AuthenticationPrincipal PrincipalDetails principal) {

        // 1) 기본 검증 실패
        if (bindingResult.hasErrors()) {
            return alertBack("필수 항목을 확인해 주세요.");
        }

        // 2) 권한 확인: 본인만 수정 가능(요구사항에 맞춰 조정)
        if (principal == null || principal.getMember() == null
                || !principal.getMember().getId().equals(memberId)) {
            return alertBack("권한이 없습니다.");
        }

        try {
            // 3) 서버 처리
            customerCompanyInfoService.updateCompanyInfo(memberId, form, bizRegFile);

            // 4) 성공: 알림 후 동일 페이지 재진입(최신 데이터)
            String redirectUrl = "/customer/companyInfoUpdate/" + memberId;
            return alertAndRedirect("정보수정이 완료되었습니다.", redirectUrl);

        } catch (IllegalArgumentException e) {
            // 비즈니스/입력 예외: 메시지 안내 후 뒤로가기
            return alertBack(safeJs(e.getMessage()));
        } catch (Exception e) {
            log.error("회사 정보 수정 처리 중 예외", e);
            return alertBack("처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    /* ===== JS 스니펫 헬퍼 ===== */

    /** alert 후 history.back() */
    private String alertBack(String msg) {
        return """
               <script>
                 alert('%s');
                 history.back();
               </script>
               """.formatted(safeJs(msg));
    }

    /** alert 후 location.replace(url) */
    private String alertAndRedirect(String msg, String url) {
        return """
               <script>
                 alert('%s');
                 location.replace('%s');
               </script>
               """.formatted(safeJs(msg), safeJs(url));
    }

    /** JS 문자열 이스케이프(기본 수준) */
    private String safeJs(String s) {
        if (s == null) return "";
        return s
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\r", "")
            .replace("\n", "\\n");
    }

    /** 기업회원 전환 화면 (본인만 접근) */
    @GetMapping("/conversionToCompany/{id}")
    public String conversionToCompany(@PathVariable Long id,
                                      @AuthenticationPrincipal PrincipalDetails principal,
                                      Model model) {
        if (principal == null || !principal.getMember().getId().equals(id)) {
            // 권한 없음: 공용 에러 처리 or 로그인 페이지 유도
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        model.addAttribute("memberId", id);
        return "front/customer/conversionToCompany";
    }

    @PostMapping(value = "/conversionToCompanyProcess", produces = "text/html; charset=UTF-8")
    @ResponseBody
    public ResponseEntity<String> conversionToCompanyProcess(
            @Valid @ModelAttribute ConversionToCompanyRequest form,
            @RequestParam("bizRegFiles") MultipartFile[] bizRegFiles,
            @AuthenticationPrincipal PrincipalDetails principal) {

        // 공통: UTF-8 Content-Type
        MediaType htmlUtf8 = MediaType.parseMediaType("text/html; charset=UTF-8");

        if (principal == null) {
            String html = "<script>"
                    + "alert('로그인이 필요합니다.');"
                    + "location.href='/signIn';"
                    + "</script>";
            return ResponseEntity.ok()
                    .contentType(htmlUtf8)
                    .body(html);
        }

        try {
            conversionService.convertToCompany(principal.getMember().getId(), form, bizRegFiles);

            // ✅ 성공 시 홈(/)으로 이동
            String html = "<script>"
                    + "alert('기업회원 전환 신청이 완료되었습니다.');"
                    + "window.location.replace('/');"
                    + "</script>";

            return ResponseEntity.ok()
                    .contentType(htmlUtf8)
                    .body(html);

        } catch (IllegalArgumentException e) {
            String safeMsg = sanitizeForAlert(e.getMessage());

            String html = "<script>"
                    + "alert('" + safeMsg + "');"
                    + "history.back();"
                    + "</script>";

            return ResponseEntity.ok()
                    .contentType(htmlUtf8)
                    .body(html);

        } catch (Exception e) {
            e.printStackTrace();

            String html = "<script>"
                    + "alert('처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.');"
                    + "history.back();"
                    + "</script>";

            return ResponseEntity.ok()
                    .contentType(htmlUtf8)
                    .body(html);
        }
    }

    /** alert()에 넣을 문자열을 간단히 이스케이프 */
    private static String sanitizeForAlert(String msg) {
        if (msg == null || msg.isBlank()) return "요청이 올바르지 않습니다.";
        String s = msg;
        s = s.replace("\\", "\\\\");   // 역슬래시
        s = s.replace("'", "\\'");     // 작은따옴표
        s = s.replace("\r", "").replace("\n", "\\n"); // 줄바꿈
        s = s.replace("</script>", "<\\/script>");    // 스크립트 조기 종료 방지
        return s;
    }


    
	@GetMapping("/inquiry/{id}")
	public String inquiry(@PathVariable Long id) {
		return "front/customer/inquiry";
	}

	@GetMapping("/inquiryList/{id}")
	public String inquiryList(@PathVariable Long id) {
		return "front/customer/inquiryList";
	}

	@GetMapping("/myPage/{id}")
	public String myPage(@PathVariable Long id) {
		return "front/customer/myPage";
	}

	@GetMapping("/orderList/{id}")
	public String orderList(@PathVariable Long id) {
		return "front/customer/orderList";
	}

	@GetMapping("/pointList/{id}")
	public String pointList(@PathVariable Long id) {
		return "front/customer/pointList";
	}

	@GetMapping("/reviewList/{id}")
	public String reviewList(@PathVariable Long id) {
		return "front/customer/reviewList";
	}

	@GetMapping("/wishList/{id}")
	public String wishList(@PathVariable Long id) {
		return "front/customer/wishList";
	}

}
