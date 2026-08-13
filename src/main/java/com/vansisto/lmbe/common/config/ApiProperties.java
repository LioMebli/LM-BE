package com.vansisto.lmbe.common.config;

import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The service's own public address, as callers should reach it. Supplied per environment;
 * see {@code specs/LM-61/contracts/environment-variables.md}.
 */
@Validated
@ConfigurationProperties(prefix = "lm.api")
public record ApiProperties(@Pattern(regexp = ABSOLUTE_HTTP_URL, message = MUST_BE_AN_ADDRESS) String baseUrl) {

    static final String ABSOLUTE_HTTP_URL = "^https?://\\S+$";

    static final String MUST_BE_AN_ADDRESS =
            "must be an absolute http(s) address. A value still reading ${LM_API_BASE_URL} means the variable "
                    + "was never supplied: configuration property binding leaves an unresolvable placeholder as "
                    + "literal text instead of failing, so without this constraint the service starts and "
                    + "publishes that text as its own address";
}
