package com.vansisto.lmbe.product.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What a catalog card and a build manifest need, and nothing else.
 *
 * <p>{@code categoryId} is redundant in the nested listing and present anyway: it is what
 * makes {@code GET /api/v1/products} self-sufficient, so the build can group the whole
 * catalog locally instead of asking once per category.
 */
@Schema(requiredProperties = {"id", "categoryId", "name"})
public record ProductSummary(Long id, Long categoryId, String name) {
}
