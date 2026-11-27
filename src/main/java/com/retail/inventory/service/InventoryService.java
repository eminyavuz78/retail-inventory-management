package com.retail.inventory.service;

import com.retail.inventory.exception.DuplicateProductException;
import com.retail.inventory.exception.ExpiredProductException;
import com.retail.inventory.exception.InsufficientStockException;
import com.retail.inventory.exception.InventoryException;
import com.retail.inventory.exception.ProductNotFoundException;
import com.retail.inventory.model.Category;
import com.retail.inventory.model.MovementType;
import com.retail.inventory.model.PerishableProduct;
import com.retail.inventory.model.Product;
import com.retail.inventory.model.StockMovement;
import com.retail.inventory.repository.InventoryRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Core business service orchestrating retail inventory operations,
 * FEFO expiration rules, pricing/discount policies, and financial reporting.
 */
public class InventoryService {

    private static final Locale TR_LOCALE = Locale.forLanguageTag("tr-TR");

    private final InventoryRepository repository;

    public InventoryService(InventoryRepository repository) {
        this.repository = Objects.requireNonNull(repository, "InventoryRepository cannot be null.");
    }

    // ==================== PRODUCT CATALOG MANAGEMENT ====================

    /**
     * Registers a new product in the catalog and logs an initial restock movement if stock > 0.
     */
    public void registerProduct(Product product) {
        Objects.requireNonNull(product, "Product cannot be null.");
        if (repository.existsByBarcode(product.getBarcode())) {
            throw new DuplicateProductException(product.getBarcode());
        }
        repository.saveProduct(product);

        if (product.getStockQuantity() > 0) {
            StockMovement initialStock = StockMovement.create(
                    product.getBarcode(),
                    MovementType.RESTOCK,
                    product.getStockQuantity(),
                    product.getPurchasePrice(),
                    "Açılış stoğu kaydı"
            );
            repository.recordMovement(initialStock);
        }
    }

    public Product getProductByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            throw new ProductNotFoundException("");
        }
        return repository.findByBarcode(barcode.trim())
                .orElseThrow(() -> new ProductNotFoundException(barcode.trim()));
    }

    public void removeProduct(String barcode) {
        Product existing = getProductByBarcode(barcode);
        repository.deleteByBarcode(existing.getBarcode());
    }

    public List<Product> getAllProducts() {
        return repository.findAllProducts();
    }

    public List<Product> getProductsByCategory(Category category) {
        return repository.findByCategory(category);
    }

    public List<Product> searchProductsByName(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String normalizedKeyword = keyword.trim().toLowerCase(TR_LOCALE);
        return repository.findAllProducts().stream()
                .filter(product -> product.getName().toLowerCase(TR_LOCALE).contains(normalizedKeyword))
                .toList();
    }

    // ==================== STOCK MOVEMENTS (RESTOCK, SALE, RETURN, WASTE) ====================

    /**
     * Adds incoming supplier stock and logs the movement at unit purchase cost.
     */
    public StockMovement restockProduct(String barcode, int quantity, String note) {
        Product product = getProductByBarcode(barcode);
        validatePositiveQuantity(quantity);

        product.increaseStock(quantity);
        StockMovement movement = StockMovement.create(
                product.getBarcode(),
                MovementType.RESTOCK,
                quantity,
                product.getPurchasePrice(),
                note
        );
        repository.recordMovement(movement);
        return movement;
    }

    /**
     * Executes a standard shelf-price sale after verifying expiration status and stock availability.
     */
    public StockMovement sellProduct(String barcode, int quantity, String note) {
        return sellProductWithDiscount(barcode, quantity, null, false, note);
    }

    /**
     * Executes a sale with an optional instant checkout discount and loss-prevention guard.
     */
    public StockMovement sellProductWithDiscount(
            String barcode,
            int quantity,
            BigDecimal discountPercentage,
            boolean allowBelowCost,
            String note
    ) {
        Product product = getProductByBarcode(barcode);
        validatePositiveQuantity(quantity);

        if (product.isExpired()) {
            throw new ExpiredProductException(product.getBarcode(), product.getName());
        }
        if (quantity > product.getStockQuantity()) {
            throw new InsufficientStockException(product.getBarcode(), quantity, product.getStockQuantity());
        }

        BigDecimal effectiveUnitPrice = product.getSalePrice();
        String effectiveNote = (note == null) ? "" : note.trim();

        if (discountPercentage != null && discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            effectiveUnitPrice = product.calculateDiscountedPrice(discountPercentage);
            if (!allowBelowCost && product.isBelowCost(effectiveUnitPrice)) {
                throw new InventoryException(String.format(
                        "Zararına satış engellendi [%s]: %%%s indirimli fiyat (%s TL), alış maliyetinin (%s TL) altında.",
                        product.getBarcode(), discountPercentage, effectiveUnitPrice, product.getPurchasePrice()
                ));
            }
            String discountTag = "%" + discountPercentage.stripTrailingZeros().toPlainString() + " kasa indirimi";
            effectiveNote = effectiveNote.isEmpty() ? discountTag : effectiveNote + " (" + discountTag + ")";
        }

        product.decreaseStock(quantity);
        StockMovement movement = StockMovement.create(
                product.getBarcode(),
                MovementType.SALE,
                quantity,
                effectiveUnitPrice,
                effectiveNote
        );
        repository.recordMovement(movement);
        return movement;
    }

    /**
     * Processes a customer return, restoring stock and logging the refund at current shelf price.
     */
    public StockMovement returnProduct(String barcode, int quantity, String note) {
        Product product = getProductByBarcode(barcode);
        validatePositiveQuantity(quantity);

        product.increaseStock(quantity);
        StockMovement movement = StockMovement.create(
                product.getBarcode(),
                MovementType.RETURN,
                quantity,
                product.getSalePrice(),
                note
        );
        repository.recordMovement(movement);
        return movement;
    }

    /**
     * Records damaged, spoiled, or expired stock as waste (fire/zayi) valued at purchase cost.
     */
    public StockMovement recordWaste(String barcode, int quantity, String note) {
        Product product = getProductByBarcode(barcode);
        validatePositiveQuantity(quantity);

        if (quantity > product.getStockQuantity()) {
            throw new InsufficientStockException(product.getBarcode(), quantity, product.getStockQuantity());
        }

        product.decreaseStock(quantity);
        StockMovement movement = StockMovement.create(
                product.getBarcode(),
                MovementType.WASTE,
                quantity,
                product.getPurchasePrice(),
                note
        );
        repository.recordMovement(movement);
        return movement;
    }

    // ==================== PRICING, INFLATION & PROMOTIONS ====================

    /**
     * Increases the shelf price of a single product by the given percentage.
     */
    public Product applyPriceIncreaseToProduct(String barcode, BigDecimal percentage) {
        Product product = getProductByBarcode(barcode);
        product.applyPriceIncrease(percentage);
        return product;
    }

    /**
     * Applies a bulk percentage price increase to all products in a given category.
     *
     * @return number of updated products
     */
    public int applyPriceIncreaseToCategory(Category category, BigDecimal percentage) {
        Objects.requireNonNull(category, "Category cannot be null.");
        List<Product> categoryProducts = repository.findByCategory(category);
        for (Product product : categoryProducts) {
            product.applyPriceIncrease(percentage);
        }
        return categoryProducts.size();
    }

    /**
     * Applies a permanent shelf discount to a single product with optional below-cost protection.
     */
    public Product applyDiscountToProduct(String barcode, BigDecimal discountPercentage, boolean allowBelowCost) {
        Product product = getProductByBarcode(barcode);
        BigDecimal candidatePrice = product.calculateDiscountedPrice(discountPercentage);

        if (!allowBelowCost && product.isBelowCost(candidatePrice)) {
            throw new InventoryException(String.format(
                    "Zararına raf indirimi engellendi [%s]: İndirimli fiyat (%s TL), alış maliyetinin (%s TL) altında.",
                    product.getBarcode(), candidatePrice, product.getPurchasePrice()
            ));
        }

        product.applyDiscount(discountPercentage);
        return product;
    }

    /**
     * Applies an automatic clearance discount to perishable products expiring within the given days (FEFO).
     *
     * @return list of perishable products whose prices were discounted
     */
    public List<PerishableProduct> applyClearanceDiscountToExpiringProducts(
            int withinDays,
            BigDecimal discountPercentage,
            boolean allowBelowCost
    ) {
        List<PerishableProduct> expiringProducts = getExpiringSoonProducts(withinDays);
        List<PerishableProduct> discountedProducts = new ArrayList<>();

        for (PerishableProduct product : expiringProducts) {
            BigDecimal candidatePrice = product.calculateDiscountedPrice(discountPercentage);
            if (!allowBelowCost && product.isBelowCost(candidatePrice)) {
                continue;
            }
            product.applyDiscount(discountPercentage);
            discountedProducts.add(product);
        }
        return discountedProducts;
    }

    // ==================== ALERTS, FEFO & FINANCIAL ANALYTICS ====================

    public List<Product> getCriticalStockProducts() {
        return repository.findAllProducts().stream()
                .filter(Product::isCriticalStock)
                .toList();
    }

    public List<PerishableProduct> getAllPerishableProductsByFefo() {
        return repository.findPerishableProductsSortedByExpiry();
    }

    public List<PerishableProduct> getExpiredProducts() {
        return repository.findPerishableProductsSortedByExpiry().stream()
                .filter(PerishableProduct::isExpired)
                .toList();
    }

    public List<PerishableProduct> getExpiringSoonProducts(int withinDays) {
        return repository.findPerishableProductsSortedByExpiry().stream()
                .filter(product -> product.isExpiringWithin(withinDays))
                .toList();
    }

    /**
     * Calculates total current warehouse inventory value based on purchase cost.
     */
    public BigDecimal getTotalInventoryCostValue() {
        return repository.findAllProducts().stream()
                .map(Product::getInventoryCostValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates net sales revenue (total sales minus customer returns).
     */
    public BigDecimal getNetSalesRevenue() {
        BigDecimal grossSales = repository.findAllMovements().stream()
                .filter(m -> m.type() == MovementType.SALE)
                .map(StockMovement::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReturns = repository.findAllMovements().stream()
                .filter(m -> m.type() == MovementType.RETURN)
                .map(StockMovement::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return grossSales.subtract(totalReturns);
    }

    /**
     * Calculates total financial loss recorded from spoiled or damaged goods (waste).
     */
    public BigDecimal getTotalWasteCost() {
        return repository.findAllMovements().stream()
                .filter(m -> m.type() == MovementType.WASTE)
                .map(StockMovement::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<StockMovement> getAllMovements() {
        return repository.findAllMovements();
    }

    public List<StockMovement> getMovementsByBarcode(String barcode) {
        return repository.findMovementsByBarcode(barcode);
    }

    private void validatePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new InventoryException("İşlem adedi 0'dan büyük olmalıdır.");
        }
    }
}