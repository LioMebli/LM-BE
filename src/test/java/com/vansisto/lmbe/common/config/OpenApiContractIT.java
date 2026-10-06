package com.vansisto.lmbe.common.config;

import java.util.Arrays;
import java.util.List;

import com.vansisto.lmbe.IntegrationTest;
import com.vansisto.lmbe.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiContractIT extends IntegrationTest {

    private static final String API_DOCS = "/v3/api-docs";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiProperties apiProperties;

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/categories",
            "/api/v1/categories/{categoryId}",
            "/api/v1/categories/{categoryId}/products",
            "/api/v1/products",
            "/api/v1/products/{productId}"})
    void everyPublishedOperationDocumentsItsSuccessfulResponse(String path) throws Exception {
        mockMvc.perform(get(API_DOCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['" + path + "'].get.responses['200'].content").exists());
    }

    @Test
    void theDocumentPublishesOneServerAndTakesItsAddressFromConfiguration() throws Exception {
        String configuredBaseUrl = apiProperties.baseUrl();

        mockMvc.perform(get(API_DOCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers.length()").value(1))
                .andExpect(jsonPath("$.servers[0].url").value(configuredBaseUrl));
    }

    @Test
    void theDocumentedErrorCodesAreTheOnesTheCodeCanProduce() throws Exception {
        List<String> codes = Arrays.stream(ErrorCode.values()).map(Enum::name).toList();

        mockMvc.perform(get(API_DOCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.code.enum")
                        .value(containsInAnyOrder(codes.toArray())));
    }
}
