package com.vansisto.lmbe.common.config;

import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The service's own public address, as callers should reach it. Supplied per environment;
 * see {@code specs/LM-61/contracts/environment-variables.md}. The constraint is what makes
 * a missing {@code LM_API_BASE_URL} stop startup — {@code specs/LM-61/research.md} R9
 * records why the placeholder alone does not.
 */
@Validated
@ConfigurationProperties(prefix = "lm.api")
public record ApiProperties(@Pattern(regexp = ABSOLUTE_HTTP_URL, message = "must be an absolute http(s) address; "
                + "set LM_API_BASE_URL") String baseUrl) {

    private static final String ABSOLUTE_HTTP_URL = "^https?://\\S+$";
}
