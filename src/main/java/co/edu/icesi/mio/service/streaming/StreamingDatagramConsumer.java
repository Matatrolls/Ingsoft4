package co.edu.icesi.mio.service.streaming;

import co.edu.icesi.mio.infra.csv.DatagramReader;
import co.edu.icesi.mio.model.streaming.Datagram;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Consumidor de datagramas en streaming desde archivo CSV.
 * Lee el archivo de forma asíncrona y encola datagramas para procesamiento en tiempo real.
 */
public class StreamingDatagramConsumer implements Runnable {

    private final String filePath;
    private final BlockingQueue<Datagram> datagramQueue;
    private final AtomicBoolean running;
    private final AtomicLong totalRead;
    private final AtomicLong totalQueued;
    private final Consumer<ConsumerStats> statsCallback;

    // Configuración
    private final int maxQueueSize;
    private final boolean skipInvalidDatagrams;
    private final int maxDatagrams; // Límite de datagramas a procesar

    public StreamingDatagramConsumer(String filePath) {
        this(filePath, 10000, 15000, null); // Límite por defecto: 15,000
    }

    public StreamingDatagramConsumer(String filePath, int maxQueueSize, Consumer<ConsumerStats> statsCallback) {
        this(filePath, maxQueueSize, 15000, statsCallback); // Límite por defecto: 15,000
    }

    public StreamingDatagramConsumer(String filePath, int maxQueueSize, int maxDatagrams,
                                     Consumer<ConsumerStats> statsCallback) {
        this.filePath = filePath;
        this.maxQueueSize = maxQueueSize;
        this.maxDatagrams = maxDatagrams;
        this.datagramQueue = new LinkedBlockingQueue<>(maxQueueSize);
        this.running = new AtomicBoolean(false);
        this.totalRead = new AtomicLong(0);
        this.totalQueued = new AtomicLong(0);
        this.skipInvalidDatagrams = true;
        this.statsCallback = statsCallback;
    }

    @Override
    public void run() {
        running.set(true);
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  STREAMING DATAGRAM CONSUMER - INICIADO");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.printf("Archivo: %s\n", filePath);
        System.out.printf("Tamaño de cola: %,d\n", maxQueueSize);
        System.out.printf("Límite de datagramas: %,d\n", maxDatagrams);
        System.out.println();

        long startTime = System.currentTimeMillis();

        try {
            AtomicLong readCount = new AtomicLong(0);
            DatagramReader reader = new DatagramReader(filePath);
            reader.skipInvalidLines(skipInvalidDatagrams);

            DatagramReader.ReadStats readStats = reader.readWithFilter(
                    this::enqueueDatagram,
                    datagram -> datagram.isValid() && readCount.incrementAndGet() <= maxDatagrams
            );

            long endTime = System.currentTimeMillis();
            double durationSeconds = (endTime - startTime) / 1000.0;

            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  LECTURA COMPLETADA");
            System.out.println("═══════════════════════════════════════════════════════════");
            System.out.printf("Total leído: %,d datagramas\n", totalRead.get());
            System.out.printf("Total encolado: %,d datagramas\n", totalQueued.get());
            System.out.printf("Duración: %.2f segundos\n", durationSeconds);
            System.out.printf("Velocidad: %.0f datagramas/segundo\n", totalRead.get() / Math.max(durationSeconds, 1));
            System.out.println();

            // Notificar estadísticas finales
            if (statsCallback != null) {
                statsCallback.accept(new ConsumerStats(
                        totalRead.get(),
                        totalQueued.get(),
                        durationSeconds,
                        true
                ));
            }

        } catch (IOException e) {
            System.err.println("✗ Error leyendo archivo de streaming: " + e.getMessage());
            e.printStackTrace();
        } finally {
            running.set(false);
        }
    }

    /**
     * Encola un datagrama para procesamiento
     */
    private void enqueueDatagram(Datagram datagram) {
        totalRead.incrementAndGet();

        try {
            datagramQueue.put(datagram);
            totalQueued.incrementAndGet();

            // Reportar progreso cada 10,000 datagramas
            if (totalQueued.get() % 10000 == 0) {
                System.out.printf("Encolados: %,d datagramas (cola: %,d)\n",
                        totalQueued.get(), datagramQueue.size());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("✗ Interrupción al encolar datagrama");
        }
    }

    /**
     * Obtiene el siguiente datagrama de la cola (bloqueante)
     */
    public Datagram nextDatagram() throws InterruptedException {
        return datagramQueue.take();
    }

    /**
     * Obtiene el siguiente datagrama con timeout
     */
    public Datagram nextDatagram(long timeoutMs) throws InterruptedException {
        return datagramQueue.poll(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Obtiene múltiples datagramas de la cola
     */
    public List<Datagram> nextBatch(int maxSize) {
        List<Datagram> batch = new ArrayList<>(maxSize);
        datagramQueue.drainTo(batch, maxSize);
        return batch;
    }

    /**
     * Verifica si el consumidor está corriendo
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Verifica si hay datagramas disponibles
     */
    public boolean hasDatagrams() {
        return !datagramQueue.isEmpty() || running.get();
    }

    /**
     * Obtiene el tamaño actual de la cola
     */
    public int getQueueSize() {
        return datagramQueue.size();
    }

    /**
     * Obtiene estadísticas actuales
     */
    public ConsumerStats getStats() {
        return new ConsumerStats(
                totalRead.get(),
                totalQueued.get(),
                0,
                running.get()
        );
    }

    /**
     * Clase para estadísticas del consumidor
     */
    public static class ConsumerStats {
        private final long totalRead;
        private final long totalQueued;
        private final double durationSeconds;
        private final boolean completed;

        public ConsumerStats(long totalRead, long totalQueued, double durationSeconds, boolean completed) {
            this.totalRead = totalRead;
            this.totalQueued = totalQueued;
            this.durationSeconds = durationSeconds;
            this.completed = completed;
        }

        public long getTotalRead() {
            return totalRead;
        }

        public long getTotalQueued() {
            return totalQueued;
        }

        public double getDurationSeconds() {
            return durationSeconds;
        }

        public boolean isCompleted() {
            return completed;
        }

        @Override
        public String toString() {
            return String.format("ConsumerStats[read=%,d, queued=%,d, duration=%.2fs, completed=%s]",
                    totalRead, totalQueued, durationSeconds, completed);
        }
    }
}
