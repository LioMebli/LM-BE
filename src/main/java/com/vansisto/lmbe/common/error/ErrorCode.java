package com.vansisto.lmbe.common.error;

/**
 * Stable identifier a client can branch on. The {@code detail} text of an error is
 * developer-facing and may be reworded at any time; this code is part of the published
 * contract and is never renamed or reused.
 *
 * <p>Deliberately carries no {@code HttpStatus}. Status is a transport concern and is
 * assigned in the exception handler; putting it here would drag
 * {@code org.springframework.http} into the domain through the front door.
 *
 * <p>Only codes this feature can actually produce are listed. The enum grows one ticket
 * at a time.
 *
 * <p>No two statuses share a code (FR-016). That is what makes branching on the code
 * equivalent to branching on the failure, and it is why the transport-level codes below
 * read like their statuses: at that level the status is the whole information.
 */
public enum ErrorCode {

    CATEGORY_NOT_FOUND,
    PRODUCT_NOT_FOUND,

    /**
     * A 404 for an address this API does not serve, as opposed to one it does serve about a
     * thing that is not there. The caller acts differently on each: this one is a bug in the
     * request, the other two are content.
     */
    ENDPOINT_NOT_FOUND,

    /**
     * Syntactic failure at the transport boundary - a non-numeric identifier, a malformed
     * body. Deliberately not {@code VALIDATION_ERROR}: {@code .claude/CLAUDE.md} § Error handling §2
     * reserves the {@code ValidationException} category for semantic business-rule
     * failures at 422, and one code name serving both a 400 and a 422 is a contract
     * nobody can branch on.
     */
    BAD_REQUEST,

    METHOD_NOT_ALLOWED,
    NOT_ACCEPTABLE,

    INTERNAL_ERROR
}
