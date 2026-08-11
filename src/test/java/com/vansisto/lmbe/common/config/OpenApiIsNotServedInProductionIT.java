package com.vansisto.lmbe.common.config;

import com.vansisto.lmbe.TestProfile;
import com.vansisto.lmbe.TestcontainersConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FR-017, asserted against the profile that ships rather than against the two lines in
 * {@code application-prod.yaml} that implement it — a property file cannot be checked by
 * reading another property file.
 */
// The port is restored because prod binds `management.server.address`, and Boot refuses to
// start when an address is set while the actuator shares the application's port. The `test`
// profile clears that port for every other suite, so this one has to put it back.
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "management.server.port=0")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles({TestProfile.NAME, "prod"})
class OpenApiIsNotServedInProductionIT {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/swagger-ui/index.html"})
    void theApiDescriptionIsUnreachable(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isNotFound());
    }
}
