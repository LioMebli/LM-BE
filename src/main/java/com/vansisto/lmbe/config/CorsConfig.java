package com.vansisto.lmbe.config;

import com.vansisto.lmbe.properties.CorsProperties;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import static org.springframework.http.HttpMethod.*;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    private static final String BASE_PATH = "/api/v1";
    private static final String ADMIN_PATH_PATTERN = BASE_PATH.concat("/admin/**");
    private static final String PUBLIC_PATH_PATTERN = BASE_PATH.concat("/**");

    private static final Duration PREFLIGHT_MAX_AGE = Duration.ofMinutes(30);

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(ADMIN_PATH_PATTERN, adminConfiguration(properties));
        source.registerCorsConfiguration(PUBLIC_PATH_PATTERN, publicConfiguration());
        return source;
    }

    /**
     * Registered manually because Spring Security is not on the classpath yet. When it
     * arrives, delete this bean and let the security filter chain consume the
     * {@code corsConfigurationSource} bean above — running both would emit duplicate
     * {@code Vary} headers and make it ambiguous which policy applied.
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration(
            @Qualifier("corsConfigurationSource") CorsConfigurationSource source) {
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    private CorsConfiguration adminConfiguration(CorsProperties properties) {
        List<String> origins = getOrigins(properties);

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of(GET.name(), POST.name(), PUT.name(), PATCH.name(), DELETE.name()));
        configuration.setAllowedHeaders(List.of("Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(PREFLIGHT_MAX_AGE);
        return configuration;
    }

    private static @NonNull List<String> getOrigins(CorsProperties properties) {
        List<String> origins = properties.adminOrigins();
        if (Objects.isNull(origins) || origins.isEmpty()) {
            throw new IllegalStateException("lm.cors.admin-origins must list at least one origin");
        }
        if (origins.contains(CorsConfiguration.ALL)) {
            throw new IllegalStateException(
                    "lm.cors.admin-origins must not contain a wildcard: the admin surface sends credentials, "
                            + "and a wildcard would let any site read admin responses with the operator's cookie");
        }
        return origins;
    }

    private CorsConfiguration publicConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin(CorsConfiguration.ALL);
        configuration.setAllowedMethods(List.of(GET.name(), HEAD.name()));
        configuration.addAllowedHeader(CorsConfiguration.ALL);
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(PREFLIGHT_MAX_AGE);
        return configuration;
    }
}
