package com.vansisto.lmbe.catalog.web;

import java.util.List;

import com.vansisto.lmbe.catalog.Category;
import com.vansisto.lmbe.catalog.web.dto.CategoryResponse;
import org.mapstruct.Mapper;

/**
 * Entity to DTO at the web boundary. The entity never crosses it — an Adapter in the same
 * position the controller occupies for the happy path.
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);

    List<CategoryResponse> toResponses(List<Category> categories);
}
