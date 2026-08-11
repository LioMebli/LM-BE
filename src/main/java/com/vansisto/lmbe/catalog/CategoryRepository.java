package com.vansisto.lmbe.catalog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Ordered by name, with the identifier as tiebreaker so the order is total. An
     * unordered listing is non-deterministic across releases, which turns a no-op rebuild
     * into a changed prerender diff.
     */
    List<Category> findAllByOrderByNameAscIdAsc();
}
