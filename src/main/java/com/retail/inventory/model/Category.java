package com.retail.inventory.model;

/**
 * Represents standardized product categories in a retail store.
 */
public enum Category {
    DAIRY("Süt ve Kahvaltılık"),
    BEVERAGE("İçecek"),
    SNACK("Atıştırmalık"),
    BAKERY("Fırın ve Unlu Mamul"),
    PRODUCE("Meyve ve Sebze"),
    CLEANING("Temizlik"),
    BASIC_FOOD("Temel Gıda");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}