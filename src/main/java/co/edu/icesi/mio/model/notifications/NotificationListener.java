package co.edu.icesi.mio.model.notifications;

/**
 * Observer para el patrón Observer de notificaciones.
 * Los controladores implementan esta interfaz para recibir notificaciones de eventos.
 */
public interface NotificationListener {

    /**
     * Método llamado cuando se genera una nueva notificación.
     * @param notification La notificación generada
     */
    void onNotification(Notification notification);

    /**
     * Identificador único del listener (ej: ID del controlador)
     */
    String getListenerId();
}
