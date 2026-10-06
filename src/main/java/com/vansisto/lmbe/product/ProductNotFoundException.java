package com.vansisto.lmbe.product;

import com.vansisto.lmbe.common.error.ErrorCode;
import com.vansisto.lmbe.common.error.NotFoundException;

public final class ProductNotFoundException extends NotFoundException {

    private static final String MESSAGE = "Product %d not found";

    public ProductNotFoundException(long productId) {
        super(ErrorCode.PRODUCT_NOT_FOUND, MESSAGE.formatted(productId));
    }
}
