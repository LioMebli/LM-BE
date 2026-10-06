package com.vansisto.lmbe.product.web.dto;

import com.vansisto.lmbe.product.Availability;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"id", "categoryId", "name", "availability"})
public record ProductDetail(
        Long id,
        Long categoryId,
        String name,
        String shortDescription,
        String description,
        Availability availability) {
}
