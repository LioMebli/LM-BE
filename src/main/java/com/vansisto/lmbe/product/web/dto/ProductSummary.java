package com.vansisto.lmbe.product.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"id", "categoryId", "name"})
public record ProductSummary(Long id, Long categoryId, String name) {
}
