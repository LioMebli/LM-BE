package com.vansisto.lmbe.product;

import java.time.Instant;

import com.vansisto.lmbe.catalog.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A catalogue item belonging to exactly one category.
 *
 * <p>No price and no images — those belong to {@code product_variant} and {@code media},
 * which arrive with LM-2. Putting a price column here would be written now and deleted
 * then, and in between it would be the thing everything else coded against.
 */
@Entity
@Table(name = "product")
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * LAZY on purpose. The listings never need the category object, and EAGER would issue
     * one extra query per row over a thousand-row listing — the N+1 this feature is most
     * likely to produce, and the one SC-004 exists to catch.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(name = "short_description", length = 512)
    private String shortDescription;

    @Column(columnDefinition = "text")
    private String description;

    /** STRING, never ORDINAL — see {@link Availability}. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Availability availability;

    /**
     * An inactive product is absent from every listing and answers "not found" by
     * identifier, indistinguishably from one that never existed (FR-014).
     */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private Instant updatedAt;
}
