package med.supply.system.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a charging station used by storage vehicles.
 * Tracks whether the station is free or currently in use.
 */
public class ChargingStation {

    private final String id;
    private final String name;
    private boolean inUse; // true = vehicle is charging, false = free




    public static final List<ChargingStation> DEFAULT_STATIONS = new CopyOnWriteArrayList<>(List.of(
            new ChargingStation("CHG-DEFAULT-1", "Default_Station_1"),
            new ChargingStation("CHG-DEFAULT-2", "Default_Station_2"),
            new ChargingStation("CHG-DEFAULT-3", "Default_Station_3")
    ));

    public ChargingStation(String id, String name) {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("id must not be blank");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name must not be blank");

        this.id = id.trim();
        this.name = name.trim();
        this.inUse = false;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isInUse() { return inUse; }

    public synchronized void occupy() { this.inUse = true; }
    public synchronized void release() { this.inUse = false; }

    @Override
    public String toString() {
        return "ChargingStation{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", status=" + (inUse ? "IN_USE" : "FREE") +
                "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChargingStation)) return false;
        ChargingStation that = (ChargingStation) o;
        return id.equalsIgnoreCase(that.id);
    }

    @Override
    public int hashCode() {
        return id.toLowerCase().hashCode();
    }
}
