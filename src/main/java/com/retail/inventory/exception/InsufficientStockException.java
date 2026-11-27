package com.retail.inventory.exception;

/**
 * Thrown when an outbound movement (sale or waste) exceeds the available stock quantity.
 */
public class InsufficientStockException extends InventoryException {

    private final String barcode;
    private final int requestedQuantity;
    private final int availableQuantity;

    public InsufficientStockException(String barcode, int requestedQuantity, int availableQuantity) {
        super(String.format(
                "Yetersiz stok [%s]: İstenen %d adet, Mevcut %d adet.",
                barcode, requestedQuantity, availableQuantity
        ));
        this.barcode = barcode;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    public String getBarcode() {
        return barcode;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}