package com.vansisto.lmbe.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A named group of products.
 *
 * <p>The identifier is the public address: {@code /category/{id}} (ADR-012). It never
 * changes, which is why renaming a category costs nothing and why there is no slug column
 * and no redirect table anywhere in this system.
 */
@Entity
@Table(name = "category")
@Getter
@Setter
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;
}
