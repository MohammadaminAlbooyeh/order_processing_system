package com.ecommerce.worker.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductCatalogTest {

    @Test
    void hasInventory_sufficientStock_returnsTrue() {
        assertThat(ProductCatalog.hasInventory(10, 10)).isTrue();
        assertThat(ProductCatalog.hasInventory(10, 50)).isTrue();
    }

    @Test
    void hasInventory_insufficientStock_returnsFalse() {
        assertThat(ProductCatalog.hasInventory(10, 51)).isFalse();
        assertThat(ProductCatalog.hasInventory(14, 201)).isFalse();
    }

    @Test
    void hasInventory_unknownProduct_returnsFalse() {
        assertThat(ProductCatalog.hasInventory(999, 10)).isFalse();
    }

    @Test
    void recommendationsFor_basedOnPurchasedProducts() {
        List<Integer> recommendations = ProductCatalog.recommendationsFor(List.of(10, 11), 5);
        assertThat(recommendations).isNotEmpty();
        assertThat(recommendations).containsAnyOf(12, 13);
    }

    @Test
    void recommendationsFor_emptyInput_returnsEmpty() {
        assertThat(ProductCatalog.recommendationsFor(List.of(), 5)).isEmpty();
    }

    @Test
    void recommendationsFor_respectsLimit() {
        List<Integer> recommendations = ProductCatalog.recommendationsFor(List.of(10, 11, 12, 13, 14), 3);
        assertThat(recommendations).hasSizeLessThanOrEqualTo(3);
    }
}
