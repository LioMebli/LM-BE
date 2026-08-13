package com.vansisto.lmbe.common.config;

import java.util.List;

import com.vansisto.lmbe.common.logging.CorrelationId;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsConfigTest {

    private static final String ADMIN_ORIGIN = "https://liomebli.example";
    private static final String FOREIGN_ORIGIN = "https://not-liomebli.example";

    private final CorsConfigurationSource source =
            new CorsConfig().corsConfigurationSource(new CorsProperties(List.of(ADMIN_ORIGIN)));

    @Test
    void publicSurfaceAllowsAnyOriginWithoutCredentials() {
        CorsConfiguration configuration = resolve("/api/v1/products/1042");

        assertThat(configuration.getAllowedOrigins()).containsExactly(CorsConfiguration.ALL);
        assertThat(configuration.getAllowCredentials()).isFalse();
        assertThat(configuration.getAllowedMethods()).containsExactly("GET", "HEAD");
    }

    /** The admin pattern is a subset of the public one, so this proves the two do not collide. */
    @Test
    void adminSurfaceKeepsStrictPolicyDespiteOverlappingPublicPattern() {
        CorsConfiguration configuration = resolve("/api/v1/admin/products");

        assertThat(configuration.getAllowedOrigins()).containsExactly(ADMIN_ORIGIN);
        assertThat(configuration.getAllowCredentials()).isTrue();
    }

    @Test
    void adminSurfaceRejectsOriginOutsideAllowlist() {
        CorsConfiguration configuration = resolve("/api/v1/admin/products");

        assertThat(configuration.checkOrigin(FOREIGN_ORIGIN)).isNull();
    }

    @Test
    void adminSurfaceAcceptsConfiguredOrigin() {
        CorsConfiguration configuration = resolve("/api/v1/admin/products");

        assertThat(configuration.checkOrigin(ADMIN_ORIGIN)).isEqualTo(ADMIN_ORIGIN);
    }

    @Test
    void bothSurfacesLetTheBrowserReadTheCorrelationId() {
        assertThat(resolve("/api/v1/products/1042").getExposedHeaders())
                .containsExactly(CorrelationId.HEADER);
        assertThat(resolve("/api/v1/admin/products").getExposedHeaders())
                .containsExactly(CorrelationId.HEADER);
    }

    @Test
    void wildcardInAdminAllowlistFailsFastAtStartup() {
        CorsProperties properties = new CorsProperties(List.of(CorsConfiguration.ALL));

        assertThatThrownBy(() -> new CorsConfig().corsConfigurationSource(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("wildcard");
    }

    @Test
    void emptyAdminAllowlistFailsFastAtStartup() {
        CorsProperties properties = new CorsProperties(List.of());

        assertThatThrownBy(() -> new CorsConfig().corsConfigurationSource(properties))
                .isInstanceOf(IllegalStateException.class);
    }

    private CorsConfiguration resolve(String path) {
        return source.getCorsConfiguration(new MockHttpServletRequest("GET", path));
    }
}
