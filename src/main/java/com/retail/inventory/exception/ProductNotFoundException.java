package com.retail.inventory.exception;

/**
 * Thrown when an operation references a product barcode that does not exist in the system.
 */
public class ProductNotFoundException extends InventoryException {

    private final String barcode;

    public ProductNotFoundException(String barcode) {
        super("Ürün bulunamadı. Kayıtsız barkod: " + barcode);
        this.barcode = barcode;
    }

    public String getBarcode() {
        return barcode;
    }
}