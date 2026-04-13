package com.dev.IbioScience.config;

import java.util.function.Supplier;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.PrincipalDetails;
import com.dev.IbioScience.service.auth.role.AdminRoleManagerService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminDynamicAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final AdminRoleManagerService adminRoleManagerService;

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier,
                                       RequestAuthorizationContext context) {

        Authentication authentication = authenticationSupplier.get();
        HttpServletRequest request = context.getRequest();

        if (authentication == null || !authentication.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof PrincipalDetails principalDetails)) {
            return new AuthorizationDecision(false);
        }

        Member member = principalDetails.getMember();
        String requestPath = request.getServletPath();
        String httpMethod = request.getMethod();

        boolean allowed = adminRoleManagerService.isAllowed(member, requestPath, httpMethod);

        return new AuthorizationDecision(allowed);
    }
}