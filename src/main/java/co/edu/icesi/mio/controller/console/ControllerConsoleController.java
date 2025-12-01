package co.edu.icesi.mio.controller.console;

import co.edu.icesi.mio.infra.ice.AdminChannelClient;
import co.edu.icesi.mio.model.events.BusEvent;
import co.edu.icesi.mio.model.events.EventType;
import co.edu.icesi.mio.model.notifications.Notification;
import co.edu.icesi.mio.model.notifications.NotificationListener;
import co.edu.icesi.mio.service.analytics.BusStatusService;
import co.edu.icesi.mio.service.notifications.NotificationService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controlador de consola para el rol de Controlador de Operación.
 * Implementa NotificationListener para recibir notificaciones de eventos en tiempo real.
 */
public class ControllerConsoleController implements NotificationListener {

    private final String controllerId;
    private final Scanner scanner;
    private final BusStatusService busStatusService;
    private final AdminChannelClient adminClient;
    private final NotificationService notificationService;
    private final Queue<Notification> pendingNotifications;

    // Polling automático de eventos de Ice
    private Thread pollingThread;
    private volatile boolean polling = true;
    private final Set<String> seenEventIds = ConcurrentHashMap.newKeySet();

    public ControllerConsoleController(String controllerId,
                                      Scanner scanner,
                                      BusStatusService busStatusService,
                                      AdminChannelClient adminClient,
                                      NotificationService notificationService) {
        this.controllerId = controllerId;
        this.scanner = scanner;
        this.busStatusService = busStatusService;
        this.adminClient = adminClient;
        this.notificationService = notificationService;
        this.pendingNotifications = new LinkedList<>();

        // Suscribirse al servicio de notificaciones
        notificationService.subscribe(this);
    }

    @Override
    public void onNotification(Notification notification) {
        // Agregar notificación a la cola de pendientes
        synchronized (pendingNotifications) {
            pendingNotifications.offer(notification);
        }

        // Mostrar alerta visual inmediata
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("NUEVA NOTIFICACIÓN PARA CONTROLADOR " + controllerId);
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println(notification);
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();
    }

    @Override
    public String getListenerId() {
        return controllerId;
    }

    public void run() {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  Bienvenido Controlador " + controllerId);
        System.out.println("║  Sistema de notificaciones activo");
        System.out.println("║  Polling automático de eventos Ice: ACTIVO");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Iniciar polling automático de eventos de Ice
        startEventPolling();

        boolean exit = false;
        while (!exit) {
            showPendingNotificationsAlert();

            System.out.println("════════════════════════════════════");
            System.out.println("   MENÚ CONTROLADOR DE OPERACIÓN");
            System.out.println("════════════════════════════════════");
            System.out.println("1. Ver notificaciones pendientes");
            System.out.println("2. Ver todas las notificaciones recientes");
            System.out.println("3. Ver notificaciones críticas");
            System.out.println("4. Ver eventos de buses");
            System.out.println("5. Ver estado de bus específico");
            System.out.println("6. Ver resumen del sistema");
            System.out.println("0. ← Volver");
            System.out.println("════════════════════════════════════");
            System.out.print("Opción: ");
            String option = scanner.nextLine();

            switch (option) {
                case "1":
                    handlePendingNotifications();
                    break;
                case "2":
                    handleAllNotifications();
                    break;
                case "3":
                    handleCriticalNotifications();
                    break;
                case "4":
                    handleBusEvents();
                    break;
                case "5":
                    handleBusStatus();
                    break;
                case "6":
                    handleSystemSummary();
                    break;
                case "0":
                    exit = true;
                    cleanup();
                    break;
                default:
                    System.out.println("Opción inválida");
            }
        }
    }

    private void showPendingNotificationsAlert() {
        int pending;
        synchronized (pendingNotifications) {
            pending = pendingNotifications.size();
        }
        if (pending > 0) {
            System.out.println("🔔 Tienes " + pending + " notificación(es) pendiente(s)");
            System.out.println();
        }
    }

    private void handlePendingNotifications() {
        System.out.println("\n════════════════════════════════════");
        System.out.println("   NOTIFICACIONES PENDIENTES");
        System.out.println("════════════════════════════════════");

        synchronized (pendingNotifications) {
            if (pendingNotifications.isEmpty()) {
                System.out.println("✓ No hay notificaciones pendientes");
            } else {
                int count = 1;
                while (!pendingNotifications.isEmpty()) {
                    Notification n = pendingNotifications.poll();
                    System.out.println("\n" + count + ". " + n);
                    n.markAsRead();
                    count++;
                }
                System.out.println("\n✓ Todas las notificaciones marcadas como leídas");
            }
        }
        System.out.println("════════════════════════════════════\n");
        pauseForUser();
    }

    private void handleAllNotifications() {
        System.out.println("\n════════════════════════════════════");
        System.out.println("   NOTIFICACIONES RECIENTES");
        System.out.println("════════════════════════════════════");

        List<Notification> notifications = notificationService.getRecentNotifications(20);
        if (notifications.isEmpty()) {
            System.out.println("No hay notificaciones");
        } else {
            for (int i = 0; i < notifications.size(); i++) {
                System.out.println("\n" + (i + 1) + ". " + notifications.get(i));
            }
        }
        System.out.println("\n════════════════════════════════════\n");
        pauseForUser();
    }

    private void handleCriticalNotifications() {
        System.out.println("\n════════════════════════════════════");
        System.out.println("   NOTIFICACIONES CRÍTICAS");
        System.out.println("════════════════════════════════════");

        List<Notification> critical = notificationService.getCriticalNotifications(10);
        if (critical.isEmpty()) {
            System.out.println("✓ No hay notificaciones críticas");
        } else {
            for (int i = 0; i < critical.size(); i++) {
                System.out.println("\n" + (i + 1) + ". " + critical.get(i));
            }
        }
        System.out.println("\n════════════════════════════════════\n");
        pauseForUser();
    }

    private void handleBusEvents() {
        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("    EVENTOS DE BUSES (DESDE SERVIDOR CENTRALIZADO)");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();

        try {
            // Obtener eventos del servidor Ice
            List<co.edu.icesi.mio.infra.ice.BusEventData> events = adminClient.getRecentEvents(20);

            if (events.isEmpty()) {
                System.out.println("✓ No hay eventos recientes en el servidor");
            } else {
                System.out.println("Se encontraron " + events.size() + " evento(s):");
                System.out.println("─".repeat(70));

                for (int i = 0; i < events.size(); i++) {
                    co.edu.icesi.mio.infra.ice.BusEventData event = events.get(i);

                    String priorityIcon = switch (event.getPriority()) {
                        case "CRITICA" -> "🚨";
                        case "ALTA" -> "⚠️";
                        case "MEDIA" -> "🔶";
                        default -> "ℹ️";
                    };

                    System.out.println();
                    System.out.printf("%d. %s [%s] - Prioridad: %s\n",
                            i + 1, priorityIcon, event.getBusId(), event.getPriority());
                    System.out.printf("   Tipo: %s\n", event.getEventType());
                    System.out.printf("   Descripción: %s\n", event.getDescription());
                    System.out.printf("   Hora: %s\n", event.getTimestamp());

                    if (event.getLatitude() != 0.0 || event.getLongitude() != 0.0) {
                        System.out.printf("   GPS: (%.6f, %.6f)\n",
                                event.getLatitude(), event.getLongitude());
                    }
                }

                System.out.println();
                System.out.println("─".repeat(70));
            }

        } catch (Exception e) {
            System.err.println("Error al obtener eventos del servidor:");
            System.err.println("   " + e.getMessage());
            System.err.println();
            System.err.println("   Asegúrese de que el servidor Ice esté en ejecución.");
        }

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();
        pauseForUser();
    }

    private void handleBusStatus() {
        System.out.print("\n🚍 Ingrese el ID del bus: ");
        String code = scanner.nextLine().trim().toUpperCase();
        busStatusService.getRecentEventsForBus(code, 5);
        pauseForUser();
    }

    private void handleSystemSummary() {
        System.out.println("\n════════════════════════════════════");
        System.out.println("   RESUMEN DEL SISTEMA");
        System.out.println("════════════════════════════════════");
        System.out.println("Controladores conectados: " + notificationService.getListenerCount());
        System.out.println("Total notificaciones: " + notificationService.getNotificationCount());
        System.out.println("Notificaciones pendientes: " + notificationService.getPendingNotifications().size());
        System.out.println("════════════════════════════════════\n");
        pauseForUser();
    }

    private void pauseForUser() {
        System.out.print("Presione Enter para continuar...");
        scanner.nextLine();
    }

    private void cleanup() {
        // Detener polling de eventos
        stopEventPolling();

        // Desuscribirse al salir
        notificationService.unsubscribe(this);
        System.out.println("✓ Controlador " + controllerId + " desconectado");
    }

    /**
     * Inicia el thread de polling automático de eventos de Ice.
     * Revisa el servidor cada 5 segundos en busca de nuevos eventos.
     */
    private void startEventPolling() {
        pollingThread = new Thread(() -> {
            System.out.println("Thread de polling automático iniciado");

            while (polling) {
                try {
                    // Esperar 5 segundos entre cada consulta
                    Thread.sleep(5000);

                    // Obtener eventos recientes del servidor Ice
                    List<co.edu.icesi.mio.infra.ice.BusEventData> events = adminClient.getRecentEvents(20);

                    // Procesar solo eventos nuevos
                    for (co.edu.icesi.mio.infra.ice.BusEventData iceEvent : events) {
                        String eventId = iceEvent.getEventId();

                        // Si ya vimos este evento, saltar
                        if (seenEventIds.contains(eventId)) {
                            continue;
                        }

                        // Marcar como visto
                        seenEventIds.add(eventId);

                        // Convertir BusEventData (Ice) a BusEvent (local)
                        BusEvent localEvent = convertIceEventToBusEvent(iceEvent);

                        // Notificar a través del sistema local de notificaciones
                        // Esto disparará el método onNotification() implementado arriba
                        notificationService.notifyEvent(localEvent);
                    }

                } catch (InterruptedException e) {
                    // Thread interrumpido, salir del loop
                    break;
                } catch (Exception e) {
                    // Error al consultar servidor - no mostrar nada para no interrumpir la UI
                    // Solo registrar si es la primera vez
                    if (seenEventIds.isEmpty()) {
                        System.err.println("Polling de eventos: servidor Ice no disponible");
                    }
                }
            }

            System.out.println("Thread de polling automático detenido");
        });

        pollingThread.setDaemon(true); // Thread daemon para que no bloquee el cierre
        pollingThread.setName("IceEventPoller-" + controllerId);
        pollingThread.start();
    }

    /**
     * Detiene el thread de polling automático
     */
    private void stopEventPolling() {
        polling = false;
        if (pollingThread != null && pollingThread.isAlive()) {
            pollingThread.interrupt();
            try {
                pollingThread.join(2000); // Esperar máximo 2 segundos
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Convierte un evento de Ice (BusEventData) a un evento local (BusEvent).
     * Esto permite integrar eventos remotos con el sistema local de notificaciones.
     */
    private BusEvent convertIceEventToBusEvent(co.edu.icesi.mio.infra.ice.BusEventData iceEvent) {
        // Convertir tipo de evento (string) a enum EventType
        EventType eventType;
        try {
            eventType = EventType.valueOf(iceEvent.getEventType());
        } catch (IllegalArgumentException e) {
            // Si no se puede convertir, usar INCIDENTE por defecto
            eventType = EventType.INCIDENTE;
        }

        // Crear evento local con los datos del evento Ice
        BusEvent localEvent = new BusEvent(
                iceEvent.getBusId(),
                eventType,
                iceEvent.getDescription()
        );

        return localEvent;
    }
}
