package co.edu.icesi.mio.model.routing;

/**
 * Representa un par de paradas (origen-destino) para cálculo de rutas.
 * Usado en procesamiento masivo de rutas.
 */
public class RoutePair {

    private final int originStopId;
    private final int destinationStopId;
    private final String description;

    public RoutePair(int originStopId, int destinationStopId) {
        this(originStopId, destinationStopId, null);
    }

    public RoutePair(int originStopId, int destinationStopId, String description) {
        this.originStopId = originStopId;
        this.destinationStopId = destinationStopId;
        this.description = description;
    }

    public int getOriginStopId() {
        return originStopId;
    }

    public int getDestinationStopId() {
        return destinationStopId;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Crea un identificador único para este par de paradas
     */
    public String getPairId() {
        return originStopId + "->" + destinationStopId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoutePair routePair = (RoutePair) o;
        return originStopId == routePair.originStopId &&
               destinationStopId == routePair.destinationStopId;
    }

    @Override
    public int hashCode() {
        return 31 * originStopId + destinationStopId;
    }

    @Override
    public String toString() {
        if (description != null) {
            return String.format("RoutePair[%d -> %d, '%s']",
                    originStopId, destinationStopId, description);
        }
        return String.format("RoutePair[%d -> %d]", originStopId, destinationStopId);
    }
}
