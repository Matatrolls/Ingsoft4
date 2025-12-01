package co.edu.icesi.mio.model.notifications;

/**
 * Estado de una notificación en el sistema.
 */
public enum NotificationStatus {
    PENDIENTE("Pendiente"),       // Notificación nueva, no vista
    LEIDA("Leída"),               // Notificación vista por el controlador
    ATENDIDA("Atendida");         // Notificación atendida y resuelta

    private final String description;

    NotificationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
