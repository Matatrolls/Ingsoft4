package co.edu.icesi.mio.model.notifications;

import co.edu.icesi.mio.model.events.BusEvent;
import co.edu.icesi.mio.model.events.EventPriority;

import java.time.Instant;
import java.util.UUID;

/**
 * Notificación generada a partir de un evento de bus.
 * Las notificaciones son enviadas a los controladores para alertarlos de eventos importantes.
 */
public class Notification {

    private final String id;
    private final BusEvent event;
    private final String message;
    private final Instant createdAt;
    private NotificationStatus status;

    public Notification(BusEvent event, String message) {
        this.id = UUID.randomUUID().toString();
        this.event = event;
        this.message = message;
        this.createdAt = Instant.now();
        this.status = NotificationStatus.PENDIENTE;
    }

    public String getId() {
        return id;
    }

    public BusEvent getEvent() {
        return event;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void markAsRead() {
        this.status = NotificationStatus.LEIDA;
    }

    public void markAsAcknowledged() {
        this.status = NotificationStatus.ATENDIDA;
    }

    public EventPriority getPriority() {
        return event.getPriority();
    }

    public boolean isCritical() {
        return event.isCritical();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s (Estado: %s)",
                getPriority().getDescription(),
                event.getBusId(),
                message,
                status);
    }
}
