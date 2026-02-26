package com.dev.IbioScience.handler.exception;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.dev.IbioScience.exception.InactiveMemberException;
import com.dev.IbioScience.exception.UseYnDisabledException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 로그인 실패 핸들러
 * - ✅ PENDING 상태일 때 "승인 대기중입니다." 메시지로 치환
 * - ✅ InternalAuthenticationServiceException(감싸진 예외)에서도 상태를 추출해 처리
 */
@Component
public class CustomAuthFailureHandler implements AuthenticationFailureHandler {

	@Override
	public void onAuthenticationFailure(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException exception
	) throws IOException, ServletException {

		String code;
		String msg;

		if (exception instanceof InternalAuthenticationServiceException) {

			Failure resolved = resolveFromInternal((InternalAuthenticationServiceException) exception);
			code = resolved.code;
			msg = resolved.msg;

		} else if (exception instanceof UsernameNotFoundException) {

			code = "NO_ID";
			msg = "존재하지 않는 아이디입니다. 회원가입을 진행해 주세요.";

		} else if (exception instanceof BadCredentialsException) {

			code = "BAD_PW";
			msg = "비밀번호가 올바르지 않습니다.";

		} else if (exception instanceof UseYnDisabledException) {

			code = "USE_YN_FALSE";
			msg = "사용불가능한 계정입니다. 관리자에게 문의하세요.";

		} else if (exception instanceof InactiveMemberException) {

			Failure resolved = resolveFromInactive((InactiveMemberException) exception);
			code = resolved.code;
			msg = resolved.msg;

		} else {

			code = "UNKNOWN";
			msg = "로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.";
		}

		String redirect = "/signIn?error=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
				+ "&msg=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);

		response.sendRedirect(redirect);
	}

	private Failure resolveFromInternal(InternalAuthenticationServiceException ex) {

		// 1) cause 체인에서 우선 탐색
		Throwable cause = ex.getCause();
		int depth = 0;

		while (cause != null && depth++ < 10) {

			if (cause instanceof UsernameNotFoundException) {
				return new Failure("NO_ID", "존재하지 않는 아이디입니다. 회원가입을 진행해 주세요.");
			}
			if (cause instanceof BadCredentialsException) {
				return new Failure("BAD_PW", "비밀번호가 올바르지 않습니다.");
			}
			if (cause instanceof UseYnDisabledException) {
				return new Failure("USE_YN_FALSE", "사용불가능한 계정입니다. 관리자에게 문의하세요.");
			}
			if (cause instanceof InactiveMemberException) {
				return resolveFromInactive((InactiveMemberException) cause);
			}

			cause = cause.getCause();
		}

		// 2) 못 찾으면 메시지에서 상태 추출
		String status = extractStatus(ex.getMessage());
		Failure f = fromStatus(status);
		if (f != null) return f;

		return new Failure("UNKNOWN", "로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.");
	}

	private Failure resolveFromInactive(InactiveMemberException ex) {
		String status = extractStatus(ex.getMessage());
		Failure f = fromStatus(status);
		if (f != null) return f;

		return new Failure("STATUS_BLOCK", "현재 계정 상태로 로그인할 수 없습니다. 관리자에게 문의하세요.");
	}

	/**
	 * ✅ 요구사항 상태별 메시지 매핑
	 */
	private Failure fromStatus(String status) {
		if (status == null || status.isBlank()) return null;

		String s = status.trim().toUpperCase();

		switch (s) {
			case "PENDING":
				return new Failure("STATUS_PENDING", "승인 대기중입니다.");
			case "SUSPENDED":
				return new Failure("STATUS_SUSPENDED", "접근이 정지 되었습니다. 관리자에게 문의하세요.");
			case "DORMANT":
				return new Failure("STATUS_DORMANT", "휴면 계정입니다. 관리자에게 문의 바랍니다.");
			case "DELETED":
				return new Failure("STATUS_DELETED", "삭제된 계정입니다. 관리자에게 문의 바랍니다.");
			default:
				// ACTIVE/WITHDRAWN은 원칙상 여기로 오지 않지만, 데이터 꼬임/예외 메시지 방어
				return new Failure("STATUS_BLOCK",
						"현재 계정 상태(" + s + ")로 로그인할 수 없습니다. 관리자에게 문의하세요.");
		}
	}

	private String extractStatus(String raw) {
		if (raw == null || raw.isBlank()) return null;

		int idx = raw.indexOf("계정 상태:");
		if (idx >= 0) {
			String tail = raw.substring(idx + "계정 상태:".length()).trim();
			return firstToken(tail);
		}

		idx = raw.indexOf("상태:");
		if (idx >= 0) {
			String tail = raw.substring(idx + "상태:".length()).trim();
			return firstToken(tail);
		}

		// 키워드 포함 방어
		if (raw.contains("PENDING")) return "PENDING";
		if (raw.contains("SUSPENDED")) return "SUSPENDED";
		if (raw.contains("DORMANT")) return "DORMANT";
		if (raw.contains("DELETED")) return "DELETED";
		if (raw.contains("WITHDRAWN")) return "WITHDRAWN";
		if (raw.contains("ACTIVE")) return "ACTIVE";

		return null;
	}

	private String firstToken(String s) {
		if (s == null) return null;
		String t = s.trim();
		if (t.isEmpty()) return null;

		String[] parts = t.split("[\\s\\)\\]\\}\\,]+");
		return (parts.length > 0) ? parts[0].trim() : t;
	}

	private static class Failure {
		final String code;
		final String msg;

		Failure(String code, String msg) {
			this.code = code;
			this.msg = msg;
		}
	}
}