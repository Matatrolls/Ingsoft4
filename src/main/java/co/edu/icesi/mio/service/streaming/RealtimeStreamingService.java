package co.edu.icesi.mio.service.streaming;

import co.edu.icesi.mio.model.events.BusEvent;
import co.edu.icesi.mio.model.events.BusEventStore;
import co.edu.icesi.mio.model.realtime.BusPosition;
import co.edu.icesi.mio.service.notifications.NotificationService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servicio orquestador de streaming en tiempo real.
 * Coordina el consumo de datagramas y su procesamiento, integrando con el sistema de eventos.
 */
public class RealtimeStreamingService {

    private final String streamingFilePath;
    private final BusEventStore eventStore;
    private final NotificationService notificationService;

    private StreamingDatagramConsumer consumer;
    private RealtimeStreamProcessor processor;
    private ExecutorService executorService;

    private final AtomicBoolean running;
    private final Map<Integer, BusPosition> latestPositions; // busId -> última posición

    // Configuración
    private double timeAccelerationFactor = 10.0; // 10x más rápido por defecto
    private boolean realTimeSimulation = true;
    private int queueSize = 10000;
    private int batchSize = 100;

    public RealtimeStreamingService(String streamingFilePath,
                                     BusEventStore eventStore,
                                     NotificationService notificationService) {
        this.streamingFilePath = streamingFilePath;
        this.eventStore = eventStore;
        this.notificationService = notificationService;
        this.running = new AtomicBoolean(false);
        this.latestPositions = new ConcurrentHashMap<>();
    }

    /**
     * Inicia el servicio de streaming
     */
    public void start() {
        if (running.get()) {
            System.out.println("⚠️  El servicio de streaming ya está corriendo");
            return;
        }

        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║     SERVICIO DE STREAMING EN TIEMPO REAL              ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.printf("Archivo: %s\n", streamingFilePath);
        System.out.printf("Aceleración temporal: %.1fx\n", timeAccelerationFactor);
        System.out.printf("Simulación tiempo real: %s\n", realTimeSimulation ? "SÍ" : "NO");
        System.out.println();

        running.set(true);

        // Crear consumer
        consumer = new StreamingDatagramConsumer(streamingFilePath, queueSize, null);

        // Crear processor
        processor = new RealtimeStreamProcessor(
                consumer,
                this::handleBusEvent,
                this::handleBusPosition,
                timeAccelerationFactor,
                realTimeSimulation,
                batchSize
        );

        // Crear executor para ambos threads
        executorService = Executors.newFixedThreadPool(2);

        // Iniciar consumer y processor
        executorService.submit(consumer);
        executorService.submit(processor);

        System.out.println("✓ Servicio de streaming iniciado correctamente");
        System.out.println();
    }

    /**
     * Maneja eventos de bus generados por el procesador
     */
    private void handleBusEvent(BusEvent event) {
        // Agregar al store de eventos
        eventStore.add(event);

        // Notificar a través del servicio de notificaciones
        notificationService.notifyEvent(event);
    }

    /**
     * Maneja actualizaciones de posición de buses
     */
    private void handleBusPosition(BusPosition position) {
        // Actualizar última posición conocida
        latestPositions.put(position.getBusId(), position);
    }

    /**
     * Detiene el servicio de streaming
     */
    public void stop() {
        if (!running.get()) {
            return;
        }

        System.out.println("\n✓ Deteniendo servicio de streaming...");

        processor.stop();
        running.set(false);

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("✓ Servicio de streaming detenido");
    }

    /**
     * Espera a que el procesamiento termine
     */
    public void waitForCompletion() {
        if (executorService == null) {
            return;
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Obtiene la última posición conocida de un bus
     */
    public BusPosition getLatestPosition(int busId) {
        return latestPositions.get(busId);
    }

    /**
     * Obtiene todas las posiciones actuales de buses
     */
    public Map<Integer, BusPosition> getAllPositions() {
        return new ConcurrentHashMap<>(latestPositions);
    }

    /**
     * Obtiene el número de buses siendo rastreados
     */
    public int getTrackedBusCount() {
        return latestPositions.size();
    }

    /**
     * Verifica si el servicio está corriendo
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Obtiene estadísticas del servicio
     */
    public ServiceStats getStats() {
        if (consumer == null || processor == null) {
            return new ServiceStats(0, 0, 0, 0);
        }

        StreamingDatagramConsumer.ConsumerStats consumerStats = consumer.getStats();
        RealtimeStreamProcessor.ProcessorStats processorStats = processor.getStats();

        return new ServiceStats(
                consumerStats.getTotalRead(),
                processorStats.getProcessedCount(),
                processorStats.getEventCount(),
                latestPositions.size()
        );
    }

    // Métodos de configuración
    public void setTimeAccelerationFactor(double factor) {
        this.timeAccelerationFactor = factor;
    }

    public void setRealTimeSimulation(boolean enabled) {
        this.realTimeSimulation = enabled;
    }

    public void setQueueSize(int size) {
        this.queueSize = size;
    }

    public void setBatchSize(int size) {
        this.batchSize = size;
    }

    /**
     * Clase para estadísticas del servicio
     */
    public static class ServiceStats {
        private final long datagramsRead;
        private final long datagramsProcessed;
        private final long eventsGenerated;
        private final int busesTracked;

        public ServiceStats(long datagramsRead, long datagramsProcessed, long eventsGenerated, int busesTracked) {
            this.datagramsRead = datagramsRead;
            this.datagramsProcessed = datagramsProcessed;
            this.eventsGenerated = eventsGenerated;
            this.busesTracked = busesTracked;
        }

        public long getDatagramsRead() {
            return datagramsRead;
        }

        public long getDatagramsProcessed() {
            return datagramsProcessed;
        }

        public long getEventsGenerated() {
            return eventsGenerated;
        }

        public int getBusesTracked() {
            return busesTracked;
        }

        @Override
        public String toString() {
            return String.format(
                    "StreamingStats[\n" +
                    "  Datagramas leídos:     %,d\n" +
                    "  Datagramas procesados: %,d\n" +
                    "  Eventos generados:     %,d\n" +
                    "  Buses rastreados:      %,d\n" +
                    "]",
                    datagramsRead, datagramsProcessed, eventsGenerated, busesTracked
            );
        }
    }
}
