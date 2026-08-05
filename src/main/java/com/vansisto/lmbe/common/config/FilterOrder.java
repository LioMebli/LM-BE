package com.vansisto.lmbe.common.config;

import org.springframework.core.Ordered;

/** The servlet filter chain, declared in the order it runs. */
final class FilterOrder {

    static final int CORS_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE;
    static final int CORRELATION_FILTER_ORDER = CORS_FILTER_ORDER + 1;

    private FilterOrder() {
    }
}
