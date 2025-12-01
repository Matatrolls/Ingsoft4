package co.edu.icesi.mio.model.realtime;

import co.edu.icesi.mio.infra.csv.Parada;

import java.time.LocalDateTime;

/**
 * Representa el tiempo estimado de llegada (ETA) de un bus a una parada.
 * Incluye información sobre el bus, la parada destino y el cálculo del ETA.
 */
public class BusETA {

    private final int busId;
    private final int routeId;
    private final int lineId;
    private final Parada targetStop;
    private final double estimatedTimeMinutes;   // Tiempo estimado en minutos
    private final double distanceMeters;         // Distancia restante en metros
    private final LocalDateTime estimatedArrival; // Hora estimada de llegada
    private final LocalDateTime calculatedAt;    // Cuándo se calculó este ETA
    private final String confidenceLevel;        // Alta/Media/Baja confiabilidad

    public BusETA(int busId, int routeId, int lineId, Parada targetStop,
                  double estimatedTimeMinutes, double distanceMeters,
                  LocalDateTime calculatedAt, String confidenceLevel) {
        this.busId = busId;
        this.routeId = routeId;
        this.lineId = lineId;
        this.targetStop = targetStop;
        this.estimatedTimeMinutes = estimatedTimeMinutes;
        this.distanceMeters = distanceMeters;
        this.calculatedAt = calculatedAt;
        this.estimatedArrival = calculatedAt.plusMinutes((long) estimatedTimeMinutes);
        this.confidenceLevel = confidenceLevel;
    }

    public int getBusId() {
        return busId;
    }

    public int getRouteId() {
        return routeId;
    }

    public int getLineId() {
        return lineId;
    }

    public Parada getTargetStop() {
        return targetStop;
    }

    public double getEstimatedTimeMinutes() {
        return estimatedTimeMinutes;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public LocalDateTime getEstimatedArrival() {
        return estimatedArrival;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public String getConfidenceLevel() {
        return confidenceLevel;
    }

    /**
     * Indica si el bus llegará pronto (menos de 5 minutos)
     */
    public boolean isArrivingSoon() {
        return estimatedTimeMinutes < 5.0;
    }

    /**
     * Indica si el ETA es confiable
     */
    public boolean isReliable() {
        return "Alta".equals(confidenceLevel);
    }

    @Override
    public String toString() {
        return String.format("ETA[Bus %d → %s: %.1f min (%.0f m), arr=%s, conf=%s]",
                busId, targetStop.getShortName(), estimatedTimeMinutes,
                distanceMeters, estimatedArrival.toLocalTime(), confidenceLevel);
    }

    /**
     * Genera una representación detallada del ETA
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════════════\n");
        sb.append(String.format("  ETA - Bus %d\n", busId));
        sb.append("═══════════════════════════════════════════════════════════\n");
        sb.append(String.format("Ruta: %d | Línea: %d\n", routeId, lineId));
        sb.append(String.format("Destino: %s\n", targetStop.getShortName()));
        sb.append(String.format("Tiempo estimado: %.1f minutos\n", estimatedTimeMinutes));
        sb.append(String.format("Distancia restante: %.0f metros (%.2f km)\n",
                distanceMeters, distanceMeters / 1000.0));
        sb.append(String.format("Hora estimada de llegada: %s\n",
                estimatedArrival.toLocalTime()));
        sb.append(String.format("Confiabilidad: %s\n", confidenceLevel));
        sb.append(String.format("Calculado a las: %s\n", calculatedAt.toLocalTime()));
        sb.append("═══════════════════════════════════════════════════════════\n");
        return sb.toString();
    }
}
