package com.retail;

import com.retail.inventory.repository.InMemoryInventoryRepository;
import com.retail.inventory.repository.InventoryRepository;
import com.retail.inventory.service.InventoryService;
import com.retail.inventory.ui.ConsoleUI;
import com.retail.inventory.ui.SampleDataLoader;

public class Main {
    public static void main(String[] args) {
        InventoryRepository repository = new InMemoryInventoryRepository();
        InventoryService service = new InventoryService(repository);

        SampleDataLoader.loadDefaultCatalog(service);

        ConsoleUI ui = new ConsoleUI(service);
        ui.start();
    }
}