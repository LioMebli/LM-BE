package com.vansisto.lmbe.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The {@code active} predicate is written into each method rather than applied globally
 * with a Hibernate {@code @SQLRestriction} on the entity.
 *
 * <p>The annotation is tempting — one line, impossible to forget. It is rejected because it
 * makes inactive rows unreachable <em>through the entity</em>, and the admin panel (LM-5)
 * exists precisely to list and edit them. The workaround at that point is a native query or
 * a second entity mapped to the same table, both worse than the repetition avoided here.
 * Invisibility is a property of the public read path, not of a product.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndActiveTrue(Long id);

    /** Whole catalog, FR-015. Ordered by id — the build does not care, only determinism does. */
    List<Product> findByActiveTrueOrderByIdAsc();

    List<Product> findByCategoryIdAndActiveTrueOrderByNameAscIdAsc(Long categoryId);
}
