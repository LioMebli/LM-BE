package com.vansisto.lmbe.product.web.dto;

import com.vansisto.lmbe.product.Availability;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * No price and no image: they belong to {@code product_variant} and {@code media} and
 * arrive with LM-2 (spec Clarification 1). A product page is visibly incomplete until then,
 * which is the accepted cost of keeping the walking skeleton thin.
 */
@Schema(requiredProperties = {"id", "categoryId", "name", "availability"})
public record ProductDetail(
        Long id,
        Long categoryId,
        String name,
        String shortDescription,
        String description,
        Availability availability) {
}
