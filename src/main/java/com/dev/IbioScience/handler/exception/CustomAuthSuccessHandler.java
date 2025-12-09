package com.dev.IbioScience.handler.exception;

import java.io.IOException;
import java.io.PrintWriter;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.PrincipalDetails;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        PrincipalDetails pd = (PrincipalDetails) authentication.getPrincipal();
        Member m = pd.getMember();

        // 1) 최초 로그인 시 비밀번호 변경 강제
        if (m.isMustChangePassword()) {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/html;charset=UTF-8");
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");

            try (PrintWriter out = response.getWriter()) {
                out.println("<!DOCTYPE html>");
                out.println("<html lang='ko'><head><meta charset='UTF-8'><title>안내</title></head>");
                out.println("<body>");
                out.println("<script>");
                out.println("alert('최초 로그인 시 계정정보를 변경해야 합니다.');");
                out.println("window.location.replace('/admin/common/memberDetail/" + m.getId() + "');");
                out.println("</script>");
                out.println("</body></html>");
                out.flush();
            }
            return;
        }

        // 2) Security 가 저장해 둔 요청(SavedRequest)이 있는 경우 → 그쪽으로 우선 이동
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            String targetUrl = savedRequest.getRedirectUrl();
            response.sendRedirect(targetUrl);
            return;
        }

        // 3) SavedRequest 가 없으면, 우리가 세션에 저장해 둔 prevPage 로 이동
        HttpSession session = request.getSession(false);
        if (session != null) {
            String prevPage = (String) session.getAttribute("prevPage");
            if (prevPage != null && !prevPage.isBlank()) {
                session.removeAttribute("prevPage");
                response.sendRedirect(prevPage);
                return;
            }
        }

        // 4) 그 외에는 기본적으로 메인으로 이동
        response.sendRedirect("/");
    }
}