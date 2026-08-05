package com.vansisto.lmbe.product;

import com.vansisto.lmbe.IntegrationTest;
import com.vansisto.lmbe.catalog.Category;
import com.vansisto.lmbe.catalog.CategoryRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.persistence.EntityManagerFactory;

import static com.vansisto.lmbe.CatalogFixture.category;
import static com.vansisto.lmbe.CatalogFixture.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductApiIT extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CategoryRepository categories;
    @Autowired
    private ProductRepository products;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Category handles;
    private Product visible;
    private Product switchedOff;

    @BeforeEach
    void seedCatalog() {
        products.deleteAll();
        categories.deleteAll();

        handles = categories.save(category("Ручки меблеві"));

        visible = product(handles, "Ручка-скоба РС-115");
        visible.setShortDescription("Алюмінієва ручка-скоба");
        visible.setDescription("Повний опис товару");
        visible = products.save(visible);

        switchedOff = product(handles, "Знята з продажу");
        switchedOff.setActive(false);
        switchedOff = products.save(switchedOff);
    }

    @Test
    void returnsFullDetailOfOneProduct() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", visible.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(visible.getId()))
                .andExpect(jsonPath("$.categoryId").value(handles.getId()))
                .andExpect(jsonPath("$.name").value("Ручка-скоба РС-115"))
                .andExpect(jsonPath("$.shortDescription").value("Алюмінієва ручка-скоба"))
                .andExpect(jsonPath("$.description").value("Повний опис товару"))
                .andExpect(jsonPath("$.availability").value("IN_STOCK"));
    }

    /** FR-015: the build needs every product address in one exchange, each with its category. */
    @Test
    void returnsWholeCatalogInOneAnswerWithCategoryOnEachItem() throws Exception {
        Category hinges = categories.save(category("Петлі"));
        products.save(product(hinges, "Петля накладна"));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].categoryId").exists());
    }

    /** FR-014: indistinguishable from a product that never existed. */
    @Test
    void inactiveProductIsAbsentFromEveryListing() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(visible.getId()));

        mockMvc.perform(get("/api/v1/categories/{id}/products", handles.getId()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(visible.getId()));
    }

    @Test
    void inactiveProductIsNotRetrievableByIdentifier() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", switchedOff.getId()))
                .andExpect(status().isNotFound());
    }

    /**
     * SC-004 as reworded 2026-08-05: a constant query count, not a wall-clock number.
     * A threshold measured on CI hardware measures the runner; an N+1 here stays invisible
     * until the catalog is full.
     */
    @Test
    void fullListingQueryCountDoesNotGrowWithTheCatalog() throws Exception {
        long small = countQueriesForFullListing();

        for (int i = 0; i < 27; i++) {
            products.save(product(handles, "Товар " + i));
        }
        long large = countQueriesForFullListing();

        assertThat(large)
                .as("query count must not depend on how many products are listed")
                .isEqualTo(small);
    }

    private long countQueriesForFullListing() throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        mockMvc.perform(get("/api/v1/products")).andExpect(status().isOk());

        return statistics.getPrepareStatementCount();
    }
}
