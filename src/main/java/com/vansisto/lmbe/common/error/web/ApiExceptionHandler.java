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

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String CODE_PROPERTY = "code";
    private static final String TRACE_ID_PROPERTY = "traceId";

    private static final String UNEXPECTED_DETAIL = "An unexpected error occurred.";

    private static final String CLIENT_ERROR_DETAIL = "The request could not be processed.";

    private static final Map<Integer, ErrorCode> CODE_BY_CLIENT_ERROR_STATUS = Map.of(
            HttpStatus.NOT_FOUND.value(), ErrorCode.ENDPOINT_NOT_FOUND,
            HttpStatus.METHOD_NOT_ALLOWED.value(), ErrorCode.METHOD_NOT_ALLOWED,
            HttpStatus.NOT_ACCEPTABLE.value(), ErrorCode.NOT_ACCEPTABLE);

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException exception) {
        log.warn("{}: {}", exception.getErrorCode(), exception.getMessage());
        return decorate(
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage()),
                exception.getErrorCode());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unhandled exception", exception);
        return decorate(
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_DETAIL),
                ErrorCode.INTERNAL_ERROR);
    }

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

        String correlationId = MDC.get(CorrelationId.MDC_KEY);
        if (correlationId != null) {
            problem.setProperty(TRACE_ID_PROPERTY, correlationId);
        }
        return problem;
    }
}
