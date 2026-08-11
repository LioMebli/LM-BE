package com.vansisto.lmbe.product.web;

import java.util.List;

import com.vansisto.lmbe.product.ProductService;
import com.vansisto.lmbe.product.web.dto.ProductDetail;
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

@RestController
@RequestMapping(path = "/api/v1/products", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ProductController {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final ProductService products;
    private final ProductMapper mapper;

    @GetMapping
    @Operation(description = """
            The whole catalog in one response. Not paginated and never will be: the only caller \
            is a once-per-release build, and filtered browsing is LM-3's own resource.""")
    @ApiResponse(responseCode = "200", description = "All active products, ordered by id.")
    @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public List<ProductSummary> listProducts() {
        return mapper.toSummaries(products.findAll());
    }

    @GetMapping("/{productId}")
    @Operation(description = """
            An inactive product answers 404, indistinguishably from one that never existed — a \
            client cannot use this endpoint to learn that an identifier was once in use.""")
    @ApiResponse(responseCode = "200", description = "The product.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProductDetail.class)))
    @ApiResponse(responseCode = "400", description = "BAD_REQUEST",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ProductDetail getProduct(@PathVariable long productId) {
        return mapper.toDetail(products.findById(productId));
    }
}
