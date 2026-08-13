package com.vansisto.lmbe.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The guarantee {@code specs/LM-61/contracts/environment-variables.md} makes to LM-12: a
 * container started on the {@code aws} profile without {@code LM_ADMIN_ORIGINS} or
 * {@code LM_API_BASE_URL} stops during startup rather than serving something wrong.
 *
 * <p>Written before the constraints that make it hold, and it failed — configuration
 * property binding leaves an unresolvable placeholder as literal text instead of raising.
 * Both properties bound to the string {@code ${LM_...}} and the context started clean, so
 * the service would have published that text as its API address and applied a CORS policy
 * matching no browser. That is the silent failure the profile split was meant to remove,
 * and only this test showed it.
 */
class AwsProfileDemandsItsAddressesTest {

    private static final String ADMIN_ORIGIN = "https://liomebli.example";
    private static final String API_BASE_URL = "https://api.liomebli.example";

    private static final String ADMIN_ORIGINS_SUPPLIED = "LM_ADMIN_ORIGINS=" + ADMIN_ORIGIN;
    private static final String API_BASE_URL_SUPPLIED = "LM_API_BASE_URL=" + API_BASE_URL;

    // ConfigDataApplicationContextInitializer is what makes the runner read the real
    // application.yaml and application-aws.yaml; without it the profile would be a name with
    // no file behind it and every assertion here would pass vacuously.
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(BindsBothProperties.class)
            .withPropertyValues("spring.profiles.active=aws");

    /**
     * Which of the two is reported is not asserted: the binding order of two independent
     * {@code @ConfigurationProperties} beans is not defined, and pinning it here would make
     * the test fail on a change that costs nothing.
     */
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

    /**
     * The failure has to name the value that is missing, otherwise a deploy that forgot one
     * variable reports only "context failed". Matched against the whole stack trace rather
     * than the top message: the binding failure sits nested under whatever the context
     * throws, and asserting the wrapper would pin an implementation detail of Boot.
     */
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
