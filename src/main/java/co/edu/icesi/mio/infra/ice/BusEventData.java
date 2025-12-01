package co.edu.icesi.mio.infra.ice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Representa un evento de bus para comunicación Ice entre clientes.
 * Estructura simple para serialización manual.
 */
public class BusEventData {
    private String eventId;
    private String busId;
    private String eventType;
    private String priority;
    private String description;
    private String timestamp;
    private double latitude;
    private double longitude;

    public BusEventData() {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public BusEventData(String eventId, String busId, String eventType, String priority,
                        String description, double latitude, double longitude) {
        this.eventId = eventId;
        this.busId = busId;
        this.eventType = eventType;
        this.priority = priority;
        this.description = description;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getBusId() {
        return busId;
    }

    public void setBusId(String busId) {
        this.busId = busId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return String.format("BusEvent[id=%s, bus=%s, type=%s, priority=%s, desc=%s, time=%s]",
                eventId, busId, eventType, priority, description, timestamp);
    }
}
