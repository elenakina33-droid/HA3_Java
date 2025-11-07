import med.supply.system.model.ChargingStation;

public class ChargingStationTest {
    public static void main(String[] args) {
        System.out.println("Running ChargingStation tests...");

        // Test 1: valid construction and getters
        try {
            ChargingStation c = new ChargingStation("ST-001", "Station_1");
            assert "1".equals(c.getId()) : "id mismatch";
            assert "Station_1".equals(c.getName()) : "name mismatch";
            assert c.getCurrentLoadPct() == 0 : "default currentLoadPct should be 0";
            System.out.println("Test 1 passed");
        } catch (Throwable t) {
            System.out.println("Test 1 failed: " + t.getMessage());
        }

        //Test 2: set valid load
        try {
            ChargingStation c = new ChargingStation("ST-002", "Station_2");
            c.setCurrentLoadPct(45);
            assert c.getCurrentLoadPct() == 45 : "currentLoadPct not set correctly";
            System.out.println("Test 2 passed");
        } catch (Throwable t) {
            System.out.println("Test 2 failed: " + t.getMessage());
        }

        // Test 3: invalid id should throw exception
        try {
            new ChargingStation("", "Name");
            System.out.println("Test 3 failed: Expected exception for blank id");
        } catch (IllegalArgumentException ok) {
            System.out.println("Test 3 passed (caught: " + ok.getMessage() + ")");
        }

        // Test 4: invalid load should throw exception
        try {
            ChargingStation c = new ChargingStation("ST-004", "Station_4");
            c.setCurrentLoadPct(200);
            System.out.println("Test 4 failed: Expected exception for invalid load");
        } catch (IllegalArgumentException ok) {
            System.out.println("Test 4 passed (caught: " + ok.getMessage() + ")");
        }

        // Test 5: toString includes id, name, and load
        try {
            ChargingStation c = new ChargingStation("ST-005", "Alpha");
            c.setCurrentLoadPct(20);
            String s = c.toString();
            assert s.contains("ST-005") && s.contains("Alpha") && s.contains("20")
                    : "toString missing expected fields";
            System.out.println("Test 5 passed");
        } catch (Throwable t) {
            System.out.println("Test 5 failed: " + t.getMessage());
        }
    }
}
