package com.vansisto.lmbe.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndActiveTrue(Long id);

    List<Product> findByActiveTrueOrderByIdAsc();

    List<Product> findByCategoryIdAndActiveTrueOrderByNameAscIdAsc(Long categoryId);
}
