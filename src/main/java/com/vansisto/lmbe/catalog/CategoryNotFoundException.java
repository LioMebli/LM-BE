package com.vansisto.lmbe.catalog;

import com.vansisto.lmbe.common.error.ErrorCode;
import com.vansisto.lmbe.common.error.NotFoundException;

public final class CategoryNotFoundException extends NotFoundException {

    private static final String MESSAGE = "Category %d not found";

    public CategoryNotFoundException(long categoryId) {
        super(ErrorCode.CATEGORY_NOT_FOUND, MESSAGE.formatted(categoryId));
    }
}
