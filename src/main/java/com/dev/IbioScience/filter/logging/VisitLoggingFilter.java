package com.dev.IbioScience.filter.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.dev.IbioScience.service.logging.VisitCounterService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VisitLoggingFilter extends OncePerRequestFilter {

    private static final String VISITOR_COOKIE_NAME = "VISITOR_ID";
    private static final int VISITOR_COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 365; // 1년

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final Set<String> EXCLUDE_ANT_PATTERNS = Set.of(
            "/front/**",
            "/administration/**",
            "/upload/**",
            "/temp/**",
            "/api/**",
            "/.well-known/**"
    );

    private static final Set<String> EXCLUDE_EXACT_PATHS = Set.of(
            "/firebase-messaging-sw.js",
            "/.well-known/appspecific/com.chrome.devtools.json",
            "/favicon.ico",
            "/robots.txt",
            "/sitemap.xml"
    );

    private final VisitCounterService visitCounterService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 먼저 체인 진행(혹시 예외/리다이렉트 등과 무관하게 "요청 들어옴" 기준으로 기록하려면 아래로 옮겨도 됩니다)
        // 여기서는 "요청이 들어왔을 때" 기록이 목적이라, 체인 전 기록합니다.
        tryRecordVisit(request, response);

        filterChain.doFilter(request, response);
    }

    private void tryRecordVisit(HttpServletRequest request, HttpServletResponse response) {
        if (!isCountTargetRequest(request)) {
            return;
        }

        String visitorId = getOrCreateVisitorId(request, response);
        String ua = nullToEmpty(request.getHeader(HttpHeaders.USER_AGENT));
        String clientIp = extractClientIp(request); // 저장은 안 하지만 fingerprint 안정성 보강용

        // fingerprint = visitorId 기반이 핵심, 보강으로 UA/IP를 섞어서 "복사된 쿠키" 같은 케이스를 조금 더 줄임
        String raw = visitorId + "|" + ua + "|" + clientIp;
        String hash = sha256Hex(raw);

        visitCounterService.recordVisit(LocalDate.now(), hash);
    }

    private boolean isCountTargetRequest(HttpServletRequest request) {
        // 1) GET만
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String path = getRequestPath(request);

        // 2) 제외 경로
        if (EXCLUDE_EXACT_PATHS.contains(path)) {
            return false;
        }
        for (String pattern : EXCLUDE_ANT_PATTERNS) {
            if (PATH_MATCHER.match(pattern, path)) {
                return false;
            }
        }

        // 3) "페이지성 요청"만 카운트 (정적 파일 확장자 대부분 제외)
        // 예: /something.js, /image.png, /manifest.json 등은 제외
        if (hasStaticExtension(path)) {
            return false;
        }

        // 4) Accept 헤더가 text/html 우선이면 더 확실 (SPA 라우팅도 종종 text/html)
        // Accept가 아예 없을 수도 있으니, 이 조건은 '강제'가 아니라 '보조'로 사용합니다.
        // => 여기서는 제외하지 않고 그대로 통과(너무 빡세게 걸면 누락 생김)
        return true;
    }

    private boolean hasStaticExtension(String path) {
        int lastSlash = path.lastIndexOf('/');
        String last = (lastSlash >= 0) ? path.substring(lastSlash + 1) : path;
        int dot = last.lastIndexOf('.');
        if (dot < 0) return false;

        String ext = last.substring(dot + 1).toLowerCase(Locale.ROOT);

        // “불필요한 것들 전부 제외” 요청에 맞춰 넓게 제외합니다.
        return Set.of(
                "js", "css", "map",
                "png", "jpg", "jpeg", "gif", "webp", "svg", "ico",
                "woff", "woff2", "ttf", "eot", "otf",
                "json", "xml", "txt",
                "pdf", "zip"
        ).contains(ext);
    }

    private String getOrCreateVisitorId(HttpServletRequest request, HttpServletResponse response) {
        String existing = readCookie(request, VISITOR_COOKIE_NAME);
        if (existing != null && !existing.isBlank()) {
            return existing;
        }

        String newId = UUID.randomUUID().toString();

        // SameSite 설정을 위해 ResponseCookie 사용
        ResponseCookie cookie = ResponseCookie.from(VISITOR_COOKIE_NAME, newId)
                .path("/")
                .httpOnly(true)
                .secure(request.isSecure()) // https면 true, http면 false
                .sameSite("Lax")
                .maxAge(VISITOR_COOKIE_MAX_AGE_SECONDS)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return newId;
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    private String getRequestPath(HttpServletRequest request) {
        // contextPath 고려
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isBlank() && uri.startsWith(ctx)) {
            return uri.substring(ctx.length());
        }
        return uri;
    }

    private String extractClientIp(HttpServletRequest request) {
        // Nginx 설정을 “안 건드린다” 하셔도,
        // 혹시 헤더가 들어오는 환경이면 우선 사용하고, 아니면 remoteAddr 사용합니다.
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // XFF는 "client, proxy1, proxy2" 형태
            String first = xff.split(",")[0].trim();
            if (!first.isBlank()) return first;
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri.trim();

        return nullToEmpty(request.getRemoteAddr());
    }

    private String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // SHA-256은 JVM에 기본 탑재라 사실상 발생하지 않지만, 방어적으로 UUID로 대체
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    private String nullToEmpty(String s) {
        return (s == null) ? "" : s;
    }
}