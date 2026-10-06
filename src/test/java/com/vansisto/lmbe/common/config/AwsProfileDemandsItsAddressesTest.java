package com.vansisto.lmbe.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;

import static org.assertj.core.api.Assertions.assertThat;

class AwsProfileDemandsItsAddressesTest {

    private static final String ADMIN_ORIGIN = "https://liomebli.example";
    private static final String API_BASE_URL = "https://api.liomebli.example";

    private static final String ADMIN_ORIGINS_SUPPLIED = "LM_ADMIN_ORIGINS=" + ADMIN_ORIGIN;
    private static final String API_BASE_URL_SUPPLIED = "LM_API_BASE_URL=" + API_BASE_URL;

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(BindsBothProperties.class)
            .withPropertyValues("spring.profiles.active=aws");

    @Test
    void startupFailsWhenNeitherAddressIsSupplied() {
        runner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void startupFailsWhenOnlyTheAdminOriginIsSupplied() {
        runner.withPropertyValues(ADMIN_ORIGINS_SUPPLIED).run(failsNaming(ApiProperties.class));
    }

    @Test
    void startupFailsWhenOnlyTheApiAddressIsSupplied() {
        runner.withPropertyValues(API_BASE_URL_SUPPLIED).run(failsNaming(CorsProperties.class));
    }

    @Test
    void bothAddressesArriveWhereTheyAreRead() {
        runner.withPropertyValues(ADMIN_ORIGINS_SUPPLIED, API_BASE_URL_SUPPLIED).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ApiProperties.class).baseUrl()).isEqualTo(API_BASE_URL);
            assertThat(context.getBean(CorsProperties.class).adminOrigins()).containsExactly(ADMIN_ORIGIN);
        });
    }

    private static ContextConsumer<AssertableApplicationContext> failsNaming(Class<?> properties) {
        return context -> assertThat(context)
                .as("A missing value for %s must stop startup, not produce a service that answers wrongly",
                        properties.getSimpleName())
                .hasFailed()
                .getFailure()
                .hasStackTraceContaining(properties.getSimpleName());
    }

    @EnableConfigurationProperties({ApiProperties.class, CorsProperties.class})
    static class BindsBothProperties {
    }
}
