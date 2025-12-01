package co.edu.icesi.mio.concurrency.datagram;

import co.edu.icesi.mio.concurrency.Master;
import co.edu.icesi.mio.model.analytics.ArcIdentifier;
import co.edu.icesi.mio.model.analytics.ArcVelocityStats;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Master que coordina el procesamiento paralelo de archivos de datagramas.
 * Divide el archivo en chunks y distribuye el trabajo entre workers.
 */
public class DatagramProcessingMaster {

    private final int numWorkers;
    private final String filePath;

    public DatagramProcessingMaster(String filePath, int numWorkers) {
        this.filePath = filePath;
        this.numWorkers = numWorkers;
    }

    /**
     * Procesa el archivo de datagramas usando múltiples workers
     *
     * @return Mapa de estadísticas de velocidad por arco
     */
    public Map<ArcIdentifier, ArcVelocityStats> process() throws IOException, InterruptedException {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  PROCESAMIENTO PARALELO DE DATAGRAMAS");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.printf("Archivo: %s\n", filePath);
        System.out.printf("Workers: %d\n", numWorkers);
        System.out.println();

        // 1. Contar líneas del archivo
        long totalLines = countLines();
        System.out.printf("✓ Líneas totales: %,d\n", totalLines);

        // 2. Crear chunks para distribución de trabajo
        List<FileChunk> chunks = createChunks(totalLines);
        System.out.printf("✓ Chunks creados: %d (aprox. %,d líneas por chunk)\n\n",
                chunks.size(), totalLines / chunks.size());

        // 3. Crear Master genérico
        Master<FileChunk, DatagramProcessingResult, Map<ArcIdentifier, ArcVelocityStats>> master =
                new Master<>(
                        numWorkers,
                        DatagramProcessingWorker::new,
                        this::aggregateResults
                );

        // 4. Procesar chunks
        Map<ArcIdentifier, ArcVelocityStats> result = master.process(chunks);

        // 5. Shutdown master
        master.shutdown();

        return result;
    }

    /**
     * Cuenta las líneas del archivo
     */
    private long countLines() throws IOException {
        long count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            while (reader.readLine() != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * Crea chunks de trabajo dividiendo el archivo en segmentos balanceados
     */
    private List<FileChunk> createChunks(long totalLines) {
        List<FileChunk> chunks = new ArrayList<>();

        // Saltar header (primera línea)
        long dataLines = totalLines - 1;
        long linesPerChunk = dataLines / numWorkers;
        long remainingLines = dataLines % numWorkers;

        long currentLine = 1; // Empezar después del header
        int chunkId = 0;

        for (int i = 0; i < numWorkers; i++) {
            long chunkSize = linesPerChunk;
            // Distribuir las líneas restantes entre los primeros chunks
            if (i < remainingLines) {
                chunkSize++;
            }

            long endLine = currentLine + chunkSize;

            chunks.add(new FileChunk(filePath, currentLine, endLine, chunkId++));

            currentLine = endLine;
        }

        return chunks;
    }

    /**
     * Agrega los resultados de todos los workers en un mapa consolidado de estadísticas
     */
    private Map<ArcIdentifier, ArcVelocityStats> aggregateResults(List<DatagramProcessingResult> results) {
        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("  AGREGANDO RESULTADOS");
        System.out.println("═══════════════════════════════════════════════════════════");

        // Consolidar velocidades de todos los workers
        Map<ArcIdentifier, List<Double>> consolidatedVelocities = new ConcurrentHashMap<>();

        long totalProcessed = 0;
        long totalValid = 0;
        long totalErrors = 0;

        for (DatagramProcessingResult result : results) {
            System.out.println("✓ " + result);

            // Merge velocities
            result.getVelocitiesByArc().forEach((arcId, velocities) -> {
                consolidatedVelocities.computeIfAbsent(arcId, k -> Collections.synchronizedList(new ArrayList<>()))
                        .addAll(velocities);
            });

            totalProcessed += result.getProcessedDatagrams();
            totalValid += result.getValidDatagrams();
            totalErrors += result.getErrorCount();
        }

        System.out.println();
        System.out.printf("Total procesados: %,d\n", totalProcessed);
        System.out.printf("Total válidos: %,d (%.1f%%)\n", totalValid,
                (totalProcessed > 0 ? (totalValid * 100.0 / totalProcessed) : 0));
        System.out.printf("Total errores: %,d\n", totalErrors);
        System.out.printf("Arcos únicos: %,d\n", consolidatedVelocities.size());
        System.out.println();

        // Calcular estadísticas para cada arco
        System.out.println("Calculando estadísticas por arco...");
        Map<ArcIdentifier, ArcVelocityStats> stats = new HashMap<>();

        for (Map.Entry<ArcIdentifier, List<Double>> entry : consolidatedVelocities.entrySet()) {
            ArcIdentifier arcId = entry.getKey();
            List<Double> velocities = entry.getValue();

            ArcVelocityStats arcStats = new ArcVelocityStats.Builder(arcId)
                    .addVelocities(velocities)
                    .build();

            stats.put(arcId, arcStats);
        }

        System.out.printf("✓ Estadísticas calculadas para %,d arcos\n", stats.size());

        return stats;
    }
}
