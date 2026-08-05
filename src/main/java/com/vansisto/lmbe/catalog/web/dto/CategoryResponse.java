package com.vansisto.lmbe.catalog.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Serves both the listing and the single-category read — they are the same projection.
 * Two identical records named {@code CategorySummary} and {@code CategoryDetail} would be
 * indirection with nobody paying for it.
 */
@Schema(requiredProperties = {"id", "name"})
public record CategoryResponse(Long id, String name) {
}
