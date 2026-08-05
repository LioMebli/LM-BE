package com.vansisto.lmbe.ops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vansisto.lmbe.TestProfile;
import com.vansisto.lmbe.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

// Real ports rather than MockMvc: the property under test is that two listeners exist and
// serve different things, which a single mock dispatcher cannot distinguish.
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.server.port=0")
@Import(TestcontainersConfiguration.class)
@ActiveProfiles(TestProfile.NAME)
class ActuatorIT {

    private static final String READINESS = "/actuator/health/readiness";
    private static final String PROMETHEUS = "/actuator/prometheus";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @Value("${local.server.port}")
    private int applicationPort;

    @Value("${local.management.port}")
    private int managementPort;

    @Test
    void readinessReportsUpAndActuallyLooksAtTheDatabase() throws Exception {
        HttpResponse<String> response = get(managementPort, READINESS);

        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode body = json.readTree(response.body());
        assertThat(body.path("status").asText()).isEqualTo("UP");
        assertThat(body.path("components").has("db"))
                .as("""
                        Readiness must include the database, not merely the process state. The \
                        group membership that makes this true is set only in the shipped \
                        application.yaml, so this assertion is also what proves the suite reads \
                        it (SC-007) rather than a copy written for the tests.""")
                .isTrue();
    }

    @Test
    void prometheusIsServedOnTheManagementPortOnly() throws Exception {
        HttpResponse<String> onManagementPort = get(managementPort, PROMETHEUS);
        assertThat(onManagementPort.statusCode()).isEqualTo(200);
        assertThat(onManagementPort.body()).contains("jvm_memory_used_bytes");

        HttpResponse<String> onApplicationPort = get(applicationPort, PROMETHEUS);
        assertThat(onApplicationPort.statusCode())
                .as("the public listener must not serve metrics")
                .isEqualTo(404);
    }

    private HttpResponse<String> get(int port, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
