package com.vansisto.lmbe.product.web;

import java.util.List;

import com.vansisto.lmbe.product.Product;
import com.vansisto.lmbe.product.web.dto.ProductDetail;
import com.vansisto.lmbe.product.web.dto.ProductSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "categoryId", source = "category.id")
    ProductSummary toSummary(Product product);

    List<ProductSummary> toSummaries(List<Product> products);

    @Mapping(target = "categoryId", source = "category.id")
    ProductDetail toDetail(Product product);
}
