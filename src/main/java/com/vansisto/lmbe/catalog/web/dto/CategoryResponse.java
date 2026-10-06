package com.vansisto.lmbe.catalog.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"id", "name"})
public record CategoryResponse(Long id, String name) {
}
