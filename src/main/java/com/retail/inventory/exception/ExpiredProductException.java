package com.retail.inventory.exception;

/**
 * Thrown when attempting to sell a perishable product whose expiration date (SKT) has already passed.
 */
public class ExpiredProductException extends InventoryException {

    private final String barcode;

    public ExpiredProductException(String barcode, String productName) {
        super(String.format("SKT geçmiş ürün satılamaz: %s [%s]. Lütfen fire (zayi) çıkışı yapın.", productName, barcode));
        this.barcode = barcode;
    }

    public String getBarcode() {
        return barcode;
    }
}