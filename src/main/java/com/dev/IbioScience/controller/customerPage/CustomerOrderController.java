package com.dev.IbioScience.controller.customerPage;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dev.IbioScience.dto.order.OrderCreateRequestDTO;
import com.dev.IbioScience.dto.order.OrderCreateResponseDTO;
import com.dev.IbioScience.dto.order.OrderDetailViewDTO;
import com.dev.IbioScience.dto.order.OrderStatusUpdateResponseDTO;
import com.dev.IbioScience.service.order.OrderService;

@Controller
@RequestMapping("/customer")
public class CustomerOrderController {

    private final OrderService orderService;

    public CustomerOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ✅ 환님 PaymentFrontController 유틸 그대로 사용한다고 하셨으니,
    // 여기서는 동일 메서드가 있다고 가정하지 않고,
    // "이 컨트롤러에 같은 유틸을 복사"해서 사용하세요.
    /**
	 * principal에서 member.id 를 최대한 안전하게 꺼냅니다. - #authentication.principal.member.id
	 * 를 타임리프에서 쓰고 계시므로, 동일 경로를 우선 시도합니다.
	 */
	private Long resolveLoginMemberId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated())
			return null;

		Object principal = auth.getPrincipal();
		if (principal == null)
			return null;

		try {
			BeanWrapper bw = new BeanWrapperImpl(principal);

			// 1) principal.member.id
			if (bw.isReadableProperty("member.id")) {
				Object v = bw.getPropertyValue("member.id");
				return toLong(v);
			}

			// 2) principal.id (혹시 이런 구조일 경우 대비)
			if (bw.isReadableProperty("id")) {
				Object v = bw.getPropertyValue("id");
				return toLong(v);
			}

		} catch (Exception ignored) {
		}

		return null;
	}

	private Long toLong(Object v) {
		if (v == null)
			return null;
		if (v instanceof Long)
			return (Long) v;
		if (v instanceof Integer)
			return ((Integer) v).longValue();
		if (v instanceof Number)
			return ((Number) v).longValue();
		try {
			return Long.parseLong(String.valueOf(v));
		} catch (Exception e) {
			return null;
		}
	}

    // =========================
    // (기존) paymentStart 페이지 진입 (환님 코드 유지)
    // GET /customer/paymentStart
    // =========================
    @GetMapping("/paymentStart")
    public String paymentStart(Model model, RedirectAttributes redirectAttributes) {

        Long loginMemberId = resolveLoginMemberId();
        if (loginMemberId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/";
        }

        model.addAttribute("loginMemberId", loginMemberId);
        return "front/payment/paymentStart";
    }

    // =========================
    // 1) 주문 생성 (결제대기 저장)
    // POST /customer/api/orders
    // =========================
    @PostMapping("/api/orders")
    @ResponseBody
    public ResponseEntity<OrderCreateResponseDTO> createOrder(@RequestBody OrderCreateRequestDTO req) {
        Long loginMemberId = resolveLoginMemberId();
        OrderCreateResponseDTO res = orderService.createPendingOrder(loginMemberId, req);
        return ResponseEntity.ok(res);
    }

    // =========================
    // 2) 결제완료 처리
    // PATCH /customer/api/orders/{orderNo}/payment-complete
    // =========================
    @PatchMapping("/api/orders/{orderNo}/payment-complete")
    @ResponseBody
    public ResponseEntity<OrderStatusUpdateResponseDTO> paymentComplete(@PathVariable String orderNo) {
        Long loginMemberId = resolveLoginMemberId();
        OrderStatusUpdateResponseDTO res = orderService.markPaymentCompleted(loginMemberId, orderNo);
        return ResponseEntity.ok(res);
    }

    // =========================
    // 3) 결제에러 처리
    // PATCH /customer/api/orders/{orderNo}/payment-error
    // =========================
    @PatchMapping("/api/orders/{orderNo}/payment-error")
    @ResponseBody
    public ResponseEntity<OrderStatusUpdateResponseDTO> paymentError(@PathVariable String orderNo) {
        Long loginMemberId = resolveLoginMemberId();
        OrderStatusUpdateResponseDTO res = orderService.markPaymentError(loginMemberId, orderNo);
        return ResponseEntity.ok(res);
    }

    // =========================
    // 4) 결제완료/에러 페이지
    // GET /customer/paymentEnd/{orderNo}
    // =========================
    @GetMapping("/paymentEnd/{orderNo}")
    public String paymentEnd(@PathVariable String orderNo, Model model, RedirectAttributes redirectAttributes) {

        Long loginMemberId = resolveLoginMemberId();
        if (loginMemberId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/";
        }

        OrderDetailViewDTO detail = orderService.getOrderDetailForView(loginMemberId, orderNo);
        model.addAttribute("orderDetail", detail);

        return "front/payment/paymentEnd";
    }
}