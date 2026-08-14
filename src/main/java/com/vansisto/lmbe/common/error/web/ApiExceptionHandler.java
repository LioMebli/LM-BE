package com.vansisto.lmbe.common.error.web;

import java.util.Map;

import com.vansisto.lmbe.common.error.ErrorCode;
import com.vansisto.lmbe.common.error.NotFoundException;
import com.vansisto.lmbe.common.logging.CorrelationId;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * The single place a domain error becomes a transport response — an Adapter at the web
 * boundary, in the same position the controller occupies for the happy path. It is what
 * lets the service layer stay free of HTTP.
 *
 * <p>Extending {@link ResponseEntityExceptionHandler} is the point, not a detail. A handler
 * that only lists domain types leaves malformed JSON, an unknown path, a wrong method and a
 * non-numeric identifier falling through to Spring's default error page — a different shape
 * from every deliberate error in the API, which no client can parse generically. An error
 * format that covers half the cases is the same as having none.
 *
 * <p>Full convention: {@code .claude/CLAUDE.md} § Error handling.
 */
@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String CODE_PROPERTY = "code";
    private static final String TRACE_ID_PROPERTY = "traceId";

    /**
     * Fixed sentence. The catch-all never includes {@code ex.getMessage()}: a SQLException
     * message carries schema names and a client message can carry a URL with credentials.
     */
    private static final String UNEXPECTED_DETAIL = "An unexpected error occurred.";

    /** Deliberately vague. A 400 that explains the parser teaches an attacker the parser. */
    private static final String CLIENT_ERROR_DETAIL = "The request could not be processed.";

    /**
     * FR-016: a client branches on {@code code}, so two failures that differ must not answer
     * with the same one. Anything not listed is a 400 by elimination — every other client
     * error this read-only surface can raise is a malformed request.
     */
    private static final Map<Integer, ErrorCode> CODE_BY_CLIENT_ERROR_STATUS = Map.of(
            HttpStatus.NOT_FOUND.value(), ErrorCode.ENDPOINT_NOT_FOUND,
            HttpStatus.METHOD_NOT_ALLOWED.value(), ErrorCode.METHOD_NOT_ALLOWED,
            HttpStatus.NOT_ACCEPTABLE.value(), ErrorCode.NOT_ACCEPTABLE);

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException exception) {
        // WARN without a stack trace: an expected outcome, and the stack is noise.
        log.warn("{}: {}", exception.getErrorCode(), exception.getMessage());
        return decorate(
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage()),
                exception.getErrorCode());
    }

    /**
     * Not optional. The one thing guaranteed to happen in production is an exception nobody
     * predicted, and without this it is the single case with no defined behaviour.
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unhandled exception", exception);
        return decorate(
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_DETAIL),
                ErrorCode.INTERNAL_ERROR);
    }

    /**
     * The hook every framework-raised failure passes through. Stamping {@code code} and
     * {@code traceId} here rather than in each handler is what makes the shape uniform:
     * one place owns the common properties, so a case nobody enumerated still comes out
     * looking like every other error.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {

        ResponseEntity<Object> response =
                super.handleExceptionInternal(exception, body, headers, statusCode, request);

        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            if (statusCode.is4xxClientError()) {
                problem.setDetail(CLIENT_ERROR_DETAIL);
                decorate(problem, CODE_BY_CLIENT_ERROR_STATUS
                        .getOrDefault(statusCode.value(), ErrorCode.BAD_REQUEST));
            } else {
                problem.setDetail(UNEXPECTED_DETAIL);
                decorate(problem, ErrorCode.INTERNAL_ERROR);
            }
        }
        return response;
    }

    private ProblemDetail decorate(ProblemDetail problem, ErrorCode code) {
        problem.setProperty(CODE_PROPERTY, code.name());

        // Written by CorrelationIdFilter. Absent until that filter exists, and absent is
        // the right answer then - an invented value would not match any log line.
        String correlationId = MDC.get(CorrelationId.MDC_KEY);
        if (correlationId != null) {
            problem.setProperty(TRACE_ID_PROPERTY, correlationId);
        }
        return problem;
    }
}
