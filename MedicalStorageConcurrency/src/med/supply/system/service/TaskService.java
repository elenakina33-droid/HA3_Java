package med.supply.system.service;

import med.supply.system.exception.ExceptionHandler;
import med.supply.system.model.*;
import med.supply.system.repository.Repository;
import med.supply.system.util.LogManager;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaskService {

    private final Repository repo;
    private final LogManager logs;

    private StorageService storage;
    private DestinationService destination;

    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    private int nextTaskNumber = 1;

    private final AtomicBoolean autoResumeEnabled = new AtomicBoolean(false);
    private final AtomicBoolean resumeRunning = new AtomicBoolean(false);

    public TaskService(Repository repo, LogManager logs) {
        this.repo = repo;
        this.logs = logs;
    }

    public void attachStorage(StorageService storage) {
        this.storage = storage;
    }

    public void attachDestination(DestinationService dst) {
        this.destination = dst;
    }

    // ============================================================
    // Task Creation
    // ============================================================
    public void createTask(Task t) throws IOException {
        if (t.id == null || t.id.isBlank())
            throw new IllegalArgumentException("Task ID cannot be empty");


        if (t.assigneeVehicleId != null && !repo.vehicles.containsKey(t.assigneeVehicleId)) {
            throw new IllegalArgumentException("Vehicle with ID '" + t.assigneeVehicleId + "' does not exist.");
        }


        repo.tasks.put(t.id, t);
        logs.logSystem("Task created: " + t);


        if (t.assigneeVehicleId != null && storage != null) {
            StorageVehicle v = repo.vehicles.get(t.assigneeVehicleId);
            if (v != null) {
                Map<String, StorageItem> unassigned = storage.getUnassignedItemsRef();
                if (!unassigned.isEmpty()) {
                    StorageItem first = unassigned.values().iterator().next();
                    unassigned.remove(first.getSku());
                    v.addItem(first);
                    logs.logVehicle(v.getName(),
                            "Assigned item '" + first.getName() +
                                    "' from unassigned pool when creating manual task " + t.id);
                }
            }
        }

        if (t.assigneeVehicleId != null) {
            StorageVehicle v = repo.vehicles.get(t.assigneeVehicleId);
            if (v != null)
                logs.logVehicle(v.getName(), "Assigned task " + t.id);
        }
    }

    // ============================================================
    // Task Status Updates (battery drop + manual delivery sim)
    // ============================================================
    public void updateStatus(String taskId, TaskStatus status) throws Exception {
        Task t = repo.tasks.get(taskId);
        if (t == null) {
            ExceptionHandler.handleTaskNotFound(taskId,
                    new IllegalArgumentException("Task not found."));
            return;
        }

        t.status = status;
        logs.logSystem("Task " + taskId + " -> " + status);

        if (t.assigneeVehicleId != null) {
            StorageVehicle v = repo.vehicles.get(t.assigneeVehicleId);
            if (v != null) {
                int before = v.getBatteryLevelPct();
                int after = Math.max(0, before - 5);
                v.setBatteryLevelPct(after);
                logs.logVehicle(v.getName(),
                        "Battery drop after update: " + before + "% → " + after + "%");


                if (status == TaskStatus.IN_PROGRESS) {
                    executor.submit(() -> {
                        try {
                            System.out.println("[TASK " + taskId + "] Running 1-minute delivery for vehicle " + v.getName());
                            Thread.sleep(60_000); // simulate delivery time


                            StorageItem assignedItem = v.getInventory().values().stream()
                                    .filter(it -> it.getQuantity() > 0)
                                    .findFirst()
                                    .orElse(null);

                            if (assignedItem != null) {
                                int deliverQty = Math.min(assignedItem.getQuantity(), 50); // batch max 50
                                String itemName = assignedItem.getName();


                                if (destination != null)
                                    destination.recordDelivery(v.getName(), itemName, deliverQty);


                                t.status = TaskStatus.DONE;
                                logs.logSystem("[TASK " + taskId + "] Delivered " + deliverQty + " of " + itemName + " from vehicle " + v.getName());
                                System.out.println("[TASK " + taskId + "] Delivered " + deliverQty + " of " + itemName + " from vehicle " + v.getName());


                                assignedItem.setQuantity(assignedItem.getQuantity() - deliverQty);
                                if (assignedItem.getQuantity() <= 0) {
                                    v.getInventory().remove(assignedItem.getSku());
                                    logs.logVehicle(v.getName(), "Item " + itemName + " fully delivered and removed from inventory.");
                                }
                            } else {
                                System.out.println("[TASK " + taskId + "] No items found in vehicle " + v.getName() + " to deliver.");
                            }

                        } catch (InterruptedException e) {
                            System.err.println("Interrupted during delivery for " + v.getName());
                            Thread.currentThread().interrupt();
                        } catch (IOException e) {
                            System.err.println("Error writing to destination file: " + e.getMessage());
                        }
                    });
                }

            } else {
                throw new IllegalArgumentException("Vehicle with ID '" + t.assigneeVehicleId + "' does not exist.");
            }
        }
    }

    // ============================================================
    // AUTO-DISTRIBUTE (unchanged)
    // ============================================================
    public void autoDistribute(String masterTaskId) {
        autoResumeEnabled.set(true);

        if (storage == null) {
            System.out.println("[AUTO " + masterTaskId + "] ERROR: StorageService missing.");
            return;
        }
        if (destination == null) {
            System.out.println("[AUTO " + masterTaskId + "] ERROR: DestinationService missing.");
            return;
        }

        Map<String, StorageItem> unassigned = storage.getUnassignedItemsRef();
        if (unassigned.isEmpty()) {
            System.out.println("[AUTO " + masterTaskId + "] No unassigned items.");
            return;
        }

        List<StorageVehicle> freeAgvs = new ArrayList<>();
        for (StorageVehicle v : repo.vehicles.values()) {
            if (v.getInventory().isEmpty() && !v.isCharging()
                    && !v.isWaitingForCharge() && !v.hasLeftQueue()) {
                freeAgvs.add(v);
            }

        }
        if (freeAgvs.isEmpty()) {
            System.out.println("[AUTO " + masterTaskId + "] No free AGVs.");
            return;
        }

        System.out.println("[AUTO-DIST " + masterTaskId + "] Starting distribution...");
        Iterator<StorageVehicle> agvQueue = freeAgvs.iterator();
        Map<String, Integer> leftovers = new LinkedHashMap<>();
        List<StorageItem> items = new ArrayList<>(unassigned.values());

        for (StorageItem item : items) {
            int remaining = item.getQuantity();
            if (remaining <= 0) continue;

            while (remaining > 0 && agvQueue.hasNext()) {
                StorageVehicle agv = agvQueue.next();
                int batch = Math.min(50, remaining);
                int qtyToSend = batch;

                String tid = "Task " + (nextTaskNumber++);
                Task nt = new Task(tid, "Deliver " + qtyToSend + " " + item.getName(), agv.getId());
                repo.tasks.put(tid, nt);

                String sku = item.getSku();
                String itemName = item.getName();
                executor.submit(() -> simulateDelivery(tid, agv, sku, itemName, qtyToSend));

                remaining -= batch;
                item.setQuantity(remaining);
            }

            if (remaining > 0) leftovers.put(item.getName(), remaining);
            if (!agvQueue.hasNext()) break;
        }

        unassigned.entrySet().removeIf(e -> e.getValue().getQuantity() <= 0);
        for (var e : leftovers.entrySet())
            System.out.println("WARNING: " + e.getValue() + " " + e.getKey() + " still unassigned. Add more free AGVs.");

        System.out.println("[AUTO-DIST " + masterTaskId + "]Completed.");
    }

    private void simulateDelivery(String taskId, StorageVehicle agv,
                                  String sku, String itemName, int qty) {
        System.out.println("[TASK " + taskId + "] Assigned to AGV " + agv.getName());
        System.out.println("[TASK " + taskId + "] AGV " + agv.getName() +
                " transporting " + qty + " " + itemName + " (~1 minute)");
        try {
            Thread.sleep(60_000);


            destination.recordDelivery(agv.getName(), itemName, qty);
            System.out.println("[TASK " + taskId + "]Delivered " + qty + " " + itemName);


            int before = agv.getBatteryLevelPct();
            int after = Math.max(0, before - 10);
            agv.setBatteryLevelPct(after);  // ⚙️ triggers auto-charging if ≤ 14
            logs.logVehicle(agv.getName(),
                    "Battery drop after auto-distribution: " + before + "% → " + after + "%");


            Task t = repo.tasks.get(taskId);
            if (t != null) t.status = TaskStatus.DONE;

            tryResumeAutoDistributeAsync();

        } catch (InterruptedException ignored) {}
        catch (IOException e) {
            System.err.println("Error writing to destination file: " + e.getMessage());
        }
    }


    public void tryResumeAutoDistributeAsync() {
        if (!autoResumeEnabled.get()) return;
        if (!resumeRunning.compareAndSet(false, true)) return;
        executor.submit(() -> {
            try {
                Map<String, StorageItem> unassigned = storage.getUnassignedItemsRef();
                if (unassigned.isEmpty()) return;
                boolean hasFree = repo.vehicles.values().stream()
                        .anyMatch(v -> v.getInventory().isEmpty() && !v.isCharging());
                if (hasFree) {
                    String id = "RESUME-" + System.currentTimeMillis();
                    System.out.println("[AUTO-RESUME] Free AGV detected → continuing distribution...");
                    autoDistribute(id);
                }
            } finally {
                resumeRunning.set(false);
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
