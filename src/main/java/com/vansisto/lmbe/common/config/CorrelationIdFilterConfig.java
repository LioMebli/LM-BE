package com.vansisto.lmbe.common.config;

import com.vansisto.lmbe.common.logging.CorrelationIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.vansisto.lmbe.common.config.FilterOrder.CORRELATION_FILTER_ORDER;

@Configuration
public class CorrelationIdFilterConfig {

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration() {
        FilterRegistrationBean<CorrelationIdFilter> registration =
                new FilterRegistrationBean<>(new CorrelationIdFilter());
        registration.setOrder(CORRELATION_FILTER_ORDER);
        return registration;
    }
}
