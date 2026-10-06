package com.vansisto.lmbe.product;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository products;

    public List<Product> findAll() {
        return products.findByActiveTrueOrderByIdAsc();
    }

    public List<Product> findByCategory(long categoryId) {
        return products.findByCategoryIdAndActiveTrueOrderByNameAscIdAsc(categoryId);
    }

    public Product findById(long id) {
        return products.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
