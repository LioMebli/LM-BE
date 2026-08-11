package com.vansisto.lmbe.common.config;

import java.util.List;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String TITLE = "LioMebli public catalog API";
    private static final String VERSION = "1.0.0";

    @Bean
    public OpenAPI publicCatalogApi() {
        return new OpenAPI()
                .info(new Info().title(TITLE).version(VERSION))
                .servers(List.of(
                        new Server().url("https://api.liomebli.com.ua").description("Production"),
                        new Server().url("http://localhost:8080").description("Local development")));
    }
}
