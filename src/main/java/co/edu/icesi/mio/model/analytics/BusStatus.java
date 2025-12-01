package co.edu.icesi.mio.model.analytics;

import co.edu.icesi.mio.model.events.EventType;

import java.time.Instant;
import java.util.UUID;

public class BusStatus {

    private final UUID busId;
    private final String externalId;
    private final EventType lastEventType;
    private final String lastEventDescription;
    private final Instant lastEventTime;
    private final String statusSummary;


    public BusStatus(UUID busId, String externalId, EventType lastEventType, String lastEventDescription, Instant lastEventTime, String statusSummary) {
        this.busId = busId;
        this.externalId = externalId;
        this.lastEventType = lastEventType;
        this.lastEventDescription = lastEventDescription;
        this.lastEventTime = lastEventTime;
        this.statusSummary = statusSummary;
    }

    public UUID getBusId() {
        return busId;
    }

    public String getExternalId() {
        return externalId;
    }

    public EventType getLastEventType() {
        return lastEventType;
    }

    public String getLastEventDescription() {
        return lastEventDescription;
    }

    public Instant getLastEventTime() {
        return lastEventTime;
    }

    public String getStatusSummary() {
        return statusSummary;
    }

    public boolean isEmergency() {
        return lastEventType == EventType.EMERGENCIA;
    }


}
