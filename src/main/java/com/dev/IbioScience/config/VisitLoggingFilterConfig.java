package com.dev.IbioScience.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import com.dev.IbioScience.filter.logging.VisitLoggingFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class VisitLoggingFilterConfig {

    private final VisitLoggingFilter visitLoggingFilter;

    @Bean
    public FilterRegistrationBean<VisitLoggingFilter> visitLoggingFilterRegistration() {
        FilterRegistrationBean<VisitLoggingFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(visitLoggingFilter);
        reg.addUrlPatterns("/*");
        reg.setName("visitLoggingFilter");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10); // 되도록 앞단에서
        return reg;
    }
}