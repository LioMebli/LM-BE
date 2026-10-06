package com.vansisto.lmbe.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Pattern ACCEPTABLE = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = acceptOrGenerate(request.getHeader(CorrelationId.HEADER));
        MDC.put(CorrelationId.MDC_KEY, correlationId);
        response.setHeader(CorrelationId.HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }

    private static String acceptOrGenerate(@Nullable String supplied) {
        return supplied != null && ACCEPTABLE.matcher(supplied).matches()
                ? supplied
                : UUID.randomUUID().toString();
    }
}
