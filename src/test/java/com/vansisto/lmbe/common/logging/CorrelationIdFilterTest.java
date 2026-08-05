package com.vansisto.lmbe.common.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private static final String SUPPLIED = "req-01JQZ8.WORKS_fine-42";
    private static final String OTHER_REQUEST = "second-request";

    private static final int GENERATED_LENGTH = "6b1f0b8e-6b1f-4b8e-8b1f-6b1f0b8e6b1f".length();

    private final CorrelationIdFilter filter = new CorrelationIdFilter();
    private final Logger logger = (Logger) LoggerFactory.getLogger(CorrelationIdFilterTest.class);
    private final ListAppender<ILoggingEvent> records = new ListAppender<>();

    @BeforeEach
    void captureLogRecords() {
        records.start();
        logger.addAppender(records);
    }

    @AfterEach
    void releaseLogRecords() {
        logger.detachAppender(records);
        records.stop();
        MDC.clear();
    }

    @Test
    void suppliedValueReachesTheMdcAndComesBackInTheResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        String observed = runWithHeader(SUPPLIED, response);

        assertThat(observed).isEqualTo(SUPPLIED);
        assertThat(response.getHeader(CorrelationId.HEADER)).isEqualTo(SUPPLIED);
    }

    @Test
    void absentValueIsGeneratedAndUsedTheSameWay() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        String observed = runWithHeader(null, response);

        assertThat(observed).hasSize(GENERATED_LENGTH);
        assertThat(response.getHeader(CorrelationId.HEADER)).isEqualTo(observed);
    }

    @Test
    void valueCarryingControlCharactersIsDiscardedAndReplaced() throws Exception {
        String forged = "abc\r\nX-Injected: yes";
        MockHttpServletResponse response = new MockHttpServletResponse();

        String observed = runWithHeader(forged, response);

        assertThat(observed).doesNotContain("\r", "\n").hasSize(GENERATED_LENGTH);
        assertThat(response.getHeader(CorrelationId.HEADER)).isEqualTo(observed);
    }

    @Test
    void overlongValueIsDiscardedAndReplaced() throws Exception {
        String tooLong = "a".repeat(65);
        MockHttpServletResponse response = new MockHttpServletResponse();

        String observed = runWithHeader(tooLong, response);

        assertThat(observed).isNotEqualTo(tooLong).hasSize(GENERATED_LENGTH);
    }

    @Test
    void mdcIsClearedOnceTheRequestIsOver() throws Exception {
        runWithHeader(SUPPLIED, new MockHttpServletResponse());

        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void everyLogRecordOfTheRequestCarriesItsOwnValueAndNoOther() throws Exception {
        run(request(SUPPLIED), new MockHttpServletResponse(), chainThatLogs("inside first request"));
        run(request(OTHER_REQUEST), new MockHttpServletResponse(), chainThatLogs("inside second request"));

        List<ILoggingEvent> emitted = records.list;
        assertThat(emitted).hasSize(2);
        assertThat(emitted.getFirst().getMDCPropertyMap())
                .containsEntry(CorrelationId.MDC_KEY, SUPPLIED);
        assertThat(emitted.getLast().getMDCPropertyMap())
                .containsEntry(CorrelationId.MDC_KEY, OTHER_REQUEST);
    }

    private String runWithHeader(String supplied, MockHttpServletResponse response) throws Exception {
        AtomicReference<String> observed = new AtomicReference<>();
        run(request(supplied), response, (req, res) -> observed.set(MDC.get(CorrelationId.MDC_KEY)));
        return observed.get();
    }

    private void run(MockHttpServletRequest request, MockHttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        filter.doFilter(request, response, chain);
    }

    private static MockHttpServletRequest request(String supplied) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        if (supplied != null) {
            request.addHeader(CorrelationId.HEADER, supplied);
        }
        return request;
    }

    private FilterChain chainThatLogs(String message) {
        return (req, res) -> logger.info(message);
    }
}
