package com.vansisto.lmbe.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import static com.vansisto.lmbe.common.config.FilterOrder.CORRELATION_FILTER_ORDER;
import static com.vansisto.lmbe.common.config.FilterOrder.CORS_FILTER_ORDER;
import static org.assertj.core.api.Assertions.assertThat;

class FilterOrderTest {

    @Test
    void corsRunsFirstAndCorrelationImmediatelyAfterIt() {
        assertThat(CORS_FILTER_ORDER).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
        assertThat(CORRELATION_FILTER_ORDER).isEqualTo(CORS_FILTER_ORDER + 1);
    }
}
