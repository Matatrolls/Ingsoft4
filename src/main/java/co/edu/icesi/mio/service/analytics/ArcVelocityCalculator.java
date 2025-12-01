package co.edu.icesi.mio.service.analytics;

import co.edu.icesi.mio.concurrency.datagram.DatagramProcessingMaster;
import co.edu.icesi.mio.infra.csv.DatagramReader;
import co.edu.icesi.mio.model.analytics.ArcIdentifier;
import co.edu.icesi.mio.model.analytics.ArcVelocityStats;
import co.edu.icesi.mio.model.streaming.Datagram;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Calculador de estadísticas de velocidad por arco.
 * Procesa datagramas históricos y genera estadísticas agregadas por arco.
 */
public class ArcVelocityCalculator {

    // Almacena todas las velocidades observadas por arco
    private final Map<ArcIdentifier, List<Double>> velocitiesByArc;

    // Contador de datagramas procesados
    private final AtomicLong processedCount;

    // Contador de datagramas válidos
    private final AtomicLong validCount;

    public ArcVelocityCalculator() {
        this.velocitiesByArc = new ConcurrentHashMap<>();
        this.processedCount = new AtomicLong(0);
        this.validCount = new AtomicLong(0);
    }

    /**
     * Procesa un archivo de datagramas para calcular velocidades por arco (versión secuencial)
     * Por defecto procesa solo 15,000 datagramas para evitar sobrecarga.
     *
     * @param filePath Ruta al archivo CSV
     * @return Estadísticas de procesamiento
     */
    public CalculationStats processDatagramFile(String filePath) throws IOException {
        return processDatagramFile(filePath, 15000); // Límite por defecto: 15,000 datagramas
    }

    /**
     * Procesa un archivo de datagramas para calcular velocidades por arco (versión secuencial)
     * con límite configurable.
     *
     * @param filePath Ruta al archivo CSV
     * @param maxDatagrams Número máximo de datagramas a procesar (usar Integer.MAX_VALUE para todos)
     * @return Estadísticas de procesamiento
     */
    public CalculationStats processDatagramFile(String filePath, int maxDatagrams) throws IOException {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  CALCULANDO VELOCIDADES DE ARCOS (SECUENCIAL)");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("Archivo: " + filePath);
        System.out.printf("Límite de datagramas: %,d\n", maxDatagrams);
        System.out.println();

        long startTime = System.currentTimeMillis();

        AtomicLong readCount = new AtomicLong(0);
        DatagramReader reader = new DatagramReader(filePath);
        DatagramReader.ReadStats readStats = reader.readWithFilter(
                this::processDatagram,
                datagram -> datagram.isValid() && readCount.incrementAndGet() <= maxDatagrams
        );

        long endTime = System.currentTimeMillis();
        long durationMs = endTime - startTime;

        CalculationStats stats = new CalculationStats(
                readStats.getProcessedRecords(),
                validCount.get(),
                velocitiesByArc.size(),
                durationMs
        );

        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("  PROCESAMIENTO COMPLETADO");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println(stats);
        System.out.println();

        return stats;
    }

    /**
     * Procesa un archivo de datagramas usando procesamiento paralelo con patrón Master-Worker.
     * Esta versión es más eficiente para archivos grandes.
     *
     * @param filePath Ruta al archivo CSV
     * @param numWorkers Número de workers para procesamiento paralelo
     * @return Mapa de estadísticas de velocidad por arco
     */
    public Map<ArcIdentifier, ArcVelocityStats> processDatagramFileParallel(String filePath, int numWorkers)
            throws IOException, InterruptedException {

        long startTime = System.currentTimeMillis();

        DatagramProcessingMaster master = new DatagramProcessingMaster(filePath, numWorkers);
        Map<ArcIdentifier, ArcVelocityStats> stats = master.process();

        long endTime = System.currentTimeMillis();
        double durationSeconds = (endTime - startTime) / 1000.0;

        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("  PROCESAMIENTO PARALELO COMPLETADO");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.printf("Duración total: %.2f segundos\n", durationSeconds);
        System.out.printf("Estadísticas calculadas: %,d arcos\n", stats.size());
        System.out.println();

        return stats;
    }

    /**
     * Procesa un datagrama individual y extrae su velocidad para el arco correspondiente
     */
    private void processDatagram(Datagram datagram) {
        processedCount.incrementAndGet();

        // Validar que tenga velocidad válida
        if (datagram.getVelocity() <= 0) {
            return;
        }

        // Crear identificador de arco (por ahora, solo ruta+línea)
        ArcIdentifier arcId = ArcIdentifier.forRoute(
                datagram.getRouteId(),
                datagram.getLineId()
        );

        // Agregar velocidad a la lista del arco
        velocitiesByArc.computeIfAbsent(arcId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add((double) datagram.getVelocity());

        validCount.incrementAndGet();
    }

    /**
     * Calcula las estadísticas finales para todos los arcos procesados
     *
     * @return Mapa de estadísticas por arco
     */
    public Map<ArcIdentifier, ArcVelocityStats> calculateStatistics() {
        System.out.println("Calculando estadísticas para " + velocitiesByArc.size() + " arcos...");

        Map<ArcIdentifier, ArcVelocityStats> stats = new HashMap<>();
        int count = 0;

        for (Map.Entry<ArcIdentifier, List<Double>> entry : velocitiesByArc.entrySet()) {
            ArcIdentifier arcId = entry.getKey();
            List<Double> velocities = entry.getValue();

            ArcVelocityStats arcStats = new ArcVelocityStats.Builder(arcId)
                    .addVelocities(velocities)
                    .build();

            stats.put(arcId, arcStats);

            count++;
            if (count % 100 == 0) {
                System.out.printf("  Procesados %d arcos...\r", count);
            }
        }

        System.out.printf("\n✓ Estadísticas calculadas para %d arcos\n", count);
        return stats;
    }

    /**
     * Obtiene estadísticas resumidas del procesamiento
     */
    public CalculationStats getProcessingStats() {
        return new CalculationStats(
                processedCount.get(),
                validCount.get(),
                velocitiesByArc.size(),
                0
        );
    }

    /**
     * Limpia los datos acumulados
     */
    public void clear() {
        velocitiesByArc.clear();
        processedCount.set(0);
        validCount.set(0);
    }

    /**
     * Clase para almacenar estadísticas del cálculo
     */
    public static class CalculationStats {
        private final long processedDatagrams;
        private final long validDatagrams;
        private final long uniqueArcs;
        private final long durationMs;

        public CalculationStats(long processedDatagrams, long validDatagrams,
                                long uniqueArcs, long durationMs) {
            this.processedDatagrams = processedDatagrams;
            this.validDatagrams = validDatagrams;
            this.uniqueArcs = uniqueArcs;
            this.durationMs = durationMs;
        }

        public long getProcessedDatagrams() {
            return processedDatagrams;
        }

        public long getValidDatagrams() {
            return validDatagrams;
        }

        public long getUniqueArcs() {
            return uniqueArcs;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public double getValidPercentage() {
            return processedDatagrams > 0 ? (validDatagrams * 100.0 / processedDatagrams) : 0.0;
        }

        public double getAvgVelocitiesPerArc() {
            return uniqueArcs > 0 ? (validDatagrams * 1.0 / uniqueArcs) : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                    "Estadísticas de Cálculo:\n" +
                    "  Datagramas procesados:    %,d\n" +
                    "  Datagramas válidos:       %,d (%.1f%%)\n" +
                    "  Arcos únicos encontrados: %,d\n" +
                    "  Velocidades promedio/arco: %.1f\n" +
                    "  Duración:                 %.2f segundos",
                    processedDatagrams, validDatagrams, getValidPercentage(),
                    uniqueArcs, getAvgVelocitiesPerArc(), durationMs / 1000.0
            );
        }
    }
}
