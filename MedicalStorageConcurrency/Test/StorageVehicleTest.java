import med.supply.system.model.StorageVehicle;
import med.supply.system.model.StorageItem;

public class StorageVehicleTest {
    public static void main(String[] args) {
        System.out.println("Running StorageVehicle tests...");

        // Test 1: valid creation and battery update
        try {
            StorageVehicle v = new StorageVehicle("VH-001", "Van_Alpha");
            v.setBatteryLevelPct(80);
            assert v.getName().equals("Van_Alpha");
            assert v.getBatteryLevelPct() == 80;
            System.out.println("Test 1 passed");
        } catch (Exception e) {
            System.out.println("Test 1 failed: " + e.getMessage());
        }

        // Test 2: invalid battery level
        try {
            StorageVehicle v2 = new StorageVehicle("VH-002", "Van_Beta");
            v2.setBatteryLevelPct(-5);
            System.out.println("Test 2 failed: Negative battery not handled");
        } catch (IllegalArgumentException e) {
            System.out.println("Test 2 passed (caught exception: " + e.getClass().getSimpleName() + ")");
        }

        // Test 3: adding items to inventory (merge)
        try {
            StorageVehicle v3 = new StorageVehicle("VH-003", "Van_Gamma");
            v3.addItem(new StorageItem("SKU001", "Gloves", 5));
            v3.addItem(new StorageItem("SKU001", "Gloves", 3)); // should merge quantities
            assert v3.getInventory().get("SKU001").getQuantity() == 8;
            System.out.println("Test 3 passed (inventory merge works)");
        } catch (Exception e) {
            System.out.println("Test 3 failed: " + e.getMessage());
        }

        // Test 4: add different SKUs (should not merge)
        try {
            StorageVehicle v4 = new StorageVehicle("VH-004", "Van_Delta");
            v4.addItem(new StorageItem("SKU100", "Masks", 10));
            v4.addItem(new StorageItem("SKU200", "Syringes", 5));

            assert v4.getInventory().get("SKU100").getQuantity() == 10;
            assert v4.getInventory().get("SKU200").getQuantity() == 5;

            System.out.println("Test 4 passed (multiple items handled separately)");
        } catch (Exception e) {
            System.out.println("Test 4 failed: " + e.getMessage());
        }

        // Test 5: battery remains unchanged on invalid update
        try {
            StorageVehicle v5 = new StorageVehicle("VH-005", "Van_Epsilon");
            v5.setBatteryLevelPct(50);
            try {
                v5.setBatteryLevelPct(200); // invalid
                System.out.println("Test 5 failed: Battery > 100% not handled");
            } catch (IllegalArgumentException e) {
                assert v5.getBatteryLevelPct() == 50;
                System.out.println("Test 5 passed (invalid battery does not override value)");
            }
        } catch (Exception e) {
            System.out.println("Test 5 failed: " + e.getMessage());
        }

        // Test 6: Auto-charging triggered at ≤14%
        try {
            StorageVehicle v6 = new StorageVehicle("VH-006", "Van_Zeta");
            v6.setBatteryLevelPct(20);
            v6.setBatteryLevelPct(14);  // should trigger goCharge()
            assert v6.isCharging() : "Vehicle did not enter charging mode";
            v6.setBatteryLevelPct(96);  // should trigger finishCharging()
            assert !v6.isCharging() : "Vehicle did not exit charging mode";
            System.out.println("Test 6 passed (auto-charging logic works)");
        } catch (AssertionError e) {
            System.out.println("Test 6 failed: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Test 6 failed: " + e.getMessage());
        }

        System.out.println("All StorageVehicle tests finished.");
    }
}
