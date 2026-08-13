package com.vansisto.lmbe.common.config;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Origins the admin surface answers with credentials. Supplied per environment; see
 * {@code specs/LM-61/contracts/environment-variables.md}. The constraints are what make a
 * missing {@code LM_ADMIN_ORIGINS} stop startup — {@code specs/LM-61/research.md} R9
 * records why the placeholder alone does not.
 */
@Validated
@ConfigurationProperties(prefix = "lm.cors")
public record CorsProperties(
        @NotEmpty
                List<@Pattern(regexp = ABSOLUTE_HTTP_ORIGIN, message = "must each be an absolute http(s) origin; "
                                + "set LM_ADMIN_ORIGINS") String>
                adminOrigins) {

    private static final String ABSOLUTE_HTTP_ORIGIN = "^https?://\\S+$";
}
