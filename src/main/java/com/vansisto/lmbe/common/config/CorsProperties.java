package com.vansisto.lmbe.common.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lm.cors")
public record CorsProperties(List<String> adminOrigins) {
}
