package co.edu.icesi.mio.app;

import co.edu.icesi.mio.concurrency.ConcurrencyManager;
import co.edu.icesi.mio.model.events.BusEventStore;
import co.edu.icesi.mio.model.notifications.Notification;
import co.edu.icesi.mio.model.notifications.NotificationListener;
import co.edu.icesi.mio.model.realtime.BusPosition;
import co.edu.icesi.mio.service.notifications.NotificationService;
import co.edu.icesi.mio.service.streaming.RealtimeStreamingService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Aplicación de prueba para el sistema de streaming en tiempo real.
 * Simula el procesamiento de datagramas en tiempo real desde el archivo de streaming.
 */
public class TestRealtimeStreaming {

    private static final String STREAMING_FILE = "src/main/resources/data/datagrams4streaming.csv";

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                ║");
        System.out.println("║        TEST: STREAMING EN TIEMPO REAL (BONUS)                 ║");
        System.out.println("║        Sistema de Gestión del MIO - Cali                      ║");
        System.out.println("║                                                                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Verificar que el archivo existe
        Path streamingPath = Path.of(STREAMING_FILE);
        if (!Files.exists(streamingPath)) {
            System.err.println("✗ Error: Archivo de streaming no encontrado:");
            System.err.println("  " + STREAMING_FILE);
            System.err.println();
            System.err.println("  Por favor asegúrese de que el archivo existe en:");
            System.err.println("  src/main/resources/data/datagrams4streaming.csv");
            return;
        }

        try {
            System.out.printf("✓ Archivo encontrado: %s\n", STREAMING_FILE);
            System.out.printf("  Tamaño: %.2f MB\n", Files.size(streamingPath) / (1024.0 * 1024.0));
            System.out.println();
        } catch (Exception e) {
            System.err.println("Error verificando archivo: " + e.getMessage());
            return;
        }

        // 1. Inicializar componentes
        System.out.println("Inicializando componentes...");
        ConcurrencyManager concurrencyManager = new ConcurrencyManager(4, 2);
        BusEventStore eventStore = new BusEventStore();
        NotificationService notificationService = new NotificationService(concurrencyManager);
        System.out.println("✓ Componentes inicializados\n");

        // 2. Suscribir un listener de prueba para mostrar notificaciones
        TestNotificationListener listener = new TestNotificationListener();
        notificationService.subscribe(listener);
        System.out.println("✓ Listener de notificaciones suscrito\n");

        // 3. Crear servicio de streaming
        // Nota: El servicio procesa máximo 15,000 datagramas (límite por defecto para evitar sobrecarga)
        RealtimeStreamingService streamingService = new RealtimeStreamingService(
                STREAMING_FILE,
                eventStore,
                notificationService
        );

        // Configuración del streaming
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  CONFIGURACIÓN DEL STREAMING");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Seleccione el modo de operación:");
        System.out.println();
        System.out.println("1. Modo Rápido    - Sin simulación temporal (más rápido)");
        System.out.println("2. Modo Normal    - Simulación 10x más rápido que tiempo real");
        System.out.println("3. Modo Lento     - Simulación 5x más rápido que tiempo real");
        System.out.println("4. Modo Muy Lento - Simulación 2x más rápido que tiempo real");
        System.out.println("5. Modo Muestra   - Solo primeros 10,000 datagramas");
        System.out.println();
        System.out.print("Opción [2]: ");

        Scanner scanner = new Scanner(System.in);
        String choice = scanner.nextLine().trim();
        if (choice.isEmpty()) {
            choice = "2";
        }

        switch (choice) {
            case "1" -> {
                streamingService.setRealTimeSimulation(false);
                streamingService.setTimeAccelerationFactor(1.0);
                System.out.println("\n✓ Modo: RÁPIDO (sin delays)");
            }
            case "2" -> {
                streamingService.setRealTimeSimulation(true);
                streamingService.setTimeAccelerationFactor(10.0);
                System.out.println("\n✓ Modo: NORMAL (10x acelerado)");
            }
            case "3" -> {
                streamingService.setRealTimeSimulation(true);
                streamingService.setTimeAccelerationFactor(5.0);
                System.out.println("\n✓ Modo: LENTO (5x acelerado)");
            }
            case "4" -> {
                streamingService.setRealTimeSimulation(true);
                streamingService.setTimeAccelerationFactor(2.0);
                System.out.println("\n✓ Modo: MUY LENTO (2x acelerado)");
            }
            case "5" -> {
                streamingService.setRealTimeSimulation(false);
                streamingService.setTimeAccelerationFactor(1.0);
                streamingService.setQueueSize(10000);
                System.out.println("\n✓ Modo: MUESTRA (10,000 datagramas)");
            }
            default -> {
                streamingService.setRealTimeSimulation(true);
                streamingService.setTimeAccelerationFactor(10.0);
                System.out.println("\n✓ Modo: NORMAL (10x acelerado) [por defecto]");
            }
        }

        System.out.println();
        System.out.println("Presione ENTER para iniciar el streaming...");
        scanner.nextLine();
        System.out.println();

        // 4. Iniciar streaming
        long startTime = System.currentTimeMillis();
        streamingService.start();

        // 5. Monitorear progreso
        Thread monitorThread = new Thread(() -> monitorProgress(streamingService, eventStore, listener));
        monitorThread.setDaemon(true);
        monitorThread.start();

        // 6. Esperar a que termine o que el usuario presione ENTER
        System.out.println();
        System.out.println("El streaming está en ejecución...");
        System.out.println("Presione ENTER para detener y ver resultados");
        System.out.println();
        scanner.nextLine();

        // 7. Detener
        streamingService.stop();
        concurrencyManager.shutdown();

        long endTime = System.currentTimeMillis();
        double totalSeconds = (endTime - startTime) / 1000.0;

        // 8. Mostrar resultados finales
        showFinalResults(streamingService, eventStore, listener, totalSeconds);

        System.out.println();
        System.out.println("✓ Prueba completada");
    }

    /**
     * Monitorea el progreso del streaming cada 5 segundos
     */
    private static void monitorProgress(RealtimeStreamingService streamingService,
                                       BusEventStore eventStore,
                                       TestNotificationListener listener) {
        try {
            while (streamingService.isRunning()) {
                Thread.sleep(5000);

                RealtimeStreamingService.ServiceStats stats = streamingService.getStats();
                System.out.println();
                System.out.println("─────────────────────────────────────────────────────");
                System.out.printf("  Datagramas procesados: %,d\n", stats.getDatagramsProcessed());
                System.out.printf("  Eventos generados:     %,d\n", stats.getEventsGenerated());
                System.out.printf("  Buses rastreados:      %,d\n", stats.getBusesTracked());
                System.out.printf("  Notificaciones:        %,d\n", listener.getNotificationCount());
                System.out.println("─────────────────────────────────────────────────────");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Muestra los resultados finales del streaming
     */
    private static void showFinalResults(RealtimeStreamingService streamingService,
                                        BusEventStore eventStore,
                                        TestNotificationListener listener,
                                        double totalSeconds) {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    RESULTADOS FINALES                          ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Estadísticas generales
        RealtimeStreamingService.ServiceStats stats = streamingService.getStats();
        System.out.println("═══ ESTADÍSTICAS GENERALES ═══");
        System.out.printf("Datagramas leídos:           %,d\n", stats.getDatagramsRead());
        System.out.printf("Datagramas procesados:       %,d\n", stats.getDatagramsProcessed());
        System.out.printf("Eventos generados:           %,d\n", stats.getEventsGenerated());
        System.out.printf("Buses rastreados:            %,d\n", stats.getBusesTracked());
        System.out.printf("Notificaciones enviadas:     %,d\n", listener.getNotificationCount());
        System.out.printf("Duración total:              %.2f segundos\n", totalSeconds);
        System.out.printf("Velocidad procesamiento:     %.0f datagramas/seg\n",
                stats.getDatagramsProcessed() / Math.max(totalSeconds, 1));
        System.out.println();

        // Distribución de eventos
        System.out.println("═══ DISTRIBUCIÓN DE EVENTOS ═══");
        Map<String, Long> eventDistribution = eventStore.getEventCountByType();
        eventDistribution.forEach((type, count) ->
                System.out.printf("  %-25s : %,d\n", type, count));
        System.out.println();

        // Últimas notificaciones
        System.out.println("═══ ÚLTIMAS 5 NOTIFICACIONES ═══");
        List<Notification> recentNotifications = listener.getRecentNotifications(5);
        if (recentNotifications.isEmpty()) {
            System.out.println("  (No hay notificaciones)");
        } else {
            for (int i = 0; i < recentNotifications.size(); i++) {
                Notification notif = recentNotifications.get(i);
                System.out.printf("%d. %s\n", i + 1, notif.getMessage());
            }
        }
        System.out.println();

        // Muestra de buses rastreados
        Map<Integer, BusPosition> positions = streamingService.getAllPositions();
        System.out.println("═══ MUESTRA DE BUSES RASTREADOS (5 primeros) ═══");
        positions.entrySet().stream()
                .limit(5)
                .forEach(entry -> {
                    BusPosition pos = entry.getValue();
                    System.out.printf("Bus %d: Ruta %d, Línea %d, Pos=(%.4f, %.4f), Vel=%.1f km/h\n",
                            pos.getBusId(), pos.getRouteId(), pos.getLineId(),
                            pos.getLatitude(), pos.getLongitude(), pos.getVelocity());
                });
        System.out.println();
    }

    /**
     * Listener de prueba para contar notificaciones
     */
    private static class TestNotificationListener implements NotificationListener {
        private int notificationCount = 0;
        private final List<Notification> recentNotifications = new java.util.concurrent.CopyOnWriteArrayList<>();

        @Override
        public void onNotification(Notification notification) {
            notificationCount++;
            recentNotifications.add(notification);
            if (recentNotifications.size() > 100) {
                recentNotifications.remove(0);
            }
        }

        @Override
        public String getListenerId() {
            return "TEST-LISTENER";
        }

        public int getNotificationCount() {
            return notificationCount;
        }

        public List<Notification> getRecentNotifications(int count) {
            int size = recentNotifications.size();
            int from = Math.max(0, size - count);
            return recentNotifications.subList(from, size);
        }
    }
}
