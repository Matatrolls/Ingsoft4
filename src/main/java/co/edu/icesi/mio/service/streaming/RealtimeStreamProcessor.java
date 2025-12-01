package co.edu.icesi.mio.service.streaming;

import co.edu.icesi.mio.model.events.BusEvent;
import co.edu.icesi.mio.model.events.EventCategory;
import co.edu.icesi.mio.model.events.EventPriority;
import co.edu.icesi.mio.model.events.EventType;
import co.edu.icesi.mio.model.realtime.BusPosition;
import co.edu.icesi.mio.model.streaming.Datagram;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Procesador de streaming en tiempo real.
 * Simula el flujo de eventos en tiempo real basándose en los timestamps de los datagramas.
 */
public class RealtimeStreamProcessor implements Runnable {

    private final StreamingDatagramConsumer consumer;
    private final Consumer<BusEvent> eventConsumer;
    private final Consumer<BusPosition> positionConsumer;
    private final AtomicBoolean running;
    private final AtomicLong processedCount;
    private final AtomicLong eventCount;

    // Configuración de simulación temporal
    private final double timeAccelerationFactor; // Factor de aceleración (1.0 = tiempo real, 2.0 = 2x más rápido)
    private final boolean realTimeSimulation;     // Si true, simula timing real basado en timestamps
    private final int batchSize;                  // Tamaño de batch para procesamiento

    // Umbrales para generación de eventos
    private static final int HIGH_VELOCITY_THRESHOLD = 80;  // km/h
    private static final int STOPPED_VELOCITY_THRESHOLD = 2; // km/h

    public RealtimeStreamProcessor(StreamingDatagramConsumer consumer,
                                    Consumer<BusEvent> eventConsumer,
                                    Consumer<BusPosition> positionConsumer) {
        this(consumer, eventConsumer, positionConsumer, 10.0, true, 100);
    }

    public RealtimeStreamProcessor(StreamingDatagramConsumer consumer,
                                    Consumer<BusEvent> eventConsumer,
                                    Consumer<BusPosition> positionConsumer,
                                    double timeAccelerationFactor,
                                    boolean realTimeSimulation,
                                    int batchSize) {
        this.consumer = consumer;
        this.eventConsumer = eventConsumer;
        this.positionConsumer = positionConsumer;
        this.running = new AtomicBoolean(false);
        this.processedCount = new AtomicLong(0);
        this.eventCount = new AtomicLong(0);
        this.timeAccelerationFactor = timeAccelerationFactor;
        this.realTimeSimulation = realTimeSimulation;
        this.batchSize = batchSize;
    }

    @Override
    public void run() {
        running.set(true);
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  REALTIME STREAM PROCESSOR - INICIADO");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.printf("Simulación tiempo real: %s\n", realTimeSimulation ? "SÍ" : "NO");
        System.out.printf("Factor de aceleración: %.1fx\n", timeAccelerationFactor);
        System.out.printf("Tamaño de batch: %d\n", batchSize);
        System.out.println();

        long startTime = System.currentTimeMillis();
        LocalDateTime lastTimestamp = null;

        try {
            while (running.get() && consumer.hasDatagrams()) {
                // Obtener batch de datagramas
                List<Datagram> batch = consumer.nextBatch(batchSize);

                if (batch.isEmpty()) {
                    // Si no hay datagramas, esperar un poco
                    if (consumer.isRunning()) {
                        Thread.sleep(100);
                        continue;
                    } else {
                        // Consumer terminó y no hay más datagramas
                        break;
                    }
                }

                // Procesar cada datagrama del batch
                for (Datagram datagram : batch) {
                    // Simular delay temporal si está habilitado
                    if (realTimeSimulation && lastTimestamp != null) {
                        simulateTemporalDelay(lastTimestamp, datagram.getTimestamp());
                    }
                    lastTimestamp = datagram.getTimestamp();

                    // Procesar datagrama
                    processDatagram(datagram);
                    processedCount.incrementAndGet();

                    // Reportar progreso
                    if (processedCount.get() % 1000 == 0) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        double rate = processedCount.get() / (elapsed / 1000.0);
                        System.out.printf("\rProcesados: %,d datagramas | Eventos: %,d | %.0f dg/s",
                                processedCount.get(), eventCount.get(), rate);
                    }
                }
            }

            long endTime = System.currentTimeMillis();
            double durationSeconds = (endTime - startTime) / 1000.0;

            System.out.println("\n\n═══════════════════════════════════════════════════════════");
            System.out.println("  PROCESAMIENTO COMPLETADO");
            System.out.println("═══════════════════════════════════════════════════════════");
            System.out.printf("Total procesados: %,d datagramas\n", processedCount.get());
            System.out.printf("Eventos generados: %,d\n", eventCount.get());
            System.out.printf("Duración: %.2f segundos\n", durationSeconds);
            System.out.printf("Velocidad: %.0f datagramas/segundo\n", processedCount.get() / Math.max(durationSeconds, 1));
            System.out.println();

        } catch (InterruptedException e) {
            System.err.println("\n✗ Procesamiento interrumpido");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("\n✗ Error durante procesamiento: " + e.getMessage());
            e.printStackTrace();
        } finally {
            running.set(false);
        }
    }

    /**
     * Procesa un datagrama individual
     */
    private void processDatagram(Datagram datagram) {
        // 1. Crear posición de bus
        BusPosition position = new BusPosition(
                datagram.getBusCode(),
                datagram.getRouteId(),
                datagram.getLineId(),
                datagram.getLatitude(),
                datagram.getLongitude(),
                datagram.getVelocity(),
                -1,  // currentStopId - no disponible en datagram básico
                -1,  // nextStopId - no disponible en datagram básico
                datagram.getTimestamp()
        );

        // Notificar posición
        if (positionConsumer != null) {
            positionConsumer.accept(position);
        }

        // 2. Generar eventos basados en condiciones
        List<BusEvent> events = generateEvents(datagram);
        for (BusEvent event : events) {
            if (eventConsumer != null) {
                eventConsumer.accept(event);
                eventCount.incrementAndGet();
            }
        }
    }

    /**
     * Genera eventos basados en las condiciones del datagrama
     */
    private List<BusEvent> generateEvents(Datagram datagram) {
        List<BusEvent> events = new ArrayList<>();

        // Evento de alta velocidad
        if (datagram.getVelocity() > HIGH_VELOCITY_THRESHOLD) {
            BusEvent event = new BusEvent(
                    datagram.getBusCode(),
                    EventType.BUS_SPEEDING,
                    EventCategory.OPERACIONAL,
                    EventPriority.ALTA,
                    String.format("Bus excede velocidad segura: %d km/h", datagram.getVelocity()),
                    datagram.getTimestamp(),
                    datagram.getLatitude(),
                    datagram.getLongitude()
            );
            event.setRouteId(datagram.getRouteId());
            event.setLineId(datagram.getLineId());
            events.add(event);
        }

        // Evento de bus detenido (si tiene velocidad muy baja pero está "en servicio")
        if (datagram.getVelocity() < STOPPED_VELOCITY_THRESHOLD && datagram.getVelocity() >= 0) {
            BusEvent event = new BusEvent(
                    datagram.getBusCode(),
                    EventType.BUS_STOPPED,
                    EventCategory.OPERACIONAL,
                    EventPriority.BAJA,
                    "Bus detenido o en parada",
                    datagram.getTimestamp(),
                    datagram.getLatitude(),
                    datagram.getLongitude()
            );
            event.setRouteId(datagram.getRouteId());
            event.setLineId(datagram.getLineId());
            events.add(event);
        }

        // Evento de actualización de posición (siempre)
        BusEvent positionEvent = new BusEvent(
                datagram.getBusCode(),
                EventType.POSITION_UPDATE,
                EventCategory.OPERACIONAL,
                EventPriority.BAJA,
                "Actualización de posición",
                datagram.getTimestamp(),
                datagram.getLatitude(),
                datagram.getLongitude()
        );
        positionEvent.setRouteId(datagram.getRouteId());
        positionEvent.setLineId(datagram.getLineId());
        events.add(positionEvent);

        return events;
    }

    /**
     * Simula el delay temporal entre dos timestamps
     */
    private void simulateTemporalDelay(LocalDateTime previous, LocalDateTime current) throws InterruptedException {
        Duration realDuration = Duration.between(previous, current);
        long realMillis = realDuration.toMillis();

        if (realMillis > 0) {
            // Aplicar factor de aceleración
            long simulatedMillis = (long) (realMillis / timeAccelerationFactor);

            // Limitar delay máximo para evitar esperas muy largas
            long maxDelay = 5000; // 5 segundos máximo
            long actualDelay = Math.min(simulatedMillis, maxDelay);

            if (actualDelay > 0) {
                Thread.sleep(actualDelay);
            }
        }
    }

    /**
     * Detiene el procesador
     */
    public void stop() {
        running.set(false);
    }

    /**
     * Verifica si está corriendo
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Obtiene estadísticas
     */
    public ProcessorStats getStats() {
        return new ProcessorStats(processedCount.get(), eventCount.get());
    }

    /**
     * Clase para estadísticas del procesador
     */
    public static class ProcessorStats {
        private final long processedCount;
        private final long eventCount;

        public ProcessorStats(long processedCount, long eventCount) {
            this.processedCount = processedCount;
            this.eventCount = eventCount;
        }

        public long getProcessedCount() {
            return processedCount;
        }

        public long getEventCount() {
            return eventCount;
        }

        @Override
        public String toString() {
            return String.format("ProcessorStats[processed=%,d, events=%,d]", processedCount, eventCount);
        }
    }
}
