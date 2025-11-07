package med.supply.system.model;

import med.supply.system.util.LogManager;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an automated storage vehicle that can store and transfer items.
 * Includes automatic charging behavior with multiple default charging stations.
 */
public class StorageVehicle implements Runnable {
    private final String id;
    private final String name;
    private int batteryLevelPct = 20;
    private ChargingStation assignedStation;
    private final Map<String, StorageItem> inventory = new HashMap<>();
    private boolean isCharging = false;
    private transient LogManager logger;
    private Thread chargingThread;
    private boolean waitingForCharge = false;
    private boolean leftQueue = false;


    // Maximum capacity of items a vehicle can carry
    private static final int MAX_CAPACITY = 50;  // Maximum number of items a vehicle can hold

    public StorageVehicle(String id, String name) {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("id must not be blank");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name must not be blank");
        this.id = id.trim();
        this.name = name.trim();
    }

    public void attachLogger(LogManager logManager) {
        this.logger = logManager;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getBatteryLevelPct() { return batteryLevelPct; }
    public boolean isCharging() { return isCharging; }
    public ChargingStation getAssignedStation() { return assignedStation; }
    public boolean isWaitingForCharge() { return waitingForCharge; }
    public boolean hasLeftQueue() { return leftQueue; }

    public Map<String, StorageItem> getInventory() {
        return inventory;
    }

    // --------------------------------------------
    // Battery & Charging Logic
    // --------------------------------------------
    public void setBatteryLevelPct(int pct) {
        if (pct < 0 || pct > 100)
            throw new IllegalArgumentException("Battery level must be 0–100");

        this.batteryLevelPct = pct;

        if (pct <= 14 && !isCharging) {
            goCharge();
        } else if (isCharging && pct >= 95) {
            finishCharging();
        }
    }

    private void goCharge() {
        // find first available station
        ChargingStation freeStation = findFreeStation();
        if (freeStation == null) {
            System.out.println("⏳ All charging stations are currently in use. " + name + " will wait...");
            new Thread(this::waitForFreeStation).start();
            return;
        }

        startCharging(freeStation);
    }

    private void startCharging(ChargingStation station) {
        this.assignedStation = station;
        station.occupy();
        isCharging = true;

        String msg = "⚠️ Battery low (" + batteryLevelPct + "%). " + name +
                " going to " + station.getName() + " for charging...";
        System.out.println(msg);
        log(msg);

        chargingThread = new Thread(this);
        chargingThread.start();
    }

    private void waitForFreeStation() {
        long startWait = System.currentTimeMillis();
        waitingForCharge = true;   // mark waiting
        leftQueue = false;

        System.out.println(" " + name + " entered queue, waiting for a free charging station...");
        try {
            while (true) {
                Thread.sleep(5000);
                long waited = System.currentTimeMillis() - startWait;

                // 15-minute timeout
                if (waited > 40_000) {
                    leftQueue = true;
                    waitingForCharge = false;
                    System.out.println("🚫 " + name + " waited more than 15 minutes (" +
                            (waited / 1000 / 60) + " min) and left the queue.");
                    log(name + " left queue after waiting " + (waited / 1000 / 60) + " minutes (timeout).");
                    return;
                }

                ChargingStation freeStation = findFreeStation();
                if (freeStation != null) {
                    waitingForCharge = false;
                    System.out.println(" " + name + " found free " + freeStation.getName() +
                            " after waiting " + (waited / 1000) + " sec and will start charging.");
                    log(name + " started charging after waiting " + (waited / 1000) + " seconds.");
                    startCharging(freeStation);
                    break;
                }
            }
        } catch (InterruptedException e) {
            System.err.println(" " + name + " interrupted while waiting for a free charging station.");
        }
    }



    private ChargingStation findFreeStation() {
        for (ChargingStation s : ChargingStation.DEFAULT_STATIONS) {
            if (!s.isInUse()) {
                return s;
            }
        }
        return null;
    }

    private void finishCharging() {
        isCharging = false;
        if (assignedStation != null) {
            assignedStation.release();
            System.out.println(" " + assignedStation.getName() + " is now FREE.");
        }
        String msg = "🔋 " + name + " fully charged and ready to resume tasks.";
        System.out.println(msg);
        log(msg);
        assignedStation = null;
    }

    @Override
    public void run() {
        try {
            while (isCharging && batteryLevelPct < 100) {
                Thread.sleep(20000); // every 20 seconds increase 5%
                if (!isCharging) break;

                batteryLevelPct = Math.min(100, batteryLevelPct + 5);
                String msg = "🔌 " + name + " charging at " + assignedStation.getName() +
                        "... Battery now " + batteryLevelPct + "%";
                System.out.println(msg);
                log(msg);

                if (batteryLevelPct >= 95) {
                    finishCharging();
                    break;
                }
            }
        } catch (InterruptedException e) {
            System.err.println("Charging interrupted for " + name);
        }
    }

    private void log(String msg) {
        if (logger != null) {
            try {
                logger.logVehicle(name, msg);
            } catch (Exception e) {
                System.err.println("[LOG ERROR] " + e.getMessage());
            }
        }
    }

    // --------------------------------------------
    // Inventory Management
    // --------------------------------------------
    public void addItem(StorageItem item) {
        if (item == null)
            throw new IllegalArgumentException("Item cannot be null");

        // Check if there's enough capacity
        if (hasCapacity()) {
            inventory.merge(item.getSku(), item, (a, b) -> {
                a.setQuantity(a.getQuantity() + b.getQuantity());
                return a;
            });
        } else {
            System.out.println("⚠️ " + name + " has no capacity to add more items.");
        }
    }

    public void setAssignedStation(ChargingStation station) {
        this.assignedStation = station;
        if (isCharging && station != null) {
            // If already charging, occupy now (rare manual switch case)
            station.occupy();
        }
        System.out.println("🚗 " + name + " assigned to " +
                (station != null ? station.getName() : "no station") +
                " (will use it when charging is needed).");
    }

    // --------------------------------------------
    // Capacity Checks
    // --------------------------------------------
    public boolean hasCapacity() {
        return inventory.size() < MAX_CAPACITY;
    }

    public int remainingCapacity() {
        return MAX_CAPACITY - inventory.size();
    }

    @Override
    public String toString() {
        return "StorageVehicle{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", battery=" + batteryLevelPct + "%" +
                (assignedStation != null ? ", station='" + assignedStation.getName() + "'" : "") +
                (isCharging ? ", charging=true" : "") +
                '}';
    }
}
