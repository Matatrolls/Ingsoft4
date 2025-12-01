package co.edu.icesi.mio.model.routing;

import co.edu.icesi.mio.infra.csv.Arco;
import co.edu.icesi.mio.infra.csv.Parada;

/**
 * Representa un segmento individual dentro de una ruta calculada.
 * Un segmento es un arco específico con información de costo y tiempo.
 */
public class RouteSegment {

    private final Arco arco;
    private final double estimatedTime;    // Tiempo estimado en minutos
    private final double distance;         // Distancia en metros
    private final double velocity;         // Velocidad estimada en km/h

    public RouteSegment(Arco arco, double estimatedTime, double distance, double velocity) {
        this.arco = arco;
        this.estimatedTime = estimatedTime;
        this.distance = distance;
        this.velocity = velocity;
    }

    public Arco getArco() {
        return arco;
    }

    public Parada getOrigin() {
        return arco.getParadaOrigen();
    }

    public Parada getDestination() {
        return arco.getParadaDestino();
    }

    public int getLineId() {
        return arco.getLineId();
    }

    public String getLineName() {
        return arco.getLineShortName();
    }

    public double getEstimatedTime() {
        return estimatedTime;
    }

    public double getDistance() {
        return distance;
    }

    public double getVelocity() {
        return velocity;
    }

    @Override
    public String toString() {
        return String.format("Segmento[%s → %s, Línea: %s, Tiempo: %.1f min, Distancia: %.0f m]",
                getOrigin().getShortName(),
                getDestination().getShortName(),
                getLineName(),
                estimatedTime,
                distance);
    }
}
