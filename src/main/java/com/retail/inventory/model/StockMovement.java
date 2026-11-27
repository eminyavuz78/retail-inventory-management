package com.retail.inventory.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable audit record representing a single inventory movement (restock, sale, return, or waste).
 */
public record StockMovement(
        String id,
        String productBarcode,
        MovementType type,
        int quantity,
        BigDecimal unitPrice,
        LocalDateTime timestamp,
        String note
) {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public StockMovement {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Movement ID cannot be null or blank.");
        }
        if (productBarcode == null || productBarcode.isBlank()) {
            throw new IllegalArgumentException("Product barcode cannot be null or blank.");
        }
        Objects.requireNonNull(type, "Movement type cannot be null.");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Movement quantity must be positive.");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be null or negative.");
        }
        Objects.requireNonNull(timestamp, "Timestamp cannot be null.");

        id = id.trim();
        productBarcode = productBarcode.trim();
        unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
        note = (note == null) ? "" : note.trim();
    }

    /**
     * Factory method for recording a new live movement with an auto-generated ID and current timestamp.
     */
    public static StockMovement create(
            String productBarcode,
            MovementType type,
            int quantity,
            BigDecimal unitPrice,
            String note
    ) {
        String shortId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new StockMovement(shortId, productBarcode, type, quantity, unitPrice, LocalDateTime.now(), note);
    }

    /**
     * Calculates the total monetary value of this stock movement.
     */
    public BigDecimal getTotalAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        String sign = type.isInbound() ? "+" : "-";
        String notePart = note.isEmpty() ? "" : " | Not: " + note;
        return String.format(
                "[%s] %s | Barkod: %s | %s (%s%d adet) | Birim: %s TL | Toplam: %s TL%s",
                id,
                timestamp.format(DATE_TIME_FORMATTER),
                productBarcode,
                type.getDisplayName(),
                sign,
                quantity,
                unitPrice,
                getTotalAmount(),
                notePart
        );
    }
}