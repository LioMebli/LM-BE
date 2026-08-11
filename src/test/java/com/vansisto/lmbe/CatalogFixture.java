package com.vansisto.lmbe;

import com.vansisto.lmbe.catalog.Category;
import com.vansisto.lmbe.product.Availability;
import com.vansisto.lmbe.product.Product;

/**
 * Builds the two catalogue entities the suites need, so the same setter sequence is not
 * written out once per test class. Kept off {@link IntegrationTest} deliberately — that base
 * carries wiring and nothing else.
 */
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
