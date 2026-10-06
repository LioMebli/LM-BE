package com.vansisto.lmbe.common.config;

import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "lm.api")
public record ApiProperties(@Pattern(regexp = ABSOLUTE_HTTP_URL_PATTERN, message = "must be an absolute http(s) address; "
                + "set LM_API_BASE_URL") String baseUrl) {

    private static final String ABSOLUTE_HTTP_URL_PATTERN = "^https?://\\S+$";
}
