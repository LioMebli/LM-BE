package com.vansisto.lmbe.common.config;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "lm.cors")
public record CorsProperties(
        @NotEmpty
                List<@Pattern(regexp = ABSOLUTE_HTTP_ORIGIN_PATTERN, message = "must each be an absolute http(s) origin; "
                                + "set LM_ADMIN_ORIGINS") String>
                adminOrigins) {

    private static final String ABSOLUTE_HTTP_ORIGIN_PATTERN = "^https?://\\S+$";
}
