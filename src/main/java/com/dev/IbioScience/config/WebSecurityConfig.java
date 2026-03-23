package com.dev.IbioScience.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;

import com.dev.IbioScience.handler.exception.CustomAuthFailureHandler;
import com.dev.IbioScience.handler.exception.CustomAuthSuccessHandler;
import com.dev.IbioScience.handler.exception.MixedAccessDeniedHandler;
import com.dev.IbioScience.handler.exception.MixedAuthenticationEntryPoint;
import com.dev.IbioScience.service.auth.PrincipalDetailService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final PrincipalDetailService principalDetailService;
    private final CustomAuthFailureHandler customAuthFailureHandler;
    private final CustomAuthSuccessHandler customAuthSuccessHandler;
    private final MixedAccessDeniedHandler mixedAccessDeniedHandler;
    private final MixedAuthenticationEntryPoint mixedAuthenticationEntryPoint;

    private static final String[] STATIC_CUSTOM_IGNORES = {
            "/front/**", "/administration/**"
    };

    private static final String[] AUTH_WHITELIST = {
            "/", "/index", "/signIn", "/signup",
            "/companySignUp", "/personalSignUp", "/error", "/signUpSuccess/**", "/upload/**",
            "/temp/api/**", "/api/customer/**", "/signUpProcess/personal", "/signUpProcess/company",
            "/api/v1/productSelect/**", "/api/v1/**", "/api/menu/**", "/api/category/**"
    };

    // 고객 구간
    private static final String[] CUSTOMER_URLS = { "/customer/**", "/mypage/**" };

    // ✅ 판매딜러 포털 전용 구간
    private static final String[] SELLER_ENTRY_URLS = { "/seller/page", "/seller/page/", "/seller/page/main" };
    private static final String[] SELLER_PAGE_URLS  = { "/seller/page/**" };
    private static final String[] SELLER_API_URLS   = { "/seller/api/**" };

    // 관리자 공통/등급별 구간
    private static final String[] ADMIN_COMMON_URLS   = { "/admin/common/**" };
    private static final String[] ADMIN_OPERATOR_URLS = { "/admin/operator/**" };
    private static final String[] ADMIN_MANAGER_URLS  = { "/admin/manager/**", "/api/manager/**" };
    private static final String[] ADMIN_MASTER_URLS   = { "/admin/master/**" };
    private static final String[] ADMIN_ROOT_URLS     = { "/admin/root/**" };

    // ✅ 관리자 엔트리
    private static final String[] ADMIN_ENTRY_URLS = { "/admin", "/admin/", "/admin/main" };

    // ✅ 누락 방지용: 나머지 모든 /admin/** 보호
    private static final String[] ADMIN_ALL_URLS = { "/admin/**" };

    private static final String[] INTERNAL_API_URLS = { };

    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    SpringSecurityDialect springSecurityDialect() {
        return new SpringSecurityDialect();
    }

    @Bean
    WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                .requestMatchers(STATIC_CUSTOM_IGNORES);
    }

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(principalDetailService);
        p.setPasswordEncoder(passwordEncoder());
        p.setHideUserNotFoundExceptions(false);
        return p;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(h -> h.frameOptions(f -> f.disable()))
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth

                // ✅ 판매딜러 포털
                .requestMatchers(SELLER_ENTRY_URLS).hasRole("SELLER_PORTAL")
                .requestMatchers(SELLER_PAGE_URLS).hasRole("SELLER_PORTAL")
                .requestMatchers(SELLER_API_URLS).hasRole("SELLER_PORTAL")

                // ✅ 관리자 엔트리/등급별
                .requestMatchers(ADMIN_ENTRY_URLS)
                    .hasAnyRole("ADMIN", "OPERATOR", "MASTER", "ROOT")

                .requestMatchers(ADMIN_COMMON_URLS)
                    .hasAnyRole("ADMIN", "OPERATOR", "MASTER", "ROOT")
                .requestMatchers(ADMIN_OPERATOR_URLS)
                    .hasAnyRole("OPERATOR", "MASTER", "ROOT")
                .requestMatchers(ADMIN_MANAGER_URLS)
                    .hasAnyRole("ADMIN", "OPERATOR", "MASTER", "ROOT")
                .requestMatchers(ADMIN_MASTER_URLS)
                    .hasAnyRole("MASTER", "ROOT")
                .requestMatchers(ADMIN_ROOT_URLS)
                    .hasRole("ROOT")

                // ✅ 빠져나가는 /admin/** 전체 막기
                .requestMatchers(ADMIN_ALL_URLS)
                    .hasAnyRole("ADMIN", "OPERATOR", "MASTER", "ROOT")

                // 고객/내부
                .requestMatchers(CUSTOMER_URLS)
                    .hasAnyRole("USER", "BUYER_DEALER", "SELLER_DEALER", "ADMIN", "OPERATOR", "MASTER", "ROOT")
                .requestMatchers(INTERNAL_API_URLS)
                    .hasAnyRole("SELLER_DEALER", "ADMIN", "OPERATOR", "MASTER", "ROOT")

                // 화이트리스트
                .requestMatchers(AUTH_WHITELIST).permitAll()

                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/signIn")
                .loginProcessingUrl("/signInProcess")
                .successHandler(customAuthSuccessHandler)
                .failureHandler(customAuthFailureHandler)
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(mixedAccessDeniedHandler)
                .authenticationEntryPoint(mixedAuthenticationEntryPoint)
            )
            .logout(l -> l
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}