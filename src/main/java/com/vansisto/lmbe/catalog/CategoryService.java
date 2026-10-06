package com.vansisto.lmbe.catalog;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public void requireExists(long id) {
        if (!categories.existsById(id)) {
            throw new CategoryNotFoundException(id);
        }
    }
}
