package com.retail.inventory.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Represents a perishable retail product with an expiration date (SKT).
 * Implements Comparable by expiration date to support FEFO (First Expired, First Out) ordering.
 */
public class PerishableProduct extends Product implements Comparable<PerishableProduct> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private LocalDate expirationDate;

    public PerishableProduct(
            String barcode,
            String name,
            Category category,
            BigDecimal purchasePrice,
            BigDecimal salePrice,
            int stockQuantity,
            int criticalStockThreshold,
            LocalDate expirationDate
    ) {
        super(barcode, name, category, purchasePrice, salePrice, stockQuantity, criticalStockThreshold);
        this.expirationDate = Objects.requireNonNull(expirationDate, "Expiration date cannot be null.");
    }

    /**
     * A product is considered expired once the current date passes its expiration date.
     */
    @Override
    public boolean isExpired() {
        return LocalDate.now().isAfter(expirationDate);
    }

    /**
     * Returns remaining days until expiration (0 = expires today, negative = already expired).
     */
    public long daysUntilExpiration() {
        return ChronoUnit.DAYS.between(LocalDate.now(), expirationDate);
    }

    /**
     * Checks whether the product is still valid but will expire within the given number of days.
     */
    public boolean isExpiringWithin(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("Days threshold cannot be negative.");
        }
        return !isExpired() && daysUntilExpiration() <= days;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = Objects.requireNonNull(expirationDate, "Expiration date cannot be null.");
    }

    @Override
    public int compareTo(PerishableProduct other) {
        Objects.requireNonNull(other, "Cannot compare to null PerishableProduct.");
        int dateComparison = this.expirationDate.compareTo(other.expirationDate);
        if (dateComparison != 0) {
            return dateComparison;
        }
        return this.getBarcode().compareTo(other.getBarcode());
    }

    @Override
    public String toString() {
        String status = isExpired() ? " [SKT GEÇMİŞ!]" : "";
        return super.toString() + " | SKT: " + expirationDate.format(DATE_FORMATTER) + status;
    }
}