package co.edu.icesi.mio.service.routing;

import co.edu.icesi.mio.infra.csv.Arco;
import co.edu.icesi.mio.infra.csv.GrafoMIO;
import co.edu.icesi.mio.infra.csv.Parada;
import co.edu.icesi.mio.model.analytics.ArcIdentifier;
import co.edu.icesi.mio.model.analytics.ArcVelocityStats;
import co.edu.icesi.mio.model.routing.CalculatedRoute;
import co.edu.icesi.mio.model.routing.RouteSegment;
import co.edu.icesi.mio.repository.ArcVelocityRepository;

/**
 * Servicio principal para cálculo de rutas óptimas.
 * Integra el grafo del MIO con las estadísticas de velocidad calculadas.
 */
public class RouteCalculatorService {

    private final GrafoMIO grafo;
    private final ArcVelocityRepository velocityRepository;

    // Constantes
    private static final double DEFAULT_VELOCITY_KMH = 25.0; // Velocidad por defecto si no hay datos
    private static final double TRANSFER_PENALTY_MINUTES = 3.0; // Penalización por transbordo

    public RouteCalculatorService(GrafoMIO grafo, ArcVelocityRepository velocityRepository) {
        this.grafo = grafo;
        this.velocityRepository = velocityRepository;
    }

    /**
     * Calcula la ruta más rápida (minimiza tiempo de viaje)
     */
    public CalculatedRoute calculateFastestRoute(int originId, int destinationId) {
        DijkstraPathfinder pathfinder = new DijkstraPathfinder(grafo, new TimeCostStrategy());
        return pathfinder.findRoute(originId, destinationId);
    }

    /**
     * Calcula la ruta más corta (minimiza distancia)
     */
    public CalculatedRoute calculateShortestRoute(int originId, int destinationId) {
        DijkstraPathfinder pathfinder = new DijkstraPathfinder(grafo, new DistanceCostStrategy());
        return pathfinder.findRoute(originId, destinationId);
    }

    /**
     * Calcula la ruta con menos transbordos
     */
    public CalculatedRoute calculateFewestTransfersRoute(int originId, int destinationId) {
        DijkstraPathfinder pathfinder = new DijkstraPathfinder(grafo,
                new TimeCostWithTransferPenaltyStrategy());
        return pathfinder.findRoute(originId, destinationId);
    }

    /**
     * Calcula distancia euclidiana entre dos paradas (en metros)
     */
    private double calculateDistance(Parada p1, Parada p2) {
        // Fórmula de Haversine simplificada para distancias cortas
        double lat1 = p1.getDecimalLatitude();
        double lon1 = p1.getDecimalLongitude();
        double lat2 = p2.getDecimalLatitude();
        double lon2 = p2.getDecimalLongitude();

        double R = 6371000; // Radio de la Tierra en metros

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Obtiene la velocidad estimada para un arco (usando estadísticas o valor por defecto)
     */
    private double getArcVelocity(Arco arco) {
        // Identificar arco por paradas específicas para mayor precisión
        ArcIdentifier arcId = ArcIdentifier.forStops(
                arco.getLineId(),
                arco.getLineId(),
                arco.getParadaOrigen().getStopId(),
                arco.getParadaDestino().getStopId()
        );

        return velocityRepository.findByArc(arcId)
                .map(ArcVelocityStats::getTypicalVelocity)
                .orElse(DEFAULT_VELOCITY_KMH);
    }

    /**
     * Estrategia basada en tiempo (usa velocidades históricas)
     */
    private class TimeCostStrategy implements DijkstraPathfinder.CostStrategy {
        @Override
        public double calculateCost(Arco arco) {
            double distance = calculateDistance(arco.getParadaOrigen(), arco.getParadaDestino());
            double velocity = getArcVelocity(arco);

            // Tiempo = distancia / velocidad
            // Convertir: (metros / (km/h)) → minutos
            return (distance / 1000.0) / velocity * 60.0;
        }

        @Override
        public RouteSegment createSegment(Arco arco) {
            double distance = calculateDistance(arco.getParadaOrigen(), arco.getParadaDestino());
            double velocity = getArcVelocity(arco);
            double time = (distance / 1000.0) / velocity * 60.0;

            return new RouteSegment(arco, time, distance, velocity);
        }
    }

    /**
     * Estrategia basada en distancia
     */
    private class DistanceCostStrategy implements DijkstraPathfinder.CostStrategy {
        @Override
        public double calculateCost(Arco arco) {
            return calculateDistance(arco.getParadaOrigen(), arco.getParadaDestino());
        }

        @Override
        public RouteSegment createSegment(Arco arco) {
            double distance = calculateDistance(arco.getParadaOrigen(), arco.getParadaDestino());
            double velocity = getArcVelocity(arco);
            double time = (distance / 1000.0) / velocity * 60.0;

            return new RouteSegment(arco, time, distance, velocity);
        }
    }

    /**
     * Estrategia basada en tiempo con penalización por transbordos
     */
    private class TimeCostWithTransferPenaltyStrategy implements DijkstraPathfinder.CostStrategy {
        private int previousLineId = -1;

        @Override
        public double calculateCost(Arco arco) {
            double distance = calculateDistance(arco.getParadaOrigen(), arco.getParadaDestino());
            double velocity = getArcVelocity(arco);
            double timeCost = (distance / 1000.0) / velocity * 60.0;

            // Agregar penalización si cambia de línea
            if (previousLineId != -1 && previousLineId != arco.getLineId()) {
                timeCost += TRANSFER_PENALTY_MINUTES;
            }

            previousLineId = arco.getLineId();
            return timeCost;
        }

        @Override
        public RouteSegment createSegment(Arco arco) {
            double distance = calculateDistance(arco.getParadaOrigen(), arco.getParadaDestino());
            double velocity = getArcVelocity(arco);
            double time = (distance / 1000.0) / velocity * 60.0;

            return new RouteSegment(arco, time, distance, velocity);
        }
    }

    /**
     * Información sobre el servicio
     */
    public ServiceInfo getServiceInfo() {
        int totalStops = grafo.getParadas().size();
        int totalLines = grafo.getRutas().size();
        int totalArcs = grafo.getArcos().size();
        int arcsWithVelocityData = velocityRepository.size();

        return new ServiceInfo(totalStops, totalLines, totalArcs, arcsWithVelocityData);
    }

    /**
     * Clase para información del servicio
     */
    public static class ServiceInfo {
        private final int totalStops;
        private final int totalLines;
        private final int totalArcs;
        private final int arcsWithVelocityData;

        public ServiceInfo(int totalStops, int totalLines, int totalArcs, int arcsWithVelocityData) {
            this.totalStops = totalStops;
            this.totalLines = totalLines;
            this.totalArcs = totalArcs;
            this.arcsWithVelocityData = arcsWithVelocityData;
        }

        @Override
        public String toString() {
            return String.format(
                    "Servicio de Cálculo de Rutas:\n" +
                            "  Paradas totales: %,d\n" +
                            "  Líneas totales: %,d\n" +
                            "  Arcos totales: %,d\n" +
                            "  Arcos con datos de velocidad: %,d (%.1f%%)",
                    totalStops, totalLines, totalArcs, arcsWithVelocityData,
                    (arcsWithVelocityData * 100.0 / Math.max(totalArcs, 1))
            );
        }
    }
}
