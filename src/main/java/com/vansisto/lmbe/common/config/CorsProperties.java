package com.vansisto.lmbe.common.config;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Origins the admin surface answers with credentials. Supplied per environment; see
 * {@code specs/LM-61/contracts/environment-variables.md}.
 *
 * <p>The constraints here answer "did a real address arrive", which is a different question
 * from the one {@code CorsConfig} answers — that a caller-supplied origin is safe to echo
 * back with credentials. Both are load-bearing: this one runs at binding, and the guard in
 * {@code CorsConfig} also covers the bean being constructed directly.
 */
@Validated
@ConfigurationProperties(prefix = "lm.cors")
public record CorsProperties(
        @NotEmpty List<@Pattern(regexp = ApiProperties.ABSOLUTE_HTTP_URL, message = MUST_BE_ADDRESSES) String>
                adminOrigins) {

    static final String MUST_BE_ADDRESSES =
            "must each be an absolute http(s) origin. A value still reading ${LM_ADMIN_ORIGINS} means the "
                    + "variable was never supplied, and the service would otherwise start with a CORS policy "
                    + "matching no browser at all";
}
