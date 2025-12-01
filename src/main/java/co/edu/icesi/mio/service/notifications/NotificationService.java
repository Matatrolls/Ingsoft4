package co.edu.icesi.mio.service.notifications;

import co.edu.icesi.mio.concurrency.ConcurrencyManager;
import co.edu.icesi.mio.model.events.BusEvent;
import co.edu.icesi.mio.model.events.EventPriority;
import co.edu.icesi.mio.model.notifications.Notification;
import co.edu.icesi.mio.model.notifications.NotificationListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servicio de notificaciones que implementa el patrón Observer.
 * Gestiona la suscripción de controladores y el envío de notificaciones basadas en eventos.
 */
public class NotificationService {

    private final ConcurrencyManager concurrencyManager;
    private final List<NotificationListener> listeners;
    private final List<Notification> notifications;
    private final int maxNotifications;

    public NotificationService(ConcurrencyManager concurrencyManager) {
        this(concurrencyManager, 1000);
    }

    public NotificationService(ConcurrencyManager concurrencyManager, int maxNotifications) {
        this.concurrencyManager = concurrencyManager;
        this.listeners = new CopyOnWriteArrayList<>();  // Thread-safe para añadir/remover listeners
        this.notifications = Collections.synchronizedList(new ArrayList<>());
        this.maxNotifications = maxNotifications;
    }

    /**
     * Suscribe un listener para recibir notificaciones
     */
    public void subscribe(NotificationListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            System.out.println("✓ Controlador " + listener.getListenerId() + " suscrito a notificaciones");
        }
    }

    /**
     * Desuscribe un listener
     */
    public void unsubscribe(NotificationListener listener) {
        if (listeners.remove(listener)) {
            System.out.println("✓ Controlador " + listener.getListenerId() + " desuscrito de notificaciones");
        }
    }

    /**
     * Notifica a todos los listeners sobre un evento de bus.
     * Solo se notifican eventos de prioridad ALTA o CRITICA.
     */
    public void notifyEvent(BusEvent event) {
        // Solo notificamos eventos de alta prioridad
        if (event.isHighPriority()) {
            String message = buildNotificationMessage(event);
            Notification notification = new Notification(event, message);

            // Almacenamos la notificación
            storeNotification(notification);

            // Notificamos a todos los listeners de forma asíncrona
            for (NotificationListener listener : listeners) {
                concurrencyManager.run(() -> {
                    try {
                        listener.onNotification(notification);
                    } catch (Exception e) {
                        System.err.println("Error notificando a listener " + listener.getListenerId() + ": " + e.getMessage());
                    }
                });
            }
        }
    }

    /**
     * Almacena la notificación con límite de tamaño
     */
    private void storeNotification(Notification notification) {
        synchronized (notifications) {
            notifications.add(notification);
            if (notifications.size() > maxNotifications) {
                int toRemove = notifications.size() - maxNotifications;
                notifications.subList(0, toRemove).clear();
            }
        }
    }

    /**
     * Construye el mensaje de notificación basado en el evento
     */
    private String buildNotificationMessage(BusEvent event) {
        StringBuilder msg = new StringBuilder();
        msg.append("⚠️ ALERTA: ");

        if (event.isCritical()) {
            msg.append("CRÍTICA - ");
        }

        msg.append(event.getType().getDescription());
        msg.append(" en bus ").append(event.getBusId());

        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            msg.append(" - ").append(event.getDescription());
        }

        return msg.toString();
    }

    /**
     * Obtiene las últimas N notificaciones
     */
    public List<Notification> getRecentNotifications(int limit) {
        synchronized (notifications) {
            int size = notifications.size();
            if (size == 0) {
                return Collections.emptyList();
            }
            int from = Math.max(0, size - limit);
            return new ArrayList<>(notifications.subList(from, size));
        }
    }

    /**
     * Obtiene las notificaciones pendientes (no leídas)
     */
    public List<Notification> getPendingNotifications() {
        synchronized (notifications) {
            return notifications.stream()
                    .filter(n -> n.getStatus() == co.edu.icesi.mio.model.notifications.NotificationStatus.PENDIENTE)
                    .toList();
        }
    }

    /**
     * Obtiene solo notificaciones críticas
     */
    public List<Notification> getCriticalNotifications(int limit) {
        synchronized (notifications) {
            List<Notification> critical = notifications.stream()
                    .filter(Notification::isCritical)
                    .toList();
            int size = critical.size();
            if (size == 0) {
                return Collections.emptyList();
            }
            int from = Math.max(0, size - limit);
            return new ArrayList<>(critical.subList(from, size));
        }
    }

    /**
     * Obtiene el número de listeners activos
     */
    public int getListenerCount() {
        return listeners.size();
    }

    /**
     * Obtiene el total de notificaciones almacenadas
     */
    public int getNotificationCount() {
        synchronized (notifications) {
            return notifications.size();
        }
    }
}
