package com.dev.IbioScience.handler.exception;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.dev.IbioScience.exception.InactiveMemberException;
import com.dev.IbioScience.exception.UseYnDisabledException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthFailureHandler implements AuthenticationFailureHandler {

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws java.io.IOException, ServletException {

		String code;
		String msg;

		if (exception instanceof UsernameNotFoundException) {
			code = "NO_ID";
			msg = "존재하지 않는 아이디입니다. 회원가입을 진행해 주세요.";
		} else if (exception instanceof BadCredentialsException) {
			code = "BAD_PW";
			msg = "비밀번호가 올바르지 않습니다.";
		} else if (exception instanceof UseYnDisabledException) {
			code = "USE_YN_FALSE";
			msg = "사용불가능한 계정입니다. 관리자에게 문의하세요.";
		} else if (exception instanceof InactiveMemberException) {
			code = "STATUS_BLOCK";
			// 예: "사용할 수 없는 계정 상태: DORMANT" → 보다 친절한 문구로 가공
			String raw = exception.getMessage();
			if (raw != null && raw.contains("계정 상태:")) {
				String status = raw.substring(raw.indexOf("계정 상태:") + 6).trim();
				msg = "현재 계정 상태(" + status.replace("계정 상태:", "").trim() + ")로 로그인할 수 없습니다. 관리자에게 문의하세요.";
			} else {
				msg = "현재 계정 상태로 로그인할 수 없습니다. 관리자에게 문의하세요.";
			}
		} else {
			code = "UNKNOWN";
			msg = "로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.";
		}

		String redirect = "/signIn?error=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
				+ "&msg=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
		response.sendRedirect(redirect);
	}
}
