package co.edu.icesi.mio.model.analytics;

import java.util.Objects;

/**
 * Identifica de manera única un arco en el grafo del MIO.
 * Un arco representa un segmento entre dos puntos/paradas en una ruta específica.
 */
public class ArcIdentifier {

    private final int routeId;      // ID de la ruta
    private final int lineId;       // ID de la línea
    private final int originStopId; // Parada de origen (puede ser -1 si no está identificada)
    private final int destStopId;   // Parada de destino (puede ser -1 si no está identificada)

    public ArcIdentifier(int routeId, int lineId, int originStopId, int destStopId) {
        this.routeId = routeId;
        this.lineId = lineId;
        this.originStopId = originStopId;
        this.destStopId = destStopId;
    }

    /**
     * Crea un identificador de arco simplificado usando solo ruta y línea
     */
    public static ArcIdentifier forRoute(int routeId, int lineId) {
        return new ArcIdentifier(routeId, lineId, -1, -1);
    }

    /**
     * Crea un identificador de arco con paradas específicas
     */
    public static ArcIdentifier forStops(int routeId, int lineId, int originStopId, int destStopId) {
        return new ArcIdentifier(routeId, lineId, originStopId, destStopId);
    }

    // Getters
    public int getRouteId() {
        return routeId;
    }

    public int getLineId() {
        return lineId;
    }

    public int getOriginStopId() {
        return originStopId;
    }

    public int getDestStopId() {
        return destStopId;
    }

    public boolean hasStops() {
        return originStopId >= 0 && destStopId >= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArcIdentifier that = (ArcIdentifier) o;
        return routeId == that.routeId &&
                lineId == that.lineId &&
                originStopId == that.originStopId &&
                destStopId == that.destStopId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeId, lineId, originStopId, destStopId);
    }

    @Override
    public String toString() {
        if (hasStops()) {
            return String.format("Arc[route=%d, line=%d, %d->%d]",
                    routeId, lineId, originStopId, destStopId);
        } else {
            return String.format("Arc[route=%d, line=%d]", routeId, lineId);
        }
    }
}
