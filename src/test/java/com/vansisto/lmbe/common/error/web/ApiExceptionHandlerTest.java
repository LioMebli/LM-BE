package com.vansisto.lmbe.common.error.web;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;
import com.vansisto.lmbe.TestProfile;
import com.vansisto.lmbe.common.error.ErrorCode;
import com.vansisto.lmbe.common.logging.CorrelationId;
import com.vansisto.lmbe.product.ProductNotFoundException;
import com.vansisto.lmbe.product.ProductService;
import com.vansisto.lmbe.product.web.ProductController;
import com.vansisto.lmbe.product.web.ProductMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@ActiveProfiles(TestProfile.NAME)
class ApiExceptionHandlerTest {

    private static final long ABSENT_PRODUCT_ID = 4711L;
    private static final String PROBE_PATH = "/internal-test/echo";
    private static final String MALFORMED_JSON = "{ this is not json";
    private static final String CORRELATION_VALUE = "b7c1f0e2a9d4";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService products;
    @MockitoBean
    private ProductMapper mapper;

    @AfterEach
    void clearCorrelationId() {
        MDC.clear();
    }

    @Test
    void domainNotFoundCarriesItsOwnCode() throws Exception {
        given(products.findById(ABSENT_PRODUCT_ID))
                .willThrow(new ProductNotFoundException(ABSENT_PRODUCT_ID));

        mockMvc.perform(get("/api/v1/products/{id}", ABSENT_PRODUCT_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(ErrorCode.PRODUCT_NOT_FOUND.name()));
    }

    @Test
    void malformedIdentifierIsRejectedInTheSameShape() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(ErrorCode.BAD_REQUEST.name()));
    }

    @Test
    void transportFailuresThatDifferCarryCodesThatDiffer() throws Exception {
        mockMvc.perform(get("/api/v1/nothing-is-mapped-here"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.ENDPOINT_NOT_FOUND.name()));

        mockMvc.perform(post("/api/v1/products"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(ErrorCode.METHOD_NOT_ALLOWED.name()));

        mockMvc.perform(get("/api/v1/products").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_ACCEPTABLE.name()));
    }

    @Test
    void malformedBodyIsRejectedInTheSameShape() throws Exception {
        mockMvc.perform(post(PROBE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MALFORMED_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(ErrorCode.BAD_REQUEST.name()));
    }

    @Test
    void unexpectedFailureIsFiveHundredWithATraceId() throws Exception {
        MDC.put(CorrelationId.MDC_KEY, CORRELATION_VALUE);
        given(products.findAll()).willThrow(new IllegalStateException("pool exhausted"));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_ERROR.name()))
                .andExpect(jsonPath("$.traceId").value(CORRELATION_VALUE));
    }

    @Test
    void traceIdIsOmittedRatherThanInventedWhenNoCorrelationIdExists() throws Exception {
        given(products.findAll()).willThrow(new IllegalStateException("pool exhausted"));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.traceId").doesNotExist());
    }

    @Test
    void frameworkRaisedAndDomainFailuresAreStructurallyIdentical() throws Exception {
        given(products.findById(ABSENT_PRODUCT_ID))
                .willThrow(new ProductNotFoundException(ABSENT_PRODUCT_ID));

        Map<String, Object> domain = propertiesOf(get("/api/v1/products/{id}", ABSENT_PRODUCT_ID));
        Map<String, Object> malformedIdentifier =
                propertiesOf(get("/api/v1/products/{id}", "not-a-number"));
        Map<String, Object> malformedBody = propertiesOf(post(PROBE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(MALFORMED_JSON));

        assertThat(malformedIdentifier).containsOnlyKeys(domain.keySet().toArray(String[]::new));
        assertThat(malformedBody).containsOnlyKeys(domain.keySet().toArray(String[]::new));

        assertThat(domain).containsOnlyKeys("title", "status", "detail", "instance", "code");
    }

    @Test
    void noErrorResponseLeaksInternalDetail() throws Exception {
        given(products.findById(ABSENT_PRODUCT_ID))
                .willThrow(new ProductNotFoundException(ABSENT_PRODUCT_ID));
        given(products.findAll()).willThrow(new IllegalStateException(
                "could not execute statement [select p1_0.id from product p1_0]; "
                        + "org.postgresql.util.PSQLException on jdbc:postgresql://db:5432/lm"
                        + " with password=hunter2"));

        List<String> bodies = List.of(
                bodyOf(get("/api/v1/products/{id}", ABSENT_PRODUCT_ID)),
                bodyOf(get("/api/v1/products/{id}", "not-a-number")),
                bodyOf(get("/api/v1/products")),
                bodyOf(post(PROBE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MALFORMED_JSON)));

        assertThat(bodies).allSatisfy(body -> assertThat(body)
                .doesNotContainIgnoringCase("exception")
                .doesNotContain("com.vansisto")
                .doesNotContain("org.springframework")
                .doesNotContain("org.postgresql")
                .doesNotContain("\tat ")
                .doesNotContainIgnoringCase("select ")
                .doesNotContainIgnoringCase("jdbc:")
                .doesNotContainIgnoringCase("password"));
    }

    private Map<String, Object> propertiesOf(RequestBuilder request) throws Exception {
        return JsonPath.read(bodyOf(request), "$");
    }

    private String bodyOf(RequestBuilder request) throws Exception {
        return mockMvc.perform(request)
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @TestConfiguration
    static class BodyProbeConfiguration {

        @Bean
        BodyProbeController bodyProbeController() {
            return new BodyProbeController();
        }
    }

    @RestController
    static class BodyProbeController {

        @PostMapping(PROBE_PATH)
        void echo(@RequestBody Payload payload) {
        }

        record Payload(String value) {
        }
    }
}
