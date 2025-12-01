package co.edu.icesi.mio.concurrency.datagram;

import co.edu.icesi.mio.model.analytics.ArcIdentifier;

import java.util.List;
import java.util.Map;

/**
 * Resultado del procesamiento de datagramas por un worker.
 * Contiene las velocidades extraídas y estadísticas del procesamiento.
 */
public class DatagramProcessingResult {

    private final int workerId;
    private final Map<ArcIdentifier, List<Double>> velocitiesByArc;
    private final long processedDatagrams;
    private final long validDatagrams;
    private final long errorCount;

    public DatagramProcessingResult(int workerId,
                                    Map<ArcIdentifier, List<Double>> velocitiesByArc,
                                    long processedDatagrams,
                                    long validDatagrams,
                                    long errorCount) {
        this.workerId = workerId;
        this.velocitiesByArc = velocitiesByArc;
        this.processedDatagrams = processedDatagrams;
        this.validDatagrams = validDatagrams;
        this.errorCount = errorCount;
    }

    public int getWorkerId() {
        return workerId;
    }

    public Map<ArcIdentifier, List<Double>> getVelocitiesByArc() {
        return velocitiesByArc;
    }

    public long getProcessedDatagrams() {
        return processedDatagrams;
    }

    public long getValidDatagrams() {
        return validDatagrams;
    }

    public long getErrorCount() {
        return errorCount;
    }

    @Override
    public String toString() {
        return String.format("Worker[%d]: procesados=%d, válidos=%d, errores=%d, arcos únicos=%d",
                workerId, processedDatagrams, validDatagrams, errorCount, velocitiesByArc.size());
    }
}
