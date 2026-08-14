package com.vansisto.lmbe.catalog;

import com.vansisto.lmbe.common.error.ErrorCode;
import com.vansisto.lmbe.common.error.NotFoundException;

/**
 * Concrete and {@code final}: subclassing it would make the hierarchy four deep.
 *
 * <p>It exists as its own class rather than a shared not-found type carrying a code,
 * because {@code .claude/CLAUDE.md} § Error handling §3 wants a typed constructor and a message pattern
 * owned by the class that raises it — and neither has anywhere to live under the collapsed
 * form. Argued in specs/LM-10/research.md R8.
 */
public final class CategoryNotFoundException extends NotFoundException {

    private static final String MESSAGE = "Category %d not found";

    public CategoryNotFoundException(long categoryId) {
        super(ErrorCode.CATEGORY_NOT_FOUND, MESSAGE.formatted(categoryId));
    }
}
