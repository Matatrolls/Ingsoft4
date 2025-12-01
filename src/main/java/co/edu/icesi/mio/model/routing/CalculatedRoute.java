package co.edu.icesi.mio.model.routing;

import co.edu.icesi.mio.infra.csv.Parada;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa una ruta completa calculada entre dos paradas.
 * Incluye todos los segmentos, tiempos, distancias y estadísticas.
 */
public class CalculatedRoute {

    private final Parada origin;
    private final Parada destination;
    private final List<RouteSegment> segments;
    private final double totalTime;        // Tiempo total en minutos
    private final double totalDistance;    // Distancia total en metros
    private final int transferCount;       // Número de transbordos

    public CalculatedRoute(Parada origin, Parada destination, List<RouteSegment> segments) {
        this.origin = origin;
        this.destination = destination;
        this.segments = new ArrayList<>(segments);
        this.totalTime = calculateTotalTime();
        this.totalDistance = calculateTotalDistance();
        this.transferCount = calculateTransferCount();
    }

    private double calculateTotalTime() {
        return segments.stream()
                .mapToDouble(RouteSegment::getEstimatedTime)
                .sum();
    }

    private double calculateTotalDistance() {
        return segments.stream()
                .mapToDouble(RouteSegment::getDistance)
                .sum();
    }

    private int calculateTransferCount() {
        if (segments.isEmpty()) return 0;

        int transfers = 0;
        for (int i = 1; i < segments.size(); i++) {
            RouteSegment prev = segments.get(i - 1);
            RouteSegment current = segments.get(i);

            // Si cambia la línea, es un transbordo
            if (prev.getLineId() != current.getLineId()) {
                transfers++;
            }
        }
        return transfers;
    }

    public Parada getOrigin() {
        return origin;
    }

    public Parada getDestination() {
        return destination;
    }

    public List<RouteSegment> getSegments() {
        return new ArrayList<>(segments);
    }

    public int getSegmentCount() {
        return segments.size();
    }

    public double getTotalTime() {
        return totalTime;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public int getTransferCount() {
        return transferCount;
    }

    public double getAverageVelocity() {
        if (totalTime == 0) return 0;
        // Convertir: (metros / minutos) * 60 / 1000 = km/h
        return (totalDistance / totalTime) * 60.0 / 1000.0;
    }

    /**
     * Obtiene la lista de paradas en orden (incluyendo origen y destino)
     */
    public List<Parada> getStopsInOrder() {
        List<Parada> stops = new ArrayList<>();
        if (segments.isEmpty()) {
            return stops;
        }

        // Primera parada
        stops.add(segments.get(0).getOrigin());

        // Paradas intermedias y destino
        for (RouteSegment segment : segments) {
            stops.add(segment.getDestination());
        }

        return stops;
    }

    /**
     * Obtiene las líneas usadas en orden
     */
    public List<String> getLinesUsed() {
        List<String> lines = new ArrayList<>();
        if (segments.isEmpty()) return lines;

        String currentLine = segments.get(0).getLineName();
        lines.add(currentLine);

        for (int i = 1; i < segments.size(); i++) {
            String lineName = segments.get(i).getLineName();
            if (!lineName.equals(currentLine)) {
                lines.add(lineName);
                currentLine = lineName;
            }
        }

        return lines;
    }

    /**
     * Indica si la ruta fue encontrada
     */
    public boolean isFound() {
        return !segments.isEmpty();
    }

    @Override
    public String toString() {
        if (!isFound()) {
            return String.format("Ruta NO encontrada de %s a %s",
                    origin.getShortName(), destination.getShortName());
        }

        return String.format("Ruta[%s → %s, Tiempo: %.1f min, Distancia: %.0f m, Segmentos: %d, Transbordos: %d, Líneas: %s]",
                origin.getShortName(),
                destination.getShortName(),
                totalTime,
                totalDistance,
                segments.size(),
                transferCount,
                getLinesUsed());
    }

    /**
     * Genera un resumen detallado de la ruta
     */
    public String toDetailedString() {
        if (!isFound()) {
            return toString();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════════════\n");
        sb.append(String.format("  RUTA: %s → %s\n", origin.getShortName(), destination.getShortName()));
        sb.append("═══════════════════════════════════════════════════════════\n");
        sb.append(String.format("Tiempo total: %.1f minutos\n", totalTime));
        sb.append(String.format("Distancia total: %.0f metros (%.2f km)\n", totalDistance, totalDistance / 1000.0));
        sb.append(String.format("Velocidad promedio: %.1f km/h\n", getAverageVelocity()));
        sb.append(String.format("Número de paradas: %d\n", getStopsInOrder().size()));
        sb.append(String.format("Número de transbordos: %d\n", transferCount));
        sb.append(String.format("Líneas utilizadas: %s\n", getLinesUsed()));
        sb.append("\nDetalle de segmentos:\n");
        sb.append("─".repeat(80)).append("\n");

        for (int i = 0; i < segments.size(); i++) {
            RouteSegment seg = segments.get(i);
            sb.append(String.format("%3d. %s\n", i + 1, seg));
        }

        sb.append("═".repeat(80)).append("\n");
        return sb.toString();
    }
}
