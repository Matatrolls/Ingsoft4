package co.edu.icesi.mio.concurrency.datagram;

import co.edu.icesi.mio.concurrency.Worker;
import co.edu.icesi.mio.model.analytics.ArcIdentifier;
import co.edu.icesi.mio.model.streaming.Datagram;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Worker que procesa un chunk de archivo de datagramas.
 * Extrae velocidades por arco de forma paralela.
 */
public class DatagramProcessingWorker implements Worker<FileChunk, DatagramProcessingResult> {

    private final int workerId;
    private final Queue<FileChunk> workQueue;
    private DatagramProcessingResult result;
    private volatile boolean done;

    public DatagramProcessingWorker(int workerId) {
        this.workerId = workerId;
        this.workQueue = new LinkedList<>();
        this.done = false;
    }

    @Override
    public void assignWork(FileChunk work) {
        workQueue.offer(work);
    }

    @Override
    public void run() {
        Map<ArcIdentifier, List<Double>> velocitiesByArc = new ConcurrentHashMap<>();
        long processedDatagrams = 0;
        long validDatagrams = 0;
        long errorCount = 0;

        // Procesar todos los chunks asignados
        while (!workQueue.isEmpty()) {
            FileChunk chunk = workQueue.poll();

            try {
                ChunkStats stats = processChunk(chunk, velocitiesByArc);
                processedDatagrams += stats.processedCount;
                validDatagrams += stats.validCount;
                errorCount += stats.errorCount;

            } catch (IOException e) {
                System.err.printf("Worker %d: Error procesando %s: %s\n",
                        workerId, chunk, e.getMessage());
                errorCount++;
            }
        }

        result = new DatagramProcessingResult(
                workerId,
                velocitiesByArc,
                processedDatagrams,
                validDatagrams,
                errorCount
        );

        done = true;
    }

    private ChunkStats processChunk(FileChunk chunk, Map<ArcIdentifier, List<Double>> velocitiesByArc)
            throws IOException {

        long processedCount = 0;
        long validCount = 0;
        long errorCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(chunk.getFilePath()))) {
            String line;
            long currentLine = 0;

            // Saltar hasta la línea de inicio
            while (currentLine < chunk.getStartLine() && (line = reader.readLine()) != null) {
                currentLine++;
            }

            // Procesar líneas del chunk
            while (currentLine < chunk.getEndLine() && (line = reader.readLine()) != null) {
                currentLine++;
                processedCount++;

                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    Datagram datagram = Datagram.fromCsvLine(line);

                    if (datagram.isValid() && datagram.getVelocity() > 0) {
                        ArcIdentifier arcId = ArcIdentifier.forRoute(
                                datagram.getRouteId(),
                                datagram.getLineId()
                        );

                        velocitiesByArc.computeIfAbsent(arcId,
                                k -> Collections.synchronizedList(new ArrayList<>()))
                                .add((double) datagram.getVelocity());

                        validCount++;
                    }

                } catch (Exception e) {
                    errorCount++;
                }
            }
        }

        return new ChunkStats(processedCount, validCount, errorCount);
    }

    @Override
    public DatagramProcessingResult getResult() {
        return result;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public int getWorkerId() {
        return workerId;
    }

    private static class ChunkStats {
        final long processedCount;
        final long validCount;
        final long errorCount;

        ChunkStats(long processedCount, long validCount, long errorCount) {
            this.processedCount = processedCount;
            this.validCount = validCount;
            this.errorCount = errorCount;
        }
    }
}
