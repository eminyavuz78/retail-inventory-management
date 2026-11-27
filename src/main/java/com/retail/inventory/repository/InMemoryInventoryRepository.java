package com.retail.inventory.repository;

import com.retail.inventory.model.Category;
import com.retail.inventory.model.PerishableProduct;
import com.retail.inventory.model.Product;
import com.retail.inventory.model.StockMovement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * In-memory implementation of InventoryRepository backed by:
 * - LinkedHashMap for O(1) barcode lookups while preserving insertion order
 * - PriorityQueue (Min-Heap) for FEFO expiration tracking
 * - ArrayList for chronological audit logging
 */
public class InMemoryInventoryRepository implements InventoryRepository {

    private final Map<String, Product> productCatalog = new LinkedHashMap<>();
    private final List<StockMovement> movementHistory = new ArrayList<>();

    @Override
    public void saveProduct(Product product) {
        Objects.requireNonNull(product, "Product cannot be null.");
        productCatalog.put(product.getBarcode(), product);
    }

    @Override
    public Optional<Product> findByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(productCatalog.get(barcode.trim()));
    }

    @Override
    public boolean existsByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return false;
        }
        return productCatalog.containsKey(barcode.trim());
    }

    @Override
    public boolean deleteByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return false;
        }
        return productCatalog.remove(barcode.trim()) != null;
    }

    @Override
    public List<Product> findAllProducts() {
        return List.copyOf(productCatalog.values());
    }

    @Override
    public List<Product> findByCategory(Category category) {
        Objects.requireNonNull(category, "Category cannot be null.");
        return productCatalog.values().stream()
                .filter(product -> product.getCategory() == category)
                .toList();
    }

    @Override
    public int countProducts() {
        return productCatalog.size();
    }

    @Override
    public List<PerishableProduct> findPerishableProductsSortedByExpiry() {
        PriorityQueue<PerishableProduct> fefoQueue = buildFefoQueue();
        List<PerishableProduct> sortedList = new ArrayList<>(fefoQueue.size());
        while (!fefoQueue.isEmpty()) {
            sortedList.add(fefoQueue.poll());
        }
        return sortedList;
    }

    @Override
    public Optional<PerishableProduct> findNextExpiringProduct() {
        return Optional.ofNullable(buildFefoQueue().peek());
    }

    @Override
    public void recordMovement(StockMovement movement) {
        Objects.requireNonNull(movement, "Stock movement cannot be null.");
        movementHistory.add(movement);
    }

    @Override
    public List<StockMovement> findAllMovements() {
        return List.copyOf(movementHistory);
    }

    @Override
    public List<StockMovement> findMovementsByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return List.of();
        }
        String normalized = barcode.trim();
        return movementHistory.stream()
                .filter(movement -> movement.productBarcode().equals(normalized))
                .toList();
    }

    private PriorityQueue<PerishableProduct> buildFefoQueue() {
        PriorityQueue<PerishableProduct> queue = new PriorityQueue<>();
        for (Product product : productCatalog.values()) {
            if (product instanceof PerishableProduct perishable) {
                queue.add(perishable);
            }
        }
        return queue;
    }
}