package com.vansisto.lmbe.common.config;

import com.vansisto.lmbe.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicCacheIT extends IntegrationTest {

    private static final long ABSENT_PRODUCT_ID = 999_999L;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aSuccessfulPublicReadMayBeCached() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=60, public"));
    }

    /**
     * A cached 404 outlives the fix: the product is created, and callers keep being told it
     * does not exist until the entry expires.
     */
    @Test
    void aFailureIsNeverCached() throws Exception {
        mockMvc.perform(get("/api/v1/products/" + ABSENT_PRODUCT_ID))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, nullValue()));
    }
}
