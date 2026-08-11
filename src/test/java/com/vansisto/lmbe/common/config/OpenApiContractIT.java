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

/**
 * Guards the agreement in {@code specs/LM-10/contracts/openapi.yaml}, which LM-11 codes
 * against, from drifting away from what the service generates.
 *
 * <p>The drift it exists to catch is silent: declaring an explicit {@code @ApiResponse} on a
 * method makes SpringDoc stop inferring the successful one, so an endpoint documents only its
 * failures and the response schema disappears from the document entirely. Nothing about the
 * running service changes, and a client generated from the document loses the return type.
 */
class OpenApiContractIT extends IntegrationTest {

    private static final String API_DOCS = "/v3/api-docs";

    @Autowired
    private MockMvc mockMvc;

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
    void theDocumentedErrorCodesAreTheOnesTheCodeCanProduce() throws Exception {
        List<String> codes = Arrays.stream(ErrorCode.values()).map(Enum::name).toList();

        mockMvc.perform(get(API_DOCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.code.enum")
                        .value(containsInAnyOrder(codes.toArray())));
    }
}
