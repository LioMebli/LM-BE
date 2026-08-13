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
 * {@code application-aws.yaml} that implement it — a property file cannot be checked by
 * reading another property file. {@code aws} is the only profile any deployed environment
 * runs, so this covers dev as much as production.
 */
// Two things this profile demands of its environment, and neither has a default: without
// them the context fails to start, which is the guarantee AwsProfileDemandsItsAddressesTest
// covers separately.
//
// The port is restored because the profile binds `management.server.address`, and Boot
// refuses to start when an address is set while the actuator shares the application's port.
// The `test` profile clears that port for every other suite, so this one has to put it back.
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
            "management.server.port=0",
            "LM_ADMIN_ORIGINS=https://liomebli.example",
            "LM_API_BASE_URL=https://api.liomebli.example"
        })
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles({TestProfile.NAME, "aws"})
class OpenApiIsNotServedInProductionIT {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/swagger-ui/index.html"})
    void theApiDescriptionIsUnreachable(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isNotFound());
    }
}
