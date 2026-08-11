package com.vansisto.lmbe.catalog.web;

import java.util.List;

import com.vansisto.lmbe.catalog.CategoryService;
import com.vansisto.lmbe.catalog.web.dto.CategoryResponse;
import com.vansisto.lmbe.product.ProductService;
import com.vansisto.lmbe.product.web.ProductMapper;
import com.vansisto.lmbe.product.web.dto.ProductSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin by design: validate, delegate, map. No business logic.
 *
 * <p>The nested products listing injects {@link ProductService}, never
 * {@code ProductRepository} — a feature may depend on another feature's service and not on
 * its persistence. That is the line that keeps the two features separable.
 */
@RestController
@RequestMapping(path = "/api/v1/categories", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CategoryController {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final CategoryService categories;
    private final ProductService products;
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    @GetMapping
    @ApiResponse(responseCode = "200", description = "All categories, ordered by name then id.")
    @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public List<CategoryResponse> listCategories() {
        return categoryMapper.toResponses(categories.findAll());
    }

    @GetMapping("/{categoryId}")
    @ApiResponse(responseCode = "200", description = "The category.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CategoryResponse.class)))
    @ApiResponse(responseCode = "400", description = "BAD_REQUEST",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "CATEGORY_NOT_FOUND",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public CategoryResponse getCategory(@PathVariable long categoryId) {
        return categoryMapper.toResponse(categories.findById(categoryId));
    }

    @GetMapping("/{categoryId}/products")
    @Operation(description = """
            An unknown category is 404, not an empty array: the products are addressed as part \
            of the category, so a missing category is a missing resource. A category that exists \
            and holds nothing returns [] with 200 — an empty collection is a successful answer.""")
    @ApiResponse(responseCode = "200", description = "The category's active products, ordered by name then id. May be empty.")
    @ApiResponse(responseCode = "400", description = "BAD_REQUEST",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "CATEGORY_NOT_FOUND",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public List<ProductSummary> listProductsInCategory(@PathVariable long categoryId) {
        categories.requireExists(categoryId);
        return productMapper.toSummaries(products.findByCategory(categoryId));
    }
}
