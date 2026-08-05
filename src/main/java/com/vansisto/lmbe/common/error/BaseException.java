package com.vansisto.lmbe.common.error;

import java.util.Objects;

/**
 * Root of the application exception hierarchy: one type to catch, carrying the error code.
 *
 * <p>The hierarchy is exactly three levels - this root, a category, a concrete exception -
 * and never four. Subclassing a concrete exception produces a chain nobody can hold in
 * their head.
 *
 * <p>Nothing in this package imports {@code org.springframework.web}. The domain describes
 * what went wrong; the web layer decides what that means over HTTP. Full convention:
 * {@code docs/ERROR_HANDLING.md}.
 */
public abstract class BaseException extends RuntimeException {

    private final ErrorCode errorCode;

    protected BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
    }

    /** Keeps the cause. A wrapped exception that drops it destroys the only evidence. */
    protected BaseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
