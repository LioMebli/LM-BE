package com.vansisto.lmbe.ops;

import com.vansisto.lmbe.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class SeedRunsOnlyInItsContextIT extends IntegrationTest {

    private static final String SEED_CHANGESET_ID = "seed-catalog";

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void inventedCatalogDoesNotRunWhereItsContextWasNotAskedFor() {
        Integer applied = jdbc.queryForObject(
                "select count(*) from databasechangelog where id = ?", Integer.class, SEED_CHANGESET_ID);

        assertThat(applied)
                .as("""
                        The seed reaching an environment that did not ask for it is one deploy from \
                        inserting invented data into production. contextFilter alone does not prevent it: \
                        Liquibase runs every changeset when no context is supplied at runtime, so this \
                        passes only while spring.liquibase.contexts names one.""")
                .isZero();
    }
}
