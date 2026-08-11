package com.vansisto.lmbe.product;

import com.vansisto.lmbe.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FR-013. The foreign key is written in the changeset and would otherwise be verified by
 * nothing — a key that has never been observed rejecting anything is an assumption, not a
 * guarantee.
 *
 * <p>Deliberately goes through JDBC rather than the repository: the point is that the
 * <em>database</em> refuses, not that the mapping happens to make it awkward.
 */
class ProductSchemaIT extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void productCannotReferenceCategoryThatDoesNotExist() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into product (category_id, name, availability, is_active)
                values (?, ?, ?, ?)
                """, 999_999L, "Сирота", "IN_STOCK", true))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void availabilityOutsideTheAllowedSetIsRejected() {
        Long categoryId = jdbc.queryForObject("""
                insert into category (name) values ('Тимчасова') returning id
                """, Long.class);

        assertThatThrownBy(() -> jdbc.update("""
                insert into product (category_id, name, availability, is_active)
                values (?, ?, ?, ?)
                """, categoryId, "Невідомий стан", "MAYBE_SOMEDAY", true))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
