package co.edu.icesi.mio.concurrency.zone;

import co.edu.icesi.mio.model.analytics.ArcVelocityStats;
import co.edu.icesi.mio.model.analytics.Zone;

import java.util.List;

/**
 * Resultado del análisis de una zona geográfica.
 * Contiene estadísticas de tráfico, paradas, rutas y velocidades en la zona.
 */
public class ZoneAnalysisResult {

    private final int workerId;
    private final Zone zone;
    private final int stopCount;           // Número de paradas en la zona
    private final int arcCount;            // Número de arcos en la zona
    private final int uniqueRoutesCount;   // Número de rutas únicas
    private final double averageVelocity;  // Velocidad promedio en la zona
    private final double minVelocity;      // Velocidad mínima observada
    private final double maxVelocity;      // Velocidad máxima observada
    private final List<ArcVelocityStats> slowestArcs;  // Arcos más lentos
    private final List<ArcVelocityStats> fastestArcs;  // Arcos más rápidos
    private final long analysisTimeMs;     // Tiempo de análisis

    public ZoneAnalysisResult(int workerId, Zone zone, int stopCount, int arcCount,
                              int uniqueRoutesCount, double averageVelocity,
                              double minVelocity, double maxVelocity,
                              List<ArcVelocityStats> slowestArcs,
                              List<ArcVelocityStats> fastestArcs,
                              long analysisTimeMs) {
        this.workerId = workerId;
        this.zone = zone;
        this.stopCount = stopCount;
        this.arcCount = arcCount;
        this.uniqueRoutesCount = uniqueRoutesCount;
        this.averageVelocity = averageVelocity;
        this.minVelocity = minVelocity;
        this.maxVelocity = maxVelocity;
        this.slowestArcs = slowestArcs;
        this.fastestArcs = fastestArcs;
        this.analysisTimeMs = analysisTimeMs;
    }

    /**
     * Calcula la densidad de paradas (paradas por km²)
     */
    public double getStopDensity() {
        double area = zone.getApproximateAreaKm2();
        return area > 0 ? stopCount / area : 0;
    }

    /**
     * Calcula la densidad de arcos (arcos por km²)
     */
    public double getArcDensity() {
        double area = zone.getApproximateAreaKm2();
        return area > 0 ? arcCount / area : 0;
    }

    /**
     * Indica si la zona tiene tráfico congestionado (velocidad baja)
     */
    public boolean isCongested() {
        return averageVelocity < 15.0; // Menos de 15 km/h se considera congestionado
    }

    /**
     * Indica si la zona tiene buena cobertura de transporte
     */
    public boolean hasGoodCoverage() {
        return stopCount >= 20 && uniqueRoutesCount >= 5;
    }

    public int getWorkerId() {
        return workerId;
    }

    public Zone getZone() {
        return zone;
    }

    public int getStopCount() {
        return stopCount;
    }

    public int getArcCount() {
        return arcCount;
    }

    public int getUniqueRoutesCount() {
        return uniqueRoutesCount;
    }

    public double getAverageVelocity() {
        return averageVelocity;
    }

    public double getMinVelocity() {
        return minVelocity;
    }

    public double getMaxVelocity() {
        return maxVelocity;
    }

    public List<ArcVelocityStats> getSlowestArcs() {
        return slowestArcs;
    }

    public List<ArcVelocityStats> getFastestArcs() {
        return fastestArcs;
    }

    public long getAnalysisTimeMs() {
        return analysisTimeMs;
    }

    @Override
    public String toString() {
        return String.format(
                "ZoneAnalysis[%s: paradas=%d, arcos=%d, rutas=%d, vel_prom=%.1f km/h, %s]",
                zone.getZoneName(), stopCount, arcCount, uniqueRoutesCount,
                averageVelocity, isCongested() ? "CONGESTIONADO" : "FLUIDO");
    }

    /**
     * Genera un reporte detallado de la zona
     */
    public String toDetailedReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════════════\n");
        sb.append(String.format("  ANÁLISIS DE ZONA: %s (%s)\n", zone.getZoneName(), zone.getZoneId()));
        sb.append("═══════════════════════════════════════════════════════════\n");
        sb.append(String.format("Área aproximada:        %.2f km²\n", zone.getApproximateAreaKm2()));
        sb.append(String.format("Paradas en la zona:     %d (densidad: %.1f paradas/km²)\n",
                stopCount, getStopDensity()));
        sb.append(String.format("Arcos en la zona:       %d (densidad: %.1f arcos/km²)\n",
                arcCount, getArcDensity()));
        sb.append(String.format("Rutas únicas:           %d\n", uniqueRoutesCount));
        sb.append(String.format("Velocidad promedio:     %.1f km/h\n", averageVelocity));
        sb.append(String.format("Velocidad mínima:       %.1f km/h\n", minVelocity));
        sb.append(String.format("Velocidad máxima:       %.1f km/h\n", maxVelocity));
        sb.append(String.format("Estado de tráfico:      %s\n",
                isCongested() ? "⚠ CONGESTIONADO" : "✓ FLUIDO"));
        sb.append(String.format("Cobertura:              %s\n",
                hasGoodCoverage() ? "✓ BUENA" : "⚠ LIMITADA"));
        sb.append(String.format("Tiempo de análisis:     %d ms\n", analysisTimeMs));

        if (!slowestArcs.isEmpty()) {
            sb.append("\nArcos más lentos (top 3):\n");
            for (int i = 0; i < Math.min(3, slowestArcs.size()); i++) {
                ArcVelocityStats arc = slowestArcs.get(i);
                sb.append(String.format("  %d. Ruta %d, Línea %d: %.1f km/h\n",
                        i + 1, arc.getArcId().getRouteId(), arc.getArcId().getLineId(),
                        arc.getTypicalVelocity()));
            }
        }

        if (!fastestArcs.isEmpty()) {
            sb.append("\nArcos más rápidos (top 3):\n");
            for (int i = 0; i < Math.min(3, fastestArcs.size()); i++) {
                ArcVelocityStats arc = fastestArcs.get(i);
                sb.append(String.format("  %d. Ruta %d, Línea %d: %.1f km/h\n",
                        i + 1, arc.getArcId().getRouteId(), arc.getArcId().getLineId(),
                        arc.getTypicalVelocity()));
            }
        }

        sb.append("═══════════════════════════════════════════════════════════\n");
        return sb.toString();
    }
}
