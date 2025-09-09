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

	private static final String[] STATIC_CUSTOM_IGNORES = {
			"/front/**", "/administration/**"
	};

	private static final String[] AUTH_WHITELIST = {
			"/", "/index", "/home", "/login", "/signIn", "/signup", "/companySignUp","/personalSignUp", "/error",
			"/signUpSuccess/**", "/health", "/actuator/**", "/upload/**", "/temp/api/**", "/api/customer/**", "/signUpProcess/personal"
			,"/signUpProcess/company"
	};

	private static final String[] CUSTOMER_URLS = { "/customer/**", "/mypage/**" };
	private static final String[] SELLER_URLS = { "/seller/**" };
	private static final String[] ADMIN_ROOT_URLS = { "/admin/root/**" };
	private static final String[] ADMIN_MASTER_URLS = { "/admin/master/**" };
	private static final String[] ADMIN_MANAGER_URLS = { "/admin/manager/**" };
	private static final String[] ADMIN_OPERATOR_URLS = { "/admin/operator/**" };
	private static final String[] INTERNAL_API_URLS = { "/api/v1/**" };

	@Bean HttpSessionEventPublisher httpSessionEventPublisher() { return new HttpSessionEventPublisher(); }
	@Bean SpringSecurityDialect springSecurityDialect() { return new SpringSecurityDialect(); }

	@Bean
	WebSecurityCustomizer webSecurityCustomizer() {
		return web -> web.ignoring()
				.requestMatchers(PathRequest.toStaticResources().atCommonLocations())
				.requestMatchers(STATIC_CUSTOM_IGNORES);
	}

	@Bean BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

	// WebSecurityConfig.java
	@Bean
	DaoAuthenticationProvider authenticationProvider() {
	    DaoAuthenticationProvider p = new DaoAuthenticationProvider();
	    p.setUserDetailsService(principalDetailService);
	    p.setPasswordEncoder(passwordEncoder());
	    // ★ 기본값(true) → false로 바꿔서 "아이디 없음"을 마스킹하지 않도록 함
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
				.requestMatchers(ADMIN_ROOT_URLS).hasRole("ROOT")
				.requestMatchers(ADMIN_MASTER_URLS).hasAnyRole("MASTER", "ROOT")
				.requestMatchers(ADMIN_MANAGER_URLS).hasAnyRole("ADMIN", "OPERATOR", "MASTER", "ROOT")
				.requestMatchers(ADMIN_OPERATOR_URLS).hasAnyRole("OPERATOR", "MASTER", "ROOT")
				.requestMatchers(SELLER_URLS).hasAnyRole("SELLER_DEALER", "OPERATOR", "MASTER", "ROOT", "ADMIN")
				.requestMatchers(CUSTOMER_URLS).hasAnyRole("USER", "BUYER_DEALER", "SELLER_DEALER", "ADMIN", "OPERATOR", "MASTER", "ROOT")
				.requestMatchers(INTERNAL_API_URLS).hasAnyRole("SELLER_DEALER", "ADMIN", "OPERATOR", "MASTER", "ROOT")
				.requestMatchers(AUTH_WHITELIST).permitAll()
				.anyRequest().permitAll()
			)
			.formLogin(form -> form
				.loginPage("/signIn")
				.loginProcessingUrl("/signInProcess")
				// defaultSuccessUrl 대신 커스텀 성공/실패 핸들러 사용
				.successHandler(customAuthSuccessHandler)
				.failureHandler(customAuthFailureHandler)
				.permitAll()
			)
			.logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/").permitAll())
			.httpBasic(Customizer.withDefaults());

		return http.build();
	}
}
