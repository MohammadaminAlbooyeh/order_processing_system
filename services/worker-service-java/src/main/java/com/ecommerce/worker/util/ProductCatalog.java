package com.ecommerce.worker.util;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mock inventory and "related products" lookup, mirroring the hardcoded
 * dictionaries duplicated across process_order/recommend_products/MockMLModel
 * in the original Python tasks.py.
 */
public final class ProductCatalog {

    private static final Map<Integer, Integer> INVENTORY = Map.of(
            10, 50,
            11, 30,
            12, 100,
            13, 15,
            14, 200
    );

    private static final Map<Integer, List<Integer>> RELATED_PRODUCTS = Map.of(
            10, List.of(11, 12),
            11, List.of(10, 13),
            12, List.of(10, 14),
            13, List.of(11, 14),
            14, List.of(12, 13)
    );

    private ProductCatalog() {
    }

    public static boolean hasInventory(int productId, int quantity) {
        return INVENTORY.getOrDefault(productId, 0) >= quantity;
    }

    public static List<Integer> recommendationsFor(Iterable<Integer> productIds, int limit) {
        Set<Integer> recommendations = new LinkedHashSet<>();
        for (int productId : productIds) {
            recommendations.addAll(RELATED_PRODUCTS.getOrDefault(productId, List.of()));
        }
        return recommendations.stream().limit(limit).toList();
    }
}
