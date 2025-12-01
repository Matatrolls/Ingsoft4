package co.edu.icesi.mio.model.core;

public class Arc {

    private final String id;
    private final String fromStopId;
    private final String toStopId;
    private final double averageTravelTimeMinutes;
    private final String zoneId;

    public Arc(String id, String fromStopId, String toStopId, double averageTravelTimeMinutes, String zoneId) {
        this.id = id;
        this.fromStopId = fromStopId;
        this.toStopId = toStopId;
        this.averageTravelTimeMinutes = averageTravelTimeMinutes;
        this.zoneId = zoneId;
    }

    public String getId() {
        return id;
    }

    public String getFromStopId() {
        return fromStopId;
    }

    public String getToStopId() {
        return toStopId;
    }

    public double getAverageTravelTimeMinutes() {
        return averageTravelTimeMinutes;
    }

    public String getZoneId() {
        return zoneId;
    }
}
