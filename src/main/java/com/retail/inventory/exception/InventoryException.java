package com.retail.inventory.exception;

/**
 * Base runtime exception for all domain and business rule violations in the inventory system.
 */
public class InventoryException extends RuntimeException {

    public InventoryException(String message) {
        super(message);
    }

    public InventoryException(String message, Throwable cause) {
        super(message, cause);
    }
}