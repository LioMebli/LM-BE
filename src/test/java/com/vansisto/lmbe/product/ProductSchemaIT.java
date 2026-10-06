package com.vansisto.lmbe.product;

import com.vansisto.lmbe.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
