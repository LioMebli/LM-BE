package com.vansisto.lmbe.common.config;

import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Declares the public catalog cacheable, in one place so a later endpoint cannot forget to. */
@Configuration
public class PublicCacheConfig implements WebMvcConfigurer {

    private static final String PUBLIC_PATH_PATTERN = "/api/v1/**";

    /** How out of date a catalog answer is allowed to be — the whole of what max-age means. */
    private static final Duration ACCEPTABLE_CATALOG_STALENESS = Duration.ofMinutes(1);

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PublicCacheInterceptor()).addPathPatterns(PUBLIC_PATH_PATTERN);
    }

    /** {@code postHandle} is skipped when a handler throws — which is what leaves errors unstamped. */
    private static final class PublicCacheInterceptor implements HandlerInterceptor {

        private static final String CACHEABLE = CacheControl
                .maxAge(ACCEPTABLE_CATALOG_STALENESS)
                .cachePublic()
                .getHeaderValue();

        @Override
        public void postHandle(
                HttpServletRequest request,
                HttpServletResponse response,
                Object handler,
                @Nullable ModelAndView modelAndView) {

            if (!HttpMethod.GET.matches(request.getMethod())) {
                return;
            }
            if (!HttpStatusCode.valueOf(response.getStatus()).is2xxSuccessful()) {
                return;
            }
            response.setHeader(HttpHeaders.CACHE_CONTROL, CACHEABLE);
        }
    }
}
