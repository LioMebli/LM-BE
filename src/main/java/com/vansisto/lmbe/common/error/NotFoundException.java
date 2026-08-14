package com.vansisto.lmbe.common.error;

/**
 * Category: something referenced does not exist. The handler maps every subtype to 404.
 *
 * <p>This level exists for exactly one reason - {@code @ExceptionHandler(NotFoundException.class)}
 * catches all of them. That is polymorphic dispatch doing real work, which is why the
 * abstraction earns its place and also why the categories must stay few.
 *
 * <p>{@code ConflictException}, {@code ValidationException} and {@code ExternalServiceException}
 * are the other three categories in {@code .claude/CLAUDE.md} § Error handling §2. They are not created
 * here: nothing in this feature can throw them, and an abstract class with no subclass is
 * speculative structure. Each arrives with the ticket that first raises it.
 */
public abstract class NotFoundException extends BaseException {

    protected NotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
