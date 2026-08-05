package com.vansisto.lmbe.catalog;

import com.vansisto.lmbe.IntegrationTest;
import com.vansisto.lmbe.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static com.vansisto.lmbe.CatalogFixture.category;
import static com.vansisto.lmbe.CatalogFixture.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryApiIT extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CategoryRepository categories;
    @Autowired
    private ProductRepository products;

    private Category handles;
    private Category hinges;
    private Category empty;

    @BeforeEach
    void seedCatalog() {
        products.deleteAll();
        categories.deleteAll();

        handles = categories.save(category("Ручки меблеві"));
        hinges = categories.save(category("Петлі"));
        empty = categories.save(category("Аксесуари"));

        products.save(product(handles, "Ручка-скоба РС-115"));
        products.save(product(handles, "Ручка-кнопка РК-20"));
        products.save(product(hinges, "Петля накладна"));
    }

    @Test
    void listsEveryCategory() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].name")
                        .value(containsInAnyOrder("Ручки меблеві", "Петлі", "Аксесуари")));
    }

    @Test
    void returnsOneCategoryByIdentifier() throws Exception {
        mockMvc.perform(get("/api/v1/categories/{id}", hinges.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(hinges.getId()))
                .andExpect(jsonPath("$.name").value("Петлі"));
    }

    @Test
    void listsProductsOfOneCategory() throws Exception {
        mockMvc.perform(get("/api/v1/categories/{id}/products", handles.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].categoryId")
                        .value(everyItem(is(handles.getId().intValue()))));
    }

    /** An empty collection is a successful answer, not a failure. */
    @Test
    void categoryWithoutProductsAnswersEmptyListNotError() throws Exception {
        mockMvc.perform(get("/api/v1/categories/{id}/products", empty.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /**
     * An unordered listing is non-deterministic across releases, which turns a no-op
     * rebuild into a changed prerender diff.
     */
    @Test
    void listingOrderIsStableAcrossCalls() throws Exception {
        String first = mockMvc.perform(get("/api/v1/categories"))
                .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(get("/api/v1/categories"))
                .andReturn().getResponse().getContentAsString();

        assertThat(first).isEqualTo(second);
    }
}
