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

/**
 * The contract of {@code .claude/CLAUDE.md} § Error handling §11: every category is covered by a slice
 * test asserting the status <em>and</em> the {@code code}, because the code is the part a
 * client branches on and an untested contract is a wish.
 *
 * <p>The slice loads the real {@link ProductController} with a mocked service rather than a
 * stub controller. That is the honest arrangement — it exercises the real path-variable
 * binding the 400 case depends on, which a hand-written stub would only imitate.
 */
@WebMvcTest(ProductController.class)
@ActiveProfiles(TestProfile.NAME)
class ApiExceptionHandlerTest {

    private static final long ABSENT_PRODUCT_ID = 4711L;
    private static final String PROBE_PATH = "/internal-test/echo";
    private static final String MALFORMED_JSON = "{ this is not json";
    private static final String CORRELATION_VALUE = "b7c1f0e2a9d4";

    @Autowired
    private MockMvc mockMvc;

    /**
     * Both collaborators of the controller are mocked: no test here reaches a successful
     * mapping, so a real {@code ProductMapperImpl} would only add a generated class name to
     * the wiring of a test that is entirely about failures.
     */
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

    /** Raised by Spring before any application code runs — the case §6 exists for. */
    @Test
    void malformedIdentifierIsRejectedInTheSameShape() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(ErrorCode.BAD_REQUEST.name()));
    }

    /**
     * FR-016. These three answer 400, 404, 405 and 406 respectively, and a client is expected
     * to act differently on each — so sharing one code would make the code carry less
     * information than the status it accompanies, which is the whole reason it exists.
     */
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

    /**
     * The regression test named in §11 for the §6 gap: a body Jackson cannot parse comes back
     * as {@code problem+json} with a code, not as Spring's default error page.
     */
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

    /**
     * The seam between this story and US3, asserted rather than assumed: with no filter
     * populating the MDC the property is simply absent. An invented value would be worse
     * than none — it matches no log line, so the client report it appears in leads nowhere.
     */
    @Test
    void traceIdIsOmittedRatherThanInventedWhenNoCorrelationIdExists() throws Exception {
        given(products.findAll()).willThrow(new IllegalStateException("pool exhausted"));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.traceId").doesNotExist());
    }

    /**
     * SC-002, stated as an assertion rather than left to an eyeball over a build log: a
     * failure Spring raises before application code runs carries exactly the same property
     * names as one the domain threw. Values differ and should; the shape is the contract,
     * and it is the shape a client parses generically.
     */
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

        // `type` is absent from all three, not missing from some: Spring omits it while it
        // holds the default `about:blank`, which RFC 9457 §3.1 makes the value a consumer
        // assumes when the member is not there. Asserted as an exact key set so that a
        // future handler adding `type` to one path and not another turns red.
        assertThat(domain).containsOnlyKeys("title", "status", "detail", "instance", "code");
    }

    /**
     * FR-008 and §10. Asserted across every error the API can produce rather than one of
     * them, because a leak is introduced by the case nobody enumerated: the underlying
     * exception messages below deliberately carry a SQL fragment, a driver class name and a
     * connection string, and none of it may reach the wire.
     */
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

    /**
     * LM-10's API is read-only, so nothing in it accepts a request body — and the one error
     * §11 names by hand is a body Jackson cannot parse. This endpoint exists only so that
     * failure has somewhere to happen; it is registered by this test and by nothing else.
     *
     * <p>Delete it when the first writing endpoint lands (LM-5) and point the test at that
     * instead: a real endpoint proves the same thing without a fixture to explain.
     */
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
            // Never reached: every call in this test fails while the body is being read.
        }

        record Payload(String value) {
        }
    }
}
