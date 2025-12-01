package co.edu.icesi.mio.model.events;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class BusEvent {

    private final String id;
    private final String busId;
    private final EventType type;
    private final EventCategory category;
    private final EventPriority priority;
    private final String description;
    private final Instant timestamp;

    // Información adicional de contexto
    private int routeId = -1;
    private int lineId = -1;
    private double latitude = 0.0;
    private double longitude = 0.0;

    public BusEvent(String busId, EventType type, String description) {
        this.id = UUID.randomUUID().toString();
        this.busId = busId;
        this.type = type;
        this.category = type.getCategory();
        this.priority = type.getPriority();
        this.description = description;
        this.timestamp = Instant.now();
    }

    public BusEvent(int busId, EventType type, EventCategory category, EventPriority priority,
                    String description, LocalDateTime localTimestamp, double latitude, double longitude) {
        this.id = UUID.randomUUID().toString();
        this.busId = String.valueOf(busId);
        this.type = type;
        this.category = category;
        this.priority = priority;
        this.description = description;
        this.timestamp = localTimestamp.atZone(ZoneId.systemDefault()).toInstant();
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public String getBusId() {
        return busId;
    }

    public EventType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public EventPriority getPriority() {
        return priority;
    }

    public EventCategory getCategory() {
        return category;
    }

    public int getRouteId() {
        return routeId;
    }

    public void setRouteId(int routeId) {
        this.routeId = routeId;
    }

    public int getLineId() {
        return lineId;
    }

    public void setLineId(int lineId) {
        this.lineId = lineId;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean isEmergency() {
        return type == EventType.EMERGENCIA;
    }

    public boolean isCritical() {
        return getPriority() == EventPriority.CRITICA;
    }

    public boolean isHighPriority() {
        return getPriority() == EventPriority.CRITICA || getPriority() == EventPriority.ALTA;
    }

    @Override
    public String toString() {
        return String.format("[%s] [%s] [%s] %s | Bus %s | %s",
                timestamp,
                getPriority().getDescription(),
                getCategory().getDescription(),
                type.getDescription(),
                busId,
                description);
    }
}
