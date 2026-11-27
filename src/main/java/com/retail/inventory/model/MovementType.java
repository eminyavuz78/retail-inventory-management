package com.retail.inventory.model;

/**
 * Defines inventory stock movement types and whether they increase or decrease stock.
 */
public enum MovementType {
    RESTOCK("Mal Kabul", true),
    SALE("Satış", false),
    RETURN("Müşteri İadesi", true),
    WASTE("Fire / Zayi", false);

    private final String displayName;
    private final boolean inbound;

    MovementType(String displayName, boolean inbound) {
        this.displayName = displayName;
        this.inbound = inbound;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns true if this movement increases stock (restock or customer return),
     * and false if it decreases stock (sale or waste).
     */
    public boolean isInbound() {
        return inbound;
    }
}