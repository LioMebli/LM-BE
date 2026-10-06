package com.vansisto.lmbe;

import com.vansisto.lmbe.catalog.Category;
import com.vansisto.lmbe.product.Availability;
import com.vansisto.lmbe.product.Product;

public final class CatalogFixture {

    public static Category category(String name) {
        Category category = new Category();
        category.setName(name);
        return category;
    }

    public static Product product(Category category, String name) {
        Product product = new Product();
        product.setCategory(category);
        product.setName(name);
        product.setAvailability(Availability.IN_STOCK);
        product.setActive(true);
        return product;
    }

    private CatalogFixture() {
    }
}
