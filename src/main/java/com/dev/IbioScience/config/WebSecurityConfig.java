package com.dev.IbioScience.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
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
    private final AdminDynamicAuthorizationManager adminDynamicAuthorizationManager;

    private static final String[] STATIC_CUSTOM_IGNORES = {
            "/front/**",
            "/administration/assets/**"
    };

    private static final String[] AUTH_WHITELIST = {
            "/", "/index", "/signIn", "/signup",
            "/companySignUp", "/personalSignUp", "/error", "/signUpSuccess/**", "/upload/**",
            "/temp/api/**", "/api/customer/**", "/signUpProcess/personal", "/signUpProcess/company",
            "/api/v1/productSelect/**", "/api/v1/**", "/api/menu/**", "/api/category/**", "/api/front/**"
    };

    private static final String[] CUSTOMER_URLS = { "/customer/**", "/mypage/**" };

    private static final String[] SELLER_ENTRY_URLS = { "/seller/page", "/seller/page/", "/seller/page/main" };
    private static final String[] SELLER_PAGE_URLS  = { "/seller/page/**" };
    private static final String[] SELLER_API_URLS   = { "/seller/api/**" };

    private static final String[] ADMIN_ENTRY_URLS = { "/admin", "/admin/", "/admin/main" };

    private static final String[] ROLE_MANAGER_ROOT_ONLY_URLS = {
            "/admin/root/roleManager",
            "/admin/root/api/role-manager/**"
    };

    private static final String[] ADMIN_STANDALONE_MANAGED_PAGE_URLS = {
            "/brandManager",
            "/categoryManager",
            "/displayManager",
            "/couponManager",
            "/productPromotionManager",
            "/internalCategoryManager"
    };

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

                .requestMatchers(new RegexRequestMatcher("^/seller/page/product/\\d+$", HttpMethod.GET.name()))
                    .hasAnyRole("SELLER_PORTAL", "EMPLOY", "MASTER", "ROOT")

                .requestMatchers(new RegexRequestMatcher("^/seller/api/products/\\d+$", HttpMethod.GET.name()))
                    .hasAnyRole("SELLER_PORTAL", "EMPLOY", "MASTER", "ROOT")

                .requestMatchers(SELLER_ENTRY_URLS).hasRole("SELLER_PORTAL")
                .requestMatchers(SELLER_PAGE_URLS).hasRole("SELLER_PORTAL")
                .requestMatchers(SELLER_API_URLS).hasRole("SELLER_PORTAL")

                .requestMatchers(ROLE_MANAGER_ROOT_ONLY_URLS).hasRole("ROOT")

                .requestMatchers(ADMIN_ENTRY_URLS).access(adminDynamicAuthorizationManager)
                .requestMatchers(ADMIN_STANDALONE_MANAGED_PAGE_URLS).access(adminDynamicAuthorizationManager)

                .requestMatchers("/administration/api/**").access(adminDynamicAuthorizationManager)

                .requestMatchers("/admin/**", "/api/manager/**", "/api/admin/**")
                    .access(adminDynamicAuthorizationManager)

                .requestMatchers(CUSTOMER_URLS)
                    .hasAnyRole("USER", "BUYER_DEALER", "SELLER_DEALER", "EMPLOY", "OPERATOR", "MASTER", "ROOT")

                .requestMatchers(INTERNAL_API_URLS)
                    .hasAnyRole("SELLER_DEALER", "EMPLOY", "OPERATOR", "MASTER", "ROOT")

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