package com.dev.IbioScience.handler.exception;

import java.io.IOException;
import java.io.PrintWriter;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import com.dev.IbioScience.enums.auth.CustomerType;
import com.dev.IbioScience.enums.auth.MemberDomain;
import com.dev.IbioScience.enums.logging.MemberAuditAction;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.PrincipalDetails;
import com.dev.IbioScience.service.logging.MemberAuditLogService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

    // ✅ 감사로그 서비스
    private final MemberAuditLogService memberAuditLogService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        PrincipalDetails pd = (PrincipalDetails) authentication.getPrincipal();
        Member m = pd.getMember();

        // ✅ 0) 로그인 성공 로깅 (흐름을 방해하지 않도록 방어)
        try {
            CustomerType ct = m.getCustomerType();

            if (ct == CustomerType.BUSINESS) {
                memberAuditLogService.logEvent(m, MemberAuditAction.COMPANY_LOGIN, "LOGIN_SUCCESS", m.getId());
            } else if (ct == CustomerType.PERSONAL) {
                memberAuditLogService.logEvent(m, MemberAuditAction.PERSONAL_LOGIN, "LOGIN_SUCCESS", m.getId());
            } else if (ct == CustomerType.STAFF) {
                memberAuditLogService.logEvent(m, MemberAuditAction.STAFF_LOGIN, "LOGIN_SUCCESS", m.getId());
            } else {
                memberAuditLogService.logEvent(m, MemberAuditAction.OTHER, "LOGIN_SUCCESS", m.getId());
            }
        } catch (Exception ignore) {
            // 로깅 실패가 로그인 흐름을 막으면 안됨
        }

        // 1) 최초 로그인 시 비밀번호 변경 강제
        if (m.isMustChangePassword()) {
            String targetUrl;

            if (m.getDomain() == MemberDomain.CUSTOMER) {
                targetUrl = "/customer/companyInfoUpdate/" + m.getId();
            } else {
                targetUrl = "/admin/common/memberDetail/" + m.getId();
            }

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
                out.println("window.location.replace('" + targetUrl + "');");
                out.println("</script>");
                out.println("</body></html>");
                out.flush();
            }
            return;
        }

        // 2) SavedRequest 우선
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            String targetUrl = savedRequest.getRedirectUrl();
            response.sendRedirect(targetUrl);
            return;
        }

        // 3) prevPage
        HttpSession session = request.getSession(false);
        if (session != null) {
            String prevPage = (String) session.getAttribute("prevPage");
            if (prevPage != null && !prevPage.isBlank()) {
                session.removeAttribute("prevPage");
                response.sendRedirect(prevPage);
                return;
            }
        }

        // 4) default
        response.sendRedirect("/");
    }
}