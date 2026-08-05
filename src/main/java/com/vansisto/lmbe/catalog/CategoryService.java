package com.vansisto.lmbe.catalog;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Knows nothing about HTTP. No status codes, no headers, no {@code org.springframework.web}
 * import — that boundary is what lets the same service answer a REST controller today and
 * something else later without a rewrite.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categories;

    public List<Category> findAll() {
        return categories.findAllByOrderByNameAscIdAsc();
    }

    public Category findById(long id) {
        return categories.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    /** Cheaper than loading the category when only its existence is in question. */
    public void requireExists(long id) {
        if (!categories.existsById(id)) {
            throw new CategoryNotFoundException(id);
        }
    }
}
