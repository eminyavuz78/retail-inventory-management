package com.retail.inventory.ui;

import com.retail.inventory.exception.InventoryException;
import com.retail.inventory.model.Category;
import com.retail.inventory.model.PerishableProduct;
import com.retail.inventory.model.Product;
import com.retail.inventory.model.StockMovement;
import com.retail.inventory.service.InventoryService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * Interactive command-line user interface for retail store inventory and POS operations.
 */
public class ConsoleUI {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final InventoryService service;
    private final Scanner scanner;

    public ConsoleUI(InventoryService service) {
        this.service = Objects.requireNonNull(service, "InventoryService cannot be null.");
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        printBanner();
        boolean running = true;

        while (running) {
            printMainMenu();
            int choice = readInt("Seçiminiz: ");
            System.out.println();

            try {
                switch (choice) {
                    case 1 -> listAllProducts();
                    case 2 -> searchProducts();
                    case 3 -> registerNewProduct();
                    case 4 -> processStandardSale();
                    case 5 -> processDiscountedSale();
                    case 6 -> processRestock();
                    case 7 -> processCustomerReturn();
                    case 8 -> processWaste();
                    case 9 -> applySingleProductPriceIncrease();
                    case 10 -> applyCategoryPriceIncrease();
                    case 11 -> applySingleProductDiscount();
                    case 12 -> applyExpiringClearanceDiscount();
                    case 13 -> showAlertsAndFefoReport();
                    case 14 -> showFinancialAndMovementReport();
                    case 0 -> {
                        running = false;
                        System.out.println("Sistemden çıkış yapılıyor. İyi çalışmalar!");
                    }
                    default -> System.out.println("[!] Geçersiz seçim. Lütfen menüdeki numaralardan birini girin.");
                }
            } catch (InventoryException | IllegalArgumentException | IllegalStateException e) {
                System.out.println("[HATA] " + e.getMessage());
            }
        }
    }

    private void printBanner() {
        System.out.println("====================================================================");
        System.out.println("       PERAKENDE STOK, KASA VE SKT (FEFO) YÖNETİM SİSTEMİ");
        System.out.println("====================================================================");
    }

    private void printMainMenu() {
        System.out.println("\n---------------------------- ANA MENÜ ------------------------------");
        System.out.println("[ÜRÜN KATALOĞU]   1. Tüm Ürünleri Listele    2. Ürün Ara    3. Yeni Ürün Ekle");
        System.out.println("[KASA & STOK]     4. Kasa Satışı            5. İndirimli Satış");
        System.out.println("                  6. Mal Kabul (Stok Giriş) 7. Müşteri İadesi   8. Fire / Zayi");
        System.out.println("[FİYAT & KAMPANYA]9. Tekli Ürün Zammı       10. Kategori Bazlı Toplu Zam");
        System.out.println("                  11. Tekli Raf İndirimi    12. SKT Yaklaşanlara Otomatik İndirim");
        System.out.println("[RAPOR & ANALİZ]  13. Kritik Stok & SKT (FEFO) Uyarıları");
        System.out.println("                  14. Finansal Özet & Stok Hareket Geçmişi");
        System.out.println("                  0. Çıkış");
        System.out.println("--------------------------------------------------------------------");
    }

    // ==================== 1-3: PRODUCT CATALOG ====================

    private void listAllProducts() {
        List<Product> products = service.getAllProducts();
        if (products.isEmpty()) {
            System.out.println("Katalogda kayıtlı ürün bulunmuyor.");
            return;
        }
        System.out.println("=== GÜNCEL ÜRÜN KATALOĞU (" + products.size() + " Ürün) ===");
        products.forEach(System.out::println);
    }

    private void searchProducts() {
        System.out.print("Barkod veya ürün adı girin: ");
        String query = scanner.nextLine().trim();
        if (query.isEmpty()) {
            System.out.println("[!] Arama terimi boş olamaz.");
            return;
        }

        if (query.chars().allMatch(Character::isDigit)) {
            Product product = service.getProductByBarcode(query);
            System.out.println("Bulunan Ürün: " + product);
        } else {
            List<Product> matches = service.searchProductsByName(query);
            if (matches.isEmpty()) {
                System.out.println("Eşleşen ürün bulunamadı: " + query);
            } else {
                System.out.println("=== ARAMA SONUÇLARI (" + matches.size() + ") ===");
                matches.forEach(System.out::println);
            }
        }
    }

    private void registerNewProduct() {
        System.out.println("=== YENİ ÜRÜN KAYDI ===");
        String barcode = readNonBlankString("Barkod: ");
        String name = readNonBlankString("Ürün Adı: ");
        Category category = readCategory();
        BigDecimal purchasePrice = readPositiveBigDecimal("Alış Fiyatı (TL): ");
        BigDecimal salePrice = readPositiveBigDecimal("Satış Fiyatı (TL): ");

        if (salePrice.compareTo(purchasePrice) < 0) {
            boolean proceed = readYesNo("Uyarı: Satış fiyatı alış maliyetinin altında! Yine de kaydedilsin mi? (E/H): ");
            if (!proceed) {
                System.out.println("Ürün kaydı iptal edildi.");
                return;
            }
        }

        int initialStock = readNonNegativeInt("Açılış Stoğu (Adet): ");
        int criticalThreshold = readNonNegativeInt("Kritik Stok Eşiği (Adet): ");
        boolean isPerishable = readYesNo("Bu ürünün Son Kullanma Tarihi (SKT) takibi var mı? (E/H): ");

        Product product;
        if (isPerishable) {
            LocalDate expirationDate = readDate("Son Kullanma Tarihi (GG.AA.YYYY): ");
            product = new PerishableProduct(
                    barcode, name, category, purchasePrice, salePrice, initialStock, criticalThreshold, expirationDate
            );
        } else {
            product = new Product(
                    barcode, name, category, purchasePrice, salePrice, initialStock, criticalThreshold
            );
        }

        service.registerProduct(product);
        System.out.println("[BAŞARILI] Ürün sisteme kaydedildi: " + product);
    }

    // ==================== 4-8: POS & STOCK MOVEMENTS ====================

    private void processStandardSale() {
        String barcode = readNonBlankString("Satılacak Ürün Barkodu: ");
        int quantity = readPositiveInt("Satış Adedi: ");
        String note = readOptionalString("Kasa Notu (isteğe bağlı): ");

        StockMovement movement = service.sellProduct(barcode, quantity, note);
        System.out.println("[SATIŞ TAMAMLANDI] " + movement);
        warnIfCriticalStock(barcode);
    }

    private void processDiscountedSale() {
        String barcode = readNonBlankString("Satılacak Ürün Barkodu: ");
        int quantity = readPositiveInt("Satış Adedi: ");
        BigDecimal discount = readPositiveBigDecimal("Kasa İndirim Oranı (%): ");
        String note = readOptionalString("Kampanya / İndirim Notu: ");

        try {
            StockMovement movement = service.sellProductWithDiscount(barcode, quantity, discount, false, note);
            System.out.println("[İNDİRİMLİ SATIŞ TAMAMLANDI] " + movement);
            warnIfCriticalStock(barcode);
        } catch (InventoryException ex) {
            if (ex.getMessage().startsWith("Zararına satış engellendi")) {
                System.out.println("[UYARI] " + ex.getMessage());
                if (readYesNo("Yönetici onayıyla zararına satışa izin verilsin mi? (E/H): ")) {
                    StockMovement forcedMovement = service.sellProductWithDiscount(barcode, quantity, discount, true, note);
                    System.out.println("[ONAYLI SATIŞ TAMAMLANDI] " + forcedMovement);
                    warnIfCriticalStock(barcode);
                } else {
                    System.out.println("İşlem iptal edildi.");
                }
            } else {
                throw ex;
            }
        }
    }

    private void processRestock() {
        String barcode = readNonBlankString("Mal Kabul Yapılacak Barkod: ");
        int quantity = readPositiveInt("Gelen Ürün Adedi: ");
        String note = readOptionalString("İrsaliye / Tedarikçi Notu: ");

        StockMovement movement = service.restockProduct(barcode, quantity, note);
        System.out.println("[MAL KABUL KAYDEDİLDİ] " + movement);
    }

    private void processCustomerReturn() {
        String barcode = readNonBlankString("İade Alınan Ürün Barkodu: ");
        int quantity = readPositiveInt("İade Adedi: ");
        String note = readOptionalString("İade Nedeni: ");

        StockMovement movement = service.returnProduct(barcode, quantity, note);
        System.out.println("[MÜŞTERİ İADESİ ALINDI] " + movement);
    }

    private void processWaste() {
        String barcode = readNonBlankString("Fire / Zayi Çıkılacak Barkod: ");
        int quantity = readPositiveInt("Fire Adedi: ");
        String note = readOptionalString("Fire Nedeni (SKT geçmiş, hasarlı vb.): ");

        StockMovement movement = service.recordWaste(barcode, quantity, note);
        System.out.println("[FİRE KAYDI İŞLENDİ] " + movement);
        warnIfCriticalStock(barcode);
    }

    // ==================== 9-12: PRICING & PROMOTIONS ====================

    private void applySingleProductPriceIncrease() {
        String barcode = readNonBlankString("Zam Yapılacak Ürün Barkodu: ");
        BigDecimal percentage = readPositiveBigDecimal("Zam Oranı (%): ");

        Product updated = service.applyPriceIncreaseToProduct(barcode, percentage);
        System.out.println("[FİYAT GÜNCELLENDİ] " + updated);
    }

    private void applyCategoryPriceIncrease() {
        Category category = readCategory();
        BigDecimal percentage = readPositiveBigDecimal("Kategori Geneli Zam Oranı (%): ");

        int count = service.applyPriceIncreaseToCategory(category, percentage);
        System.out.printf("[TOPLU ZAM TAMAMLANDI] %s kategorisindeki %d ürünün fiyatı %%%s artırıldı.%n",
                category.getDisplayName(), count, percentage.stripTrailingZeros().toPlainString());
    }

    private void applySingleProductDiscount() {
        String barcode = readNonBlankString("Raf İndirimi Yapılacak Barkod: ");
        BigDecimal percentage = readPositiveBigDecimal("İndirim Oranı (%): ");

        try {
            Product updated = service.applyDiscountToProduct(barcode, percentage, false);
            System.out.println("[RAF İNDİRİMİ UYGULANDI] " + updated);
        } catch (InventoryException ex) {
            if (ex.getMessage().startsWith("Zararına raf indirimi engellendi")) {
                System.out.println("[UYARI] " + ex.getMessage());
                if (readYesNo("Yönetici onayıyla maliyet altı etikete izin verilsin mi? (E/H): ")) {
                    Product updated = service.applyDiscountToProduct(barcode, percentage, true);
                    System.out.println("[ONAYLI RAF İNDİRİMİ UYGULANDI] " + updated);
                } else {
                    System.out.println("Raf indirimi iptal edildi.");
                }
            } else {
                throw ex;
            }
        }
    }

    private void applyExpiringClearanceDiscount() {
        int days = readNonNegativeInt("SKT'sine kaç gün ve daha az kalan ürünlere indirim yapılsın?: ");
        BigDecimal percentage = readPositiveBigDecimal("Fire Önleyici İndirim Oranı (%): ");
        boolean allowBelowCost = readYesNo("İndirimli fiyat maliyetin altına düşerse yine de uygulansın mı? (E/H): ");

        List<PerishableProduct> discounted = service.applyClearanceDiscountToExpiringProducts(
                days, percentage, allowBelowCost
        );

        if (discounted.isEmpty()) {
            System.out.println("Belirtilen kriterde indirim uygulanabilecek ürün bulunamadı.");
        } else {
            System.out.println("=== SKT ERİTME İNDİRİMİ UYGULANAN ÜRÜNLER (" + discounted.size() + ") ===");
            discounted.forEach(System.out::println);
        }
    }

    // ==================== 13-14: ALERTS & ANALYTICS ====================

    private void showAlertsAndFefoReport() {
        System.out.println("=== KRİTİK STOK UYARILARI ===");
        List<Product> criticalProducts = service.getCriticalStockProducts();
        if (criticalProducts.isEmpty()) {
            System.out.println("Kritik stok seviyesinde ürün yok.");
        } else {
            criticalProducts.forEach(System.out::println);
        }

        System.out.println("\n=== SKT / FEFO ÖNCELİK LİSTESİ (EN YAKIN TARİH EN ÜSTTE) ===");
        List<PerishableProduct> perishables = service.getAllPerishableProductsByFefo();
        if (perishables.isEmpty()) {
            System.out.println("SKT takipli ürün bulunmuyor.");
        } else {
            for (PerishableProduct item : perishables) {
                long remainingDays = item.daysUntilExpiration();
                String status = remainingDays < 0
                        ? "SÜRESİ GEÇMİŞ (" + Math.abs(remainingDays) + " gün önce) -> FİRE ÇIKILMALI"
                        : remainingDays == 0
                        ? "BUGÜN SON GÜN!"
                        : "Kalan: " + remainingDays + " gün";
                System.out.println(item + " | Durum: " + status);
            }
        }
    }

    private void showFinancialAndMovementReport() {
        System.out.println("=== MAĞAZA FİNANSAL & ENVANTER ÖZETİ ===");
        System.out.println("Toplam Depo Maliyet Değeri : " + service.getTotalInventoryCostValue() + " TL");
        System.out.println("Net Satış Cirosu            : " + service.getNetSalesRevenue() + " TL");
        System.out.println("Toplam Fire / Zayi Zararı   : " + service.getTotalWasteCost() + " TL");

        System.out.println("\n=== SON STOK HAREKETLERİ ===");
        List<StockMovement> movements = service.getAllMovements();
        if (movements.isEmpty()) {
            System.out.println("Henüz stok hareketi kaydedilmemiş.");
        } else {
            movements.forEach(System.out::println);
        }
    }

    private void warnIfCriticalStock(String barcode) {
        Product product = service.getProductByBarcode(barcode);
        if (product.isCriticalStock()) {
            System.out.printf("[KRİTİK STOK UYARISI] %s stoğu kritik seviyede! (Mevcut: %d, Eşik: %d)%n",
                    product.getName(), product.getStockQuantity(), product.getCriticalStockThreshold());
        }
    }

    // ==================== SAFE INPUT HELPERS ====================

    private String readNonBlankString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("[!] Bu alan boş bırakılamaz.");
        }
    }

    private String readOptionalString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("[!] Lütfen geçerli bir tam sayı girin.");
            }
        }
    }

    private int readPositiveInt(String prompt) {
        while (true) {
            int value = readInt(prompt);
            if (value > 0) {
                return value;
            }
            System.out.println("[!] Değer 0'dan büyük olmalıdır.");
        }
    }

    private int readNonNegativeInt(String prompt) {
        while (true) {
            int value = readInt(prompt);
            if (value >= 0) {
                return value;
            }
            System.out.println("[!] Değer negatif olamaz.");
        }
    }

    private BigDecimal readPositiveBigDecimal(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().replace(',', '.');
            try {
                BigDecimal value = new BigDecimal(input);
                if (value.compareTo(BigDecimal.ZERO) > 0) {
                    return value;
                }
                System.out.println("[!] Değer 0'dan büyük olmalıdır.");
            } catch (NumberFormatException e) {
                System.out.println("[!] Lütfen geçerli bir sayısal tutar girin (Örn: 49.90).");
            }
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return LocalDate.parse(input, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.println("[!] Geçersiz tarih formatı. Lütfen GG.AA.YYYY biçiminde girin (Örn: 15.12.2026).");
            }
        }
    }

    private Category readCategory() {
        Category[] categories = Category.values();
        System.out.println("Kategoriler:");
        for (int i = 0; i < categories.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, categories[i].getDisplayName());
        }
        while (true) {
            int selection = readInt("Kategori Numarası: ");
            if (selection >= 1 && selection <= categories.length) {
                return categories[selection - 1];
            }
            System.out.println("[!] Lütfen 1 ile " + categories.length + " arasında bir numara seçin.");
        }
    }

    private boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String answer = scanner.nextLine().trim().toUpperCase();
            if (answer.equals("E") || answer.equals("EVET")) {
                return true;
            }
            if (answer.equals("H") || answer.equals("HAYIR")) {
                return false;
            }
            System.out.println("[!] Lütfen E (Evet) veya H (Hayır) girin.");
        }
    }
}