package com.vansisto.lmbe;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base for every test that needs the full application against a real PostgreSQL.
 *
 * <p>It carries wiring and nothing else — the Spring context, MockMvc, and the
 * Testcontainers database. That restraint is what makes inheritance the right tool here:
 * anything a test asserts stays in that test, so no assertion is ever inherited from a
 * class the reader has to go and open.
 *
 * <p>H2 is deliberately absent. It diverges from PostgreSQL exactly where the catalog
 * leans on it hardest, and a suite that passes against a database the product will never
 * run on has told you nothing.
 *
 * <p>The {@code test} profile <em>adds to</em> the shipped configuration rather than
 * replacing it (SC-007), so what the suite exercises is what is deployed.
 *
 * <p>Note for anyone arriving from Spring Boot 3: {@code @AutoConfigureMockMvc} moved to
 * {@code org.springframework.boot.webmvc.test.autoconfigure} in Boot 4. The old
 * {@code ...test.autoconfigure.web.servlet} package no longer exists.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles(TestProfile.NAME)
public abstract class IntegrationTest {
}
