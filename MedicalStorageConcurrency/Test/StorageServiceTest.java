import med.supply.system.model.*;
import med.supply.system.repository.Repository;
import med.supply.system.service.StorageService;
import med.supply.system.util.*;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;

public class StorageServiceTest {

    public static void main(String[] args) {
        System.out.println("Running StorageService tests...");
        try {
            PathsConfig cfg = new PathsConfig();
            cfg.ensure();

            Repository repo = new Repository();
            LogManager logManager = new LogManager(cfg);
            StorageService service = new StorageService(repo, logManager);

            testAddVehicle(service, repo, cfg);
            testAddChargingStation(service, repo, cfg);
            testUpdateChargingLoad(service, repo, cfg);
            testAddItemToVehicle(service, repo, cfg);   // <-- fixed here
            testInvalidNameThrows(service);

            System.out.println("All StorageService tests finished.");
        } catch (Exception e) {
            System.err.println("StorageService tests failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ---------- TEST 1 ----------
    private static void testAddVehicle(StorageService service, Repository repo, PathsConfig cfg) throws IOException {
        StorageVehicle v = new StorageVehicle("V001", "Van_Beta");
        service.addVehicle(v);

        assert repo.vehicles.containsKey("V001") : "Vehicle not added to repository";
        Path logFile = cfg.logsVehicles.resolve("Van_Beta").resolve(LocalDate.now().toString() + ".log");
        assert Files.exists(logFile) : "Vehicle log not created";
        String logContent = Files.readString(logFile);
        assert logContent.contains("created") : "Vehicle creation log missing";

        System.out.println("Test 1 passed (addVehicle)");
    }

    // ---------- TEST 2 ----------
    private static void testAddChargingStation(StorageService service, Repository repo, PathsConfig cfg) throws IOException {
        ChargingStation s = new ChargingStation("S001", "Station_Alpha");
        service.addChargingStation(s);

        assert repo.stations.containsKey("S001") : "Charging station not added to repository";
        Path logFile = cfg.logsCharging.resolve("Station_Alpha").resolve(LocalDate.now().toString() + ".log");
        assert Files.exists(logFile) : "Charging station log not created";
        String logContent = Files.readString(logFile);
        assert logContent.contains("created") : "Charging creation log missing";

        System.out.println("Test 2 passed  (addChargingStation)");
    }

    // ---------- TEST 3 ----------
    private static void testUpdateChargingLoad(StorageService service, Repository repo, PathsConfig cfg) throws IOException {
        ChargingStation s = new ChargingStation("S002", "Station_LoadTest");
        service.addChargingStation(s);

        service.updateChargingLoad("S002", 75);
        assert repo.stations.get("S002").getCurrentLoadPct() == 75 : "Charging load not updated correctly";

        Path logFile = cfg.logsCharging.resolve("Station_LoadTest").resolve(LocalDate.now().toString() + ".log");
        String logContent = Files.readString(logFile);
        assert logContent.contains("Load set to 75%") : "Charging load update not logged";

        System.out.println("Test 3 passed  (updateChargingLoad)");
    }

    // ---------- TEST 4 ----------
    private static void testAddItemToVehicle(StorageService service, Repository repo, PathsConfig cfg) throws IOException {
        StorageVehicle v = new StorageVehicle("V002", "Van_Items");
        service.addVehicle(v);

        StorageItem item = new StorageItem("SKU123", "Bandages", 5);
        service.addItemToVehicle("V002", item);

        //  Fix: inventory is a Map<String, StorageItem>
        assert !v.getInventory().isEmpty() : "Item not added to vehicle inventory";
        assert v.getInventory().containsKey("SKU123") : "Inventory missing SKU123 key";

        StorageItem stored = v.getInventory().get("SKU123");
        assert stored != null : "Stored item is null";
        assert stored.getSku().equals("SKU123") : "Incorrect SKU in inventory";
        assert stored.getQuantity() == 5 : "Incorrect quantity in inventory";

        Path logFile = cfg.logsVehicles.resolve("Van_Items").resolve(LocalDate.now().toString() + ".log");
        String logContent = Files.readString(logFile);
        assert logContent.contains("Added item SKU123 x5") : "Vehicle item addition log missing or mismatched";

        System.out.println("Test 4 passed  (addItemToVehicle)");
    }

    // ---------- TEST 5 ----------
    private static void testInvalidNameThrows(StorageService service) {
        try {
            StorageVehicle invalid = new StorageVehicle("V003", "###Invalid###");
            service.addVehicle(invalid);
            assert false : "Expected IllegalArgumentException not thrown for invalid name";
        } catch (IllegalArgumentException | IOException e) {
            System.out.println("Test 5 passed (requireValidName throws)");
        }
    }
}
