package com.retail.inventory.ui;

import com.retail.inventory.model.Category;
import com.retail.inventory.model.PerishableProduct;
import com.retail.inventory.model.Product;
import com.retail.inventory.service.InventoryService;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Seeds realistic initial retail store products for immediate testing and demonstration.
 */
public final class SampleDataLoader {

    private SampleDataLoader() {
    }

    public static void loadDefaultCatalog(InventoryService service) {
        service.registerProduct(new Product(
                "8690504010011",
                "Siyah Çay 1000g",
                Category.BASIC_FOOD,
                new BigDecimal("115.00"),
                new BigDecimal("149.90"),
                35,
                10
        ));

        service.registerProduct(new Product(
                "8690504010028",
                "Ayçiçek Yağı 2L",
                Category.BASIC_FOOD,
                new BigDecimal("132.00"),
                new BigDecimal("164.50"),
                9,
                10
        ));

        service.registerProduct(new Product(
                "8690504010035",
                "Kakaolu Bisküvi 3'lü Paket",
                Category.SNACK,
                new BigDecimal("21.50"),
                new BigDecimal("29.90"),
                48,
                15
        ));

        service.registerProduct(new Product(
                "8690504010042",
                "Doğal Maden Suyu 6x200ml",
                Category.BEVERAGE,
                new BigDecimal("28.00"),
                new BigDecimal("37.50"),
                24,
                8
        ));

        service.registerProduct(new Product(
                "8690504010059",
                "Sıvı Bulaşık Deterjanı 750ml",
                Category.CLEANING,
                new BigDecimal("34.00"),
                new BigDecimal("46.00"),
                14,
                5
        ));

        service.registerProduct(new PerishableProduct(
                "8690504020010",
                "Tam Yağlı Süzme Peynir 500g",
                Category.DAIRY,
                new BigDecimal("64.00"),
                new BigDecimal("82.50"),
                16,
                8,
                LocalDate.now().plusDays(2)
        ));

        service.registerProduct(new PerishableProduct(
                "8690504020027",
                "Kaymaklı Yoğurt 1000g",
                Category.DAIRY,
                new BigDecimal("39.00"),
                new BigDecimal("49.90"),
                11,
                6,
                LocalDate.now().plusDays(1)
        ));

        service.registerProduct(new PerishableProduct(
                "8690504020034",
                "Tam Tost Ekmeği 670g",
                Category.BAKERY,
                new BigDecimal("22.00"),
                new BigDecimal("29.50"),
                7,
                5,
                LocalDate.now().plusDays(4)
        ));

        // SKT'si dün geçmiş örnek ürün (satış engeli ve fire/zayi testleri için)
        service.registerProduct(new PerishableProduct(
                "8690504020041",
                "Günlük Pastörize Süt 1L",
                Category.DAIRY,
                new BigDecimal("24.00"),
                new BigDecimal("31.50"),
                4,
                6,
                LocalDate.now().minusDays(1)
        ));
    }
}