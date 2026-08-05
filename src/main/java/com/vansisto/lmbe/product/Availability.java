package com.vansisto.lmbe.product;

/**
 * Stock state at the time of writing — not derivable later, which is why it is stored.
 *
 * <p>Persisted as its name, never its ordinal: an ordinal column silently re-points every
 * existing row the first time a constant is inserted in the middle of this list, and
 * nothing fails while it happens.
 *
 * <p>Crosses the API boundary as this identifier. The Ukrainian label is the frontend's
 * choice — sending "В наявності" over the wire would make a copy change a backend release.
 */
public enum Availability {

    IN_STOCK,
    MADE_TO_ORDER,
    DISCONTINUED
}
