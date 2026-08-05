package com.vansisto.lmbe.catalog;

import com.vansisto.lmbe.IntegrationTest;
import com.vansisto.lmbe.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static com.vansisto.lmbe.CatalogFixture.category;
import static com.vansisto.lmbe.CatalogFixture.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * FR-006. Every public response is identical for every caller, including one presenting a
 * token.
 *
 * <p>This is not defensive trivia. It is the property the entire cost model rests on: with
 * no response varying by caller, the whole catalog can be cached at the Cloudflare edge and
 * the origin sees a fraction of real traffic. The test exists so that the first
 * {@code if (user != null)} branch inside a public controller turns red instead of quietly
 * making every cached response wrong for somebody.
 */
class PublicApiIsAnonymousIT extends IntegrationTest {

    private static final String SOME_TOKEN = "Bearer eyJhbGciOiJIUzI1NiJ9.e30.signature";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CategoryRepository categories;
    @Autowired
    private ProductRepository products;

    private Long categoryId;
    private Long productId;

    @BeforeEach
    void seedCatalog() {
        products.deleteAll();
        categories.deleteAll();

        Category handles = categories.save(category("Ручки меблеві"));
        categoryId = handles.getId();
        productId = products.save(product(handles, "Ручка-скоба РС-115")).getId();
    }

    @Test
    void everyPublicReadIsIdenticalWithAndWithoutCredentials() throws Exception {
        assertIdenticalWithAndWithoutToken("/api/v1/categories");
        assertIdenticalWithAndWithoutToken("/api/v1/categories/" + categoryId);
        assertIdenticalWithAndWithoutToken("/api/v1/categories/" + categoryId + "/products");
        assertIdenticalWithAndWithoutToken("/api/v1/products");
        assertIdenticalWithAndWithoutToken("/api/v1/products/" + productId);
    }

    private void assertIdenticalWithAndWithoutToken(String path) throws Exception {
        String anonymous = mockMvc.perform(get(path))
                .andReturn().getResponse().getContentAsString();
        String authenticated = mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, SOME_TOKEN))
                .andReturn().getResponse().getContentAsString();

        assertThat(authenticated)
                .as("%s must not vary by caller", path)
                .isEqualTo(anonymous);
    }
}
