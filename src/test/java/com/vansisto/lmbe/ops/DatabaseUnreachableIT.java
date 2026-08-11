package com.vansisto.lmbe.ops;

import com.vansisto.lmbe.TestProfile;
import com.vansisto.lmbe.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Stopping the container poisons this context permanently: hence @DirtiesContext, and
// hence the distinct properties, which give the class a context cache key of its own.
@SpringBootTest(properties = {
        "spring.datasource.hikari.connection-timeout=2000",
        "spring.datasource.hikari.validation-timeout=1000"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles(TestProfile.NAME)
class DatabaseUnreachableIT {

    /** Fragments a real SQLException or Hibernate message would carry. */
    private static final String[] INTERNAL_VOCABULARY = {
            "select", "insert", "jdbc", "postgres", "sql", "hikari", "connection", "exception"
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostgreSQLContainer database;

    @Test
    void readinessTellsTheTruthWhileTheApiGivesNothingAway() throws Exception {
        database.stop();

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(not("UP")));

        String body = mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body.toLowerCase())
                .as("a failure the client cannot fix must not describe the machinery that failed")
                .doesNotContain(INTERNAL_VOCABULARY);
    }
}
