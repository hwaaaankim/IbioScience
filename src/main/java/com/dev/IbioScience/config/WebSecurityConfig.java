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

import com.dev.IbioScience.service.auth.PrincipalDetailService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

	private final PrincipalDetailService principalDetailService;

	/* ===== URL 패턴 (중앙 관리) ===== */

	// 정적 리소스(전역 허용) - WebSecurityCustomizer에서 ignore 처리
	private static final String[] STATIC_CUSTOM_IGNORES = { "/front/**", // resources/static/front/** (정적)
			"/administration/**" // resources/static/administration/** (정적)
	};

	// 게스트/공용 허용 경로(백엔드 라우트)
	private static final String[] AUTH_WHITELIST = { "/", "/index", "/home", "/login", "/signup/**", "/error",
			"/health", "/actuator/**", "/upload/**" // 업로드 공개 리소스 매핑 사용 시
	};

	// 고객 마이페이지(구매 가능한 회원만: 일반회원/일반구매딜러/일반기업/일반기업구매/판매딜러 포함)
	private static final String[] CUSTOMER_URLS = { "/customer/**", "/mypage/**" };

	// 판매 가능 회원(입점/판매딜러) 전용
	private static final String[] SELLER_URLS = { "/seller/**" };

	// 사내 관리자용 영역 (역할별)
	private static final String[] ADMIN_ROOT_URLS = { "/admin/root/**" }; // ROOT만
	private static final String[] ADMIN_MASTER_URLS = { "/admin/master/**" }; // MASTER 이상
	private static final String[] ADMIN_MANAGER_URLS = { "/admin/manager/**" }; // ADMIN 이상(ADMIN/OPERATOR/MASTER/ROOT)
	private static final String[] ADMIN_OPERATOR_URLS = { "/admin/operator/**" }; // OPERATOR 이상(OPERATOR/MASTER/ROOT)

	// 내부/판매 API
	private static final String[] INTERNAL_API_URLS = { "/api/v1/**" };

	/* ===== 공용 빈 ===== */

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
		// 정적 리소스는 보안 필터 자체를 태우지 않음
		return web -> web.ignoring().requestMatchers(PathRequest.toStaticResources().atCommonLocations())
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
		return p;
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http.csrf(csrf -> csrf.disable()).headers(h -> h.frameOptions(f -> f.disable()))
				.authenticationProvider(authenticationProvider()).authorizeHttpRequests(auth -> auth

						/* 1) 먼저 ‘보호 경로들’을 구체적으로 막고(역할 부여) */
						// 사내 관리자
						.requestMatchers(ADMIN_ROOT_URLS).hasRole("ROOT").requestMatchers(ADMIN_MASTER_URLS)
						.hasAnyRole("MASTER", "ROOT").requestMatchers(ADMIN_MANAGER_URLS)
						.hasAnyRole("ADMIN", "OPERATOR", "MASTER", "ROOT").requestMatchers(ADMIN_OPERATOR_URLS)
						.hasAnyRole("OPERATOR", "MASTER", "ROOT")

						// 판매 전용
						.requestMatchers(SELLER_URLS).hasAnyRole("SELLER_DEALER", "OPERATOR", "MASTER", "ROOT", "ADMIN")

						// 고객 마이페이지(구매 가능 회원)
						.requestMatchers(CUSTOMER_URLS)
						.hasAnyRole("USER", "BUYER_DEALER", "SELLER_DEALER", "ADMIN", "OPERATOR", "MASTER", "ROOT")

						// 내부/판매 API: 판매 가능 회원 + 사내 직원만
						.requestMatchers(INTERNAL_API_URLS)
						.hasAnyRole("SELLER_DEALER", "ADMIN", "OPERATOR", "MASTER", "ROOT")

						/* 2) 그 외는 전부 ‘게스트 허용’ */
						.requestMatchers(AUTH_WHITELIST).permitAll().anyRequest().permitAll())
				.formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login").defaultSuccessUrl("/", true)
						.permitAll())
				.logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/").permitAll())
				.httpBasic(Customizer.withDefaults());

		return http.build();
	}
}
