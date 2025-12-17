package com.dev.IbioScience.handler.exception;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.dev.IbioScience.exception.ProductNotDisplayableException;
import com.dev.IbioScience.exception.ProductNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalApiExceptionHandler {

	private ResponseEntity<Map<String, Object>> json(HttpStatus status, String message, HttpServletRequest req) {

		Map<String, Object> body = Map.of("success", false, "status", status.value(), "error", status.getReasonPhrase(),
				"message", message, "path", req != null ? req.getRequestURI() : "", "timestamp",
				LocalDateTime.now().toString());

		return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
	}

	/*
	 * ======================= 403 / 401 (Security 이후 단계) =======================
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {

		log.warn("AccessDeniedException: {}", ex.getMessage());
		return json(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.", req);
	}

	/*
	 * ======================= 404 - 상품 =======================
	 */
	@ExceptionHandler(ProductNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleProductNotFound(ProductNotFoundException ex,
			HttpServletRequest req) {

		log.warn("ProductNotFoundException: {}", ex.getMessage());
		return json(HttpStatus.NOT_FOUND, ex.getMessage(), req);
	}

	/*
	 * ======================= 403 - 상품 진열 불가 =======================
	 */
	@ExceptionHandler(ProductNotDisplayableException.class)
	public ResponseEntity<Map<String, Object>> handleProductNotDisplayable(ProductNotDisplayableException ex,
			HttpServletRequest req) {

		log.warn("ProductNotDisplayableException: productId={}", ex.getProductId());
		return json(HttpStatus.FORBIDDEN, ex.getMessage(), req);
	}

	/*
	 * ======================= 400 - 비즈니스 =======================
	 */
	@ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
	public ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException ex, HttpServletRequest req) {

		log.warn("BadRequest: {}", ex.getMessage());
		return json(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
	}

	/*
	 * ======================= 404 - 리소스 (API 요청에 한함) =======================
	 */
	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<Map<String, Object>> handleNoSuchElement(NoSuchElementException ex, HttpServletRequest req) {

		log.warn("NoSuchElementException: {}", ex.getMessage());
		return json(HttpStatus.NOT_FOUND, ex.getMessage(), req);
	}

	/*
	 * ======================= ❗ NoResourceFoundException (로그만 남기고 응답은 404)
	 * =======================
	 */
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex,
			HttpServletRequest req) {

		// 로그 레벨 낮춤 (정상 브라우저 동작)
		log.debug("NoResourceFound: {}", ex.getResourcePath());
		return json(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다.", req);
	}

	/*
	 * ======================= 500 - 최종 =======================
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleUnhandled(Exception ex, HttpServletRequest req) {

		log.error("Unhandled Exception", ex);
		return json(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", req);
	}
}