package com.retail.inventory.service;

import com.retail.inventory.exception.ExpiredProductException;
import com.retail.inventory.exception.InsufficientStockException;
import com.retail.inventory.exception.InventoryException;
import com.retail.inventory.model.Category;
import com.retail.inventory.model.PerishableProduct;
import com.retail.inventory.model.Product;
import com.retail.inventory.model.StockMovement;
import com.retail.inventory.repository.InMemoryInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryServiceTest {

    private InventoryService service;

    @BeforeEach
    void setUp() {
        service = new InventoryService(new InMemoryInventoryRepository());
    }

    @Test
    @DisplayName("Standart satış stoğu düşürmeli ve net ciroyu doğru hesaplamalı")
    void shouldDecreaseStockAndCalculateNetRevenueOnSale() {
        Product tea = new Product(
                "8690001", "Siyah Çay 1kg", Category.BASIC_FOOD,
                new BigDecimal("100.00"), new BigDecimal("150.00"), 20, 5
        );
        service.registerProduct(tea);

        service.sellProduct("8690001", 3, "Kasa 1");

        assertEquals(17, service.getProductByBarcode("8690001").getStockQuantity());
        assertEquals(new BigDecimal("450.00"), service.getNetSalesRevenue());
    }

    @Test
    @DisplayName("Mevcut stoktan fazla satış yapılmak istendiğinde InsufficientStockException fırlatmalı")
    void shouldThrowExceptionWhenStockIsInsufficient() {
        Product oil = new Product(
                "8690002", "Ayçiçek Yağı 2L", Category.BASIC_FOOD,
                new BigDecimal("120.00"), new BigDecimal("160.00"), 4, 5
        );
        service.registerProduct(oil);

        assertThrows(InsufficientStockException.class, () ->
                service.sellProduct("8690002", 5, "Stok üstü deneme")
        );
        assertEquals(4, service.getProductByBarcode("8690002").getStockQuantity());
    }

    @Test
    @DisplayName("SKT süresi dolmuş ürünün satışını ExpiredProductException ile engellemeli")
    void shouldBlockSaleOfExpiredProduct() {
        PerishableProduct expiredMilk = new PerishableProduct(
                "8690003", "Günlük Süt 1L", Category.DAIRY,
                new BigDecimal("25.00"), new BigDecimal("35.00"), 10, 3,
                LocalDate.now().minusDays(1)
        );
        service.registerProduct(expiredMilk);

        assertThrows(ExpiredProductException.class, () ->
                service.sellProduct("8690003", 1, "SKT geçmiş ürün denemesi")
        );
    }

    @Test
    @DisplayName("Maliyet altı kasa indirimini engellemeli, yönetici onayında izin vermeli")
    void shouldGuardAgainstBelowCostDiscountUnlessApproved() {
        Product detergent = new Product(
                "8690004", "Deterjan", Category.CLEANING,
                new BigDecimal("100.00"), new BigDecimal("150.00"), 10, 2
        );
        service.registerProduct(detergent);

        assertThrows(InventoryException.class, () ->
                service.sellProductWithDiscount("8690004", 1, new BigDecimal("40"), false, "Onaysız indirim")
        );

        StockMovement approvedSale = service.sellProductWithDiscount(
                "8690004", 1, new BigDecimal("40"), true, "Yönetici onaylı indirim"
        );
        assertEquals(new BigDecimal("90.00"), approvedSale.unitPrice());
        assertEquals(9, service.getProductByBarcode("8690004").getStockQuantity());
    }

    @Test
    @DisplayName("Kategori bazlı toplu zam sadece ilgili kategorideki ürünleri güncellemeli")
    void shouldApplyBulkPriceIncreaseOnlyToTargetCategory() {
        service.registerProduct(new Product(
                "8690005", "Çay", Category.BASIC_FOOD,
                new BigDecimal("100.00"), new BigDecimal("150.00"), 10, 2
        ));
        service.registerProduct(new Product(
                "8690006", "Kola", Category.BEVERAGE,
                new BigDecimal("30.00"), new BigDecimal("45.00"), 10, 2
        ));

        int updatedCount = service.applyPriceIncreaseToCategory(Category.BASIC_FOOD, new BigDecimal("10"));

        assertEquals(1, updatedCount);
        assertEquals(new BigDecimal("165.00"), service.getProductByBarcode("8690005").getSalePrice());
        assertEquals(new BigDecimal("45.00"), service.getProductByBarcode("8690006").getSalePrice());
    }

    @Test
    @DisplayName("FEFO sıralaması en yakın SKT'yi öne almalı ve sadece kriterdeki ürünlere indirim uygulamalı")
    void shouldSortByFefoAndDiscountOnlyExpiringProducts() {
        PerishableProduct cheese = new PerishableProduct(
                "8690007", "Peynir", Category.DAIRY,
                new BigDecimal("60.00"), new BigDecimal("80.00"), 10, 3,
                LocalDate.now().plusDays(2)
        );
        PerishableProduct yogurt = new PerishableProduct(
                "8690008", "Yoğurt", Category.DAIRY,
                new BigDecimal("30.00"), new BigDecimal("50.00"), 10, 3,
                LocalDate.now().plusDays(1)
        );
        PerishableProduct bread = new PerishableProduct(
                "8690009", "Tost Ekmeği", Category.BAKERY,
                new BigDecimal("20.00"), new BigDecimal("30.00"), 10, 3,
                LocalDate.now().plusDays(5)
        );

        service.registerProduct(cheese);
        service.registerProduct(yogurt);
        service.registerProduct(bread);

        List<PerishableProduct> fefoOrder = service.getAllPerishableProductsByFefo();
        assertEquals("8690008", fefoOrder.get(0).getBarcode());
        assertEquals("8690007", fefoOrder.get(1).getBarcode());

        List<PerishableProduct> discounted = service.applyClearanceDiscountToExpiringProducts(
                2, new BigDecimal("20"), false
        );
        assertEquals(2, discounted.size());
        assertEquals(new BigDecimal("40.00"), service.getProductByBarcode("8690008").getSalePrice());
        assertEquals(new BigDecimal("64.00"), service.getProductByBarcode("8690007").getSalePrice());
        assertEquals(new BigDecimal("30.00"), service.getProductByBarcode("8690009").getSalePrice());
    }

    @Test
    @DisplayName("Fire/zayi kaydı stoğu düşürmeli ve alış maliyeti üzerinden zarar tutarını hesaplamalı")
    void shouldRecordWasteAtPurchaseCost() {
        service.registerProduct(new Product(
                "8690010", "Cam Kavanoz Reçel", Category.BASIC_FOOD,
                new BigDecimal("42.50"), new BigDecimal("65.00"), 10, 3
        ));

        service.recordWaste("8690010", 2, "Kırılan ürün");

        assertEquals(8, service.getProductByBarcode("8690010").getStockQuantity());
        assertEquals(new BigDecimal("85.00"), service.getTotalWasteCost());
    }
}