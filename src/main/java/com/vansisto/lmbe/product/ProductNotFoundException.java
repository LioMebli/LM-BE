package com.vansisto.lmbe.product;

import com.vansisto.lmbe.common.error.ErrorCode;
import com.vansisto.lmbe.common.error.NotFoundException;

/**
 * Raised both for an identifier that never existed and for one belonging to a product that
 * has been switched off. The two are deliberately indistinguishable (FR-014): telling them
 * apart would let a caller learn that a given identifier was once in use.
 */
public final class ProductNotFoundException extends NotFoundException {

    private static final String MESSAGE = "Product %d not found";

    public ProductNotFoundException(long productId) {
        super(ErrorCode.PRODUCT_NOT_FOUND, MESSAGE.formatted(productId));
    }
}
