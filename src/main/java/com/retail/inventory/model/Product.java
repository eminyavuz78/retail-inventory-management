package com.retail.inventory.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Represents a standard retail product tracked by a unique barcode,
 * including stock control, cost valuation, and price adjustment operations.
 */
public class Product {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final String barcode;
    private String name;
    private Category category;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private int stockQuantity;
    private int criticalStockThreshold;

    public Product(
            String barcode,
            String name,
            Category category,
            BigDecimal purchasePrice,
            BigDecimal salePrice,
            int stockQuantity,
            int criticalStockThreshold
    ) {
        this.barcode = validateText(barcode, "Barcode");
        this.name = validateText(name, "Product name");
        this.category = Objects.requireNonNull(category, "Category cannot be null.");
        this.purchasePrice = validatePrice(purchasePrice, "Purchase price");
        this.salePrice = validatePrice(salePrice, "Sale price");
        this.stockQuantity = validateNonNegative(stockQuantity, "Stock quantity");
        this.criticalStockThreshold = validateNonNegative(criticalStockThreshold, "Critical stock threshold");
    }

    /**
     * Increases current stock after restocking or a customer return.
     */
    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to add must be positive.");
        }
        this.stockQuantity += quantity;
    }

    /**
     * Decreases current stock after a sale or recorded waste.
     */
    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to remove must be positive.");
        }
        if (quantity > this.stockQuantity) {
            throw new IllegalStateException("Insufficient stock for barcode: " + barcode);
        }
        this.stockQuantity -= quantity;
    }

    /**
     * Permanently increases the shelf sale price by the given percentage (e.g., 10 for +10%).
     */
    public void applyPriceIncrease(BigDecimal percentage) {
        if (percentage == null || percentage.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Increase percentage must be positive.");
        }
        BigDecimal increaseAmount = salePrice.multiply(percentage)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        this.salePrice = salePrice.add(increaseAmount).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Permanently reduces the shelf sale price by the given percentage (e.g., 25 for -25%).
     * Used for promotional shelf campaigns or near-expiry clearance markdowns.
     */
    public void applyDiscount(BigDecimal discountPercentage) {
        this.salePrice = calculateDiscountedPrice(discountPercentage);
    }

    /**
     * Calculates a discounted unit price without altering the product's shelf price.
     * Used for instant checkout discounts.
     */
    public BigDecimal calculateDiscountedPrice(BigDecimal discountPercentage) {
        if (discountPercentage == null
                || discountPercentage.compareTo(BigDecimal.ZERO) <= 0
                || discountPercentage.compareTo(ONE_HUNDRED) >= 0) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100 (exclusive).");
        }
        BigDecimal discountAmount = salePrice.multiply(discountPercentage)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        return salePrice.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Checks whether a given price falls below the unit purchase cost (selling at a loss).
     */
    public boolean isBelowCost(BigDecimal candidatePrice) {
        Objects.requireNonNull(candidatePrice, "Candidate price cannot be null.");
        return candidatePrice.compareTo(purchasePrice) < 0;
    }

    /**
     * Checks whether the current stock has reached or dropped below the critical threshold.
     */
    public boolean isCriticalStock() {
        return this.stockQuantity <= this.criticalStockThreshold;
    }

    /**
     * Standard non-perishable products do not expire.
     * Subclasses like PerishableProduct override this behavior.
     */
    public boolean isExpired() {
        return false;
    }

    /**
     * Calculates gross profit per single unit sold.
     */
    public BigDecimal getUnitProfit() {
        return salePrice.subtract(purchasePrice);
    }

    /**
     * Calculates total inventory valuation at purchase cost (standard retail accounting).
     */
    public BigDecimal getInventoryCostValue() {
        return purchasePrice.multiply(BigDecimal.valueOf(stockQuantity));
    }

    public String getBarcode() {
        return barcode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = validateText(name, "Product name");
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = Objects.requireNonNull(category, "Category cannot be null.");
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = validatePrice(purchasePrice, "Purchase price");
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = validatePrice(salePrice, "Sale price");
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public int getCriticalStockThreshold() {
        return criticalStockThreshold;
    }

    public void setCriticalStockThreshold(int criticalStockThreshold) {
        this.criticalStockThreshold = validateNonNegative(criticalStockThreshold, "Critical stock threshold");
    }

    private String validateText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank.");
        }
        return value.trim();
    }

    private BigDecimal validatePrice(BigDecimal price, String fieldName) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be null or negative.");
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private int validateNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative.");
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product other)) return false;
        return barcode.equals(other.barcode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(barcode);
    }

    @Override
    public String toString() {
        return String.format(
                "[%s] %s | Kategori: %s | Alış: %s TL | Satış: %s TL | Stok: %d (Kritik: %d)",
                barcode, name, category.getDisplayName(), purchasePrice, salePrice, stockQuantity, criticalStockThreshold
        );
    }
}