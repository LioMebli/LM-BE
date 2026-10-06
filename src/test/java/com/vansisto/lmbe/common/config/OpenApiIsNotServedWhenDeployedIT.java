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
class OpenApiIsNotServedWhenDeployedIT {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/swagger-ui/index.html"})
    void theApiDescriptionIsUnreachable(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isNotFound());
    }
}
