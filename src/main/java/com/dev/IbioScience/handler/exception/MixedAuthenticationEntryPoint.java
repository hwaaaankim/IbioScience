package com.dev.IbioScience.handler.exception;

import java.io.IOException;
import java.util.Optional;

import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MixedAuthenticationEntryPoint implements org.springframework.security.web.AuthenticationEntryPoint {
	@Override
	public void commence(HttpServletRequest req, HttpServletResponse res,
			org.springframework.security.core.AuthenticationException ex) throws IOException, ServletException {
		if (isApiRequest(req)) {
			res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			res.setContentType("application/json;charset=UTF-8");
			res.getWriter().write(
					"{\"success\":false,\"status\":401,\"error\":\"Unauthorized\",\"message\":\"인증이 필요합니다.\",\"path\":\""
							+ req.getRequestURI() + "\"}");
		} else {
			res.sendRedirect("/signIn"); // 일반 페이지는 로그인 페이지로
		}
	}

	private boolean isApiRequest(HttpServletRequest req) {
		String uri = req.getRequestURI();
		String accept = Optional.ofNullable(req.getHeader("Accept")).orElse("");
		return uri.startsWith("/api/") || accept.contains("application/json")
				|| "XMLHttpRequest".equals(req.getHeader("X-Requested-With"));
	}
}
