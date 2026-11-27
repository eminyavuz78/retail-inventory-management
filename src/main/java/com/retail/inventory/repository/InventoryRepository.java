package com.retail.inventory.repository;

import com.retail.inventory.model.Category;
import com.retail.inventory.model.PerishableProduct;
import com.retail.inventory.model.Product;
import com.retail.inventory.model.StockMovement;

import java.util.List;
import java.util.Optional;

/**
 * Defines the data access contract for managing retail products and stock movement records.
 */
public interface InventoryRepository {

    void saveProduct(Product product);

    Optional<Product> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);

    boolean deleteByBarcode(String barcode);

    List<Product> findAllProducts();

    List<Product> findByCategory(Category category);

    int countProducts();

    /**
     * Returns all perishable products ordered by earliest expiration date first (FEFO).
     */
    List<PerishableProduct> findPerishableProductsSortedByExpiry();

    /**
     * Returns the perishable product with the closest expiration date, if any exists.
     */
    Optional<PerishableProduct> findNextExpiringProduct();

    void recordMovement(StockMovement movement);

    List<StockMovement> findAllMovements();

    List<StockMovement> findMovementsByBarcode(String barcode);
}