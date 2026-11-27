package com.retail.inventory.exception;

/**
 * Thrown when attempting to register a product with a barcode that is already in use.
 */
public class DuplicateProductException extends InventoryException {

    private final String barcode;

    public DuplicateProductException(String barcode) {
        super("Bu barkod sistemde zaten kayıtlı: " + barcode);
        this.barcode = barcode;
    }

    public String getBarcode() {
        return barcode;
    }
}