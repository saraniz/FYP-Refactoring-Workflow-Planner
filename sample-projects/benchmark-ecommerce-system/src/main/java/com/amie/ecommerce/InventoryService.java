package com.amie.ecommerce;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages item inventory and stock reservation.
 */
public class InventoryService {

    private final Map<String, Integer> inventoryMap = new HashMap<>();

    public InventoryService() {
        inventoryMap.put("SKU-LAPTOP-01", 15);
        inventoryMap.put("SKU-PHONE-02", 40);
        inventoryMap.put("SKU-HEADPHONES-03", 100);
    }

    public boolean isAvailable(String sku, int quantity) {
        return inventoryMap.getOrDefault(sku, 0) >= quantity;
    }

    public void deductStock(String sku, int quantity) {
        int current = inventoryMap.getOrDefault(sku, 0);
        inventoryMap.put(sku, Math.max(0, current - quantity));
    }
}
