package med.supply.system.service;

import med.supply.system.model.*;
import med.supply.system.repository.Repository;
import med.supply.system.util.LogManager;
import med.supply.system.util.RegexUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class StorageService {

    private final Repository repo;
    private final LogManager logs;
    private TaskService taskService;

    // All newly created items live here until deliveries are executed
    private final Map<String, StorageItem> unassignedItems = new HashMap<>();

    public StorageService(Repository repo, LogManager logs) {
        this.repo = repo;
        this.logs = logs;

        for (ChargingStation s : ChargingStation.DEFAULT_STATIONS)
            repo.stations.putIfAbsent(s.getId(), s);

        loadDefaultItems();
    }

    public void attachTaskService(TaskService tasks) {
        this.taskService = tasks;
    }

    private void loadDefaultItems() {
        String file = "resources/default_items.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            br.readLine(); // header
            String line;

            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length != 3) continue;

                String sku = p[0].trim();
                String name = p[1].trim();
                int qty = Integer.parseInt(p[2].trim());

                StorageItem it = new StorageItem(sku, name, qty);
                unassignedItems.put(sku, it);
            }

            System.out.println("Default medical items loaded.");

        } catch (Exception e) {
            System.err.println("Error loading CSV: " + e.getMessage());
        }
    }

    public Map<String, StorageItem> getUnassignedItemsRef() {
        return unassignedItems;
    }

    public LogManager getLogManager() {
        return logs;
    }

    // ------------------------------------------------------------
    // Vehicles
    // ------------------------------------------------------------
    public void addVehicle(StorageVehicle v) throws IOException {
        requireValidName(v.getName(), "vehicle");

        v.attachLogger(logs);
        repo.vehicles.put(v.getId(), v);

        logs.logSystem("Vehicle added: " + v);
        logs.logVehicle(v.getName(), "created");

        // If auto-distribute was started earlier, resume when a free AGV appears
        if (taskService != null && v.getInventory().isEmpty() && !v.isCharging()) {
            taskService.tryResumeAutoDistributeAsync();
        }
    }

    private void requireValidName(String name, String kind) {
        if (!RegexUtils.isValidEquipment(name)) {
            throw new IllegalArgumentException(kind + " name must be 2–40 characters.");
        }
    }

    // ------------------------------------------------------------
    // Charging Stations
    // ------------------------------------------------------------
    public void addChargingStation(ChargingStation s) throws IOException {
        requireValidName(s.getName(), "station");
        repo.stations.put(s.getId(), s);
        logs.logSystem("Charging station added: " + s);
    }

    public void updateChargingLoad(String stationId, int status) throws IOException {
        ChargingStation s = repo.stations.get(stationId);
        if (s == null)
            throw new IllegalArgumentException("Station not found: " + stationId);

        if (status == 1) s.occupy();
        else s.release();
    }

    // ------------------------------------------------------------
    // Items (NO vehicleId anywhere)
    // ------------------------------------------------------------
    public void addItem(StorageItem item) throws IOException {
        if (item == null) throw new IllegalArgumentException("Item cannot be null");
        unassignedItems.put(item.getSku(), item);
        logs.logSystem("Added unassigned item: " + item.getSku() + " (" + item.getName() + "), qty=" + item.getQuantity());
    }

    // ------------------------------------------------------------
    // Listing
    // ------------------------------------------------------------
    public void listAllItems() {
        System.out.println("\n===== ALL STORAGE ITEMS =====");

        System.out.println("\nAssigned Items:");
        boolean found = false;
        for (StorageVehicle v : repo.vehicles.values()) {
            for (StorageItem it : v.getInventory().values()) {
                System.out.println("  • " + it + " → Vehicle: " + v.getName());
                found = true;
            }
        }
        if (!found) System.out.println("  (none)");

        System.out.println("\nUnassigned Items:");
        if (unassignedItems.isEmpty())
            System.out.println("  (none)");
        else
            unassignedItems.values().forEach(it -> System.out.println("  • " + it));

        System.out.println("================================\n");
    }
}
