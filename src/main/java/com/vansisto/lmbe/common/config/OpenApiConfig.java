package com.vansisto.lmbe.common.config;

import java.util.List;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ApiProperties.class)
public class OpenApiConfig {

    private static final String TITLE = "LioMebli public catalog API";
    private static final String VERSION = "1.0.0";
    private static final String SERVER_DESCRIPTION = "This environment";

    @Bean
    public OpenAPI publicCatalogApi(ApiProperties properties) {
        return new OpenAPI()
                .info(new Info().title(TITLE).version(VERSION))
                .servers(List.of(new Server().url(properties.baseUrl()).description(SERVER_DESCRIPTION)));
    }
}
