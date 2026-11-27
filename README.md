# Retail Inventory & POS Management System

A layered, domain-driven **Retail Store Inventory, POS, and Expiration (FEFO) Management System** built with **Java 21** and **JUnit 5**. Designed around real-world supermarket and retail store operations, the system enforces strict financial precision, loss-prevention guards, and automated clearance workflows.

---

## Key Features & Retail Domain Logic

* **Financial Precision (`BigDecimal`):** Eliminates floating-point rounding inaccuracies across pricing, percentage markups/markdowns, net revenue, and shrinkage valuation using `BigDecimal` with `RoundingMode.HALF_UP`.
* **FEFO (First Expired, First Out) Tracking:** Uses `Comparable<PerishableProduct>` backed by a `PriorityQueue` (Min-Heap) to surface items closest to expiration and apply automated clearance discounts.
* **Loss-Prevention Guards:** Blocks checkout discounts or permanent shelf markdowns that drop the unit sale price below purchase cost (`allowBelowCost` policy), requiring explicit manager override.
* **Expiration Enforcement:** Prevents selling expired perishable items (`ExpiredProductException`) and routes spoiled/damaged stock to purchase-cost waste (`WASTE`) accounting.
* **Immutable Audit Trail (`record`):** Captures every restock, sale, customer return, and waste event in an immutable Java 21 `StockMovement` record—freezing the unit price at transaction time so future price changes never distort historical revenue.
* **Dynamic Pricing Operations:** Supports single-item price increases, bulk category-wide percentage markups, and instant POS checkout discounts.

---

## Architecture & Data Structures

The application follows a clean **5-layer architecture** with Dependency Inversion between the service and data access layers:

```text
com.retail
├── Main.java                          # Application entry point & dependency wiring
└── inventory
    ├── model                          # Rich domain models & immutable records
    │   ├── Product.java               # Core SKU entity with cost/price & margin logic
    │   ├── PerishableProduct.java     # Subclass with expiration tracking & Comparable (FEFO)
    │   ├── StockMovement.java         # Immutable Java 21 record for audit logs
    │   ├── Category.java              # Retail product categories
    │   └── MovementType.java          # RESTOCK, SALE, RETURN, WASTE
    ├── exception                      # Domain-specific runtime exception hierarchy
    │   ├── InventoryException.java
    │   ├── ProductNotFoundException.java
    │   ├── DuplicateProductException.java
    │   ├── InsufficientStockException.java
    │   └── ExpiredProductException.java
    ├── repository                     # Data access contract & optimized in-memory store
    │   ├── InventoryRepository.java
    │   └── InMemoryInventoryRepository.java  # LinkedHashMap O(1) lookup, PriorityQueue FEFO, ArrayList log
    ├── service                        # Core retail business rules & analytics
    │   └── InventoryService.java
    └── ui                             # Interactive CLI & seed catalog
        ├── ConsoleUI.java
        └── SampleDataLoader.java
```

### Algorithmic Complexity Choices
* **Barcode Lookup (`LinkedHashMap`):** $O(1)$ average time complexity for SKU retrieval while preserving catalog insertion order.
* **Expiration Prioritization (`PriorityQueue`):** Min-Heap ordering based on `LocalDate` expiration dates for rapid FEFO sorting.
* **Transaction Audit Log (`ArrayList`):** $O(1)$ amortized append operations for chronological stock movement history.

---

## Tech Stack

* **Language:** Java 21 (Records, Pattern Matching for `instanceof`, Switch Expressions)
* **Build Tool:** Apache Maven
* **Testing:** JUnit 5 (`junit-jupiter`)

---

## Getting Started

### Prerequisites
* JDK 21 or higher
* Apache Maven 3.8+ (or IntelliJ IDEA)

### Running the Application
1. Clone the repository:
   ```bash
   git clone https://github.com/eminyavuz78/retail-inventory-management.git
   cd retail-inventory-management
   ```
2. Compile and run `com.retail.Main` from your IDE or via Maven:
   ```bash
   mvn compile exec:java -Dexec.mainClass="com.retail.Main"
   ```
   *(The application starts pre-loaded with sample retail products via `SampleDataLoader` for immediate testing.)*

### Running Unit Tests
Execute the JUnit 5 test suite covering stock validation, expiration blocking, loss-prevention guards, category markups, and FEFO clearance filtering:
```bash
mvn test
```