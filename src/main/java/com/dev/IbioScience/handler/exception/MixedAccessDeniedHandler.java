package com.dev.IbioScience.handler.exception;

import java.io.IOException;
import java.util.Optional;

import org.springframework.stereotype.Component;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MixedAccessDeniedHandler implements org.springframework.security.web.access.AccessDeniedHandler {
	@Override
	public void handle(HttpServletRequest req, HttpServletResponse res,
			org.springframework.security.access.AccessDeniedException ex) throws IOException, ServletException {

		if (isApiRequest(req)) {
			res.setStatus(HttpServletResponse.SC_FORBIDDEN);
			res.setContentType("application/json;charset=UTF-8");
			res.getWriter().write(
					"{\"success\":false,\"status\":403,\"error\":\"Forbidden\",\"message\":\"접근 권한이 없습니다.\",\"path\":\""
							+ req.getRequestURI() + "\"}");
		} else {
			// 에러 페이지로 위임 (BasicErrorController or ErrorViewResolver 경유)
			req.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 403);
			req.getRequestDispatcher("/error").forward(req, res);
		}
	}

	private boolean isApiRequest(HttpServletRequest req) {
		String uri = req.getRequestURI();
		String accept = Optional.ofNullable(req.getHeader("Accept")).orElse("");
		return uri.startsWith("/api/") || accept.contains("application/json")
				|| "XMLHttpRequest".equals(req.getHeader("X-Requested-With"));
	}
}
