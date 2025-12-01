package co.edu.icesi.mio.service.realtime;

import co.edu.icesi.mio.infra.csv.Arco;
import co.edu.icesi.mio.infra.csv.GrafoMIO;
import co.edu.icesi.mio.infra.csv.Parada;
import co.edu.icesi.mio.model.analytics.ArcIdentifier;
import co.edu.icesi.mio.model.analytics.ArcVelocityStats;
import co.edu.icesi.mio.model.realtime.BusETA;
import co.edu.icesi.mio.model.realtime.BusPosition;
import co.edu.icesi.mio.repository.ArcVelocityRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para calcular tiempos estimados de llegada (ETA) de buses.
 * Usa velocidades históricas de arcos y posición actual del bus.
 */
public class ETACalculatorService {

    private static final double DEFAULT_VELOCITY_KMH = 20.0; // Velocidad por defecto
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final GrafoMIO grafo;
    private final ArcVelocityRepository velocityRepository;

    public ETACalculatorService(GrafoMIO grafo, ArcVelocityRepository velocityRepository) {
        this.grafo = grafo;
        this.velocityRepository = velocityRepository;
    }

    /**
     * Calcula el ETA para un bus hacia su próxima parada
     *
     * @param busPosition Posición actual del bus
     * @return ETA calculado
     */
    public BusETA calculateETA(BusPosition busPosition) {
        Parada nextStop = grafo.getParadas().get(busPosition.getNextStopId());

        if (nextStop == null) {
            throw new IllegalArgumentException("Parada destino no encontrada: " + busPosition.getNextStopId());
        }

        // Calcular distancia directa desde posición actual a próxima parada
        double distanceToNextStop = calculateHaversineDistance(
                busPosition.getLatitude(),
                busPosition.getLongitude(),
                nextStop.getDecimalLatitude(),
                nextStop.getDecimalLongitude()
        );

        // Obtener velocidad histórica del arco
        ArcIdentifier arcId = ArcIdentifier.forRoute(busPosition.getRouteId(), busPosition.getLineId());
        double expectedVelocity = getExpectedVelocity(arcId, busPosition);

        // Calcular tiempo estimado
        double estimatedTimeMinutes = (distanceToNextStop / expectedVelocity) * 60.0;

        // Determinar nivel de confianza
        String confidenceLevel = determineConfidenceLevel(arcId, busPosition);

        return new BusETA(
                busPosition.getBusId(),
                busPosition.getRouteId(),
                busPosition.getLineId(),
                nextStop,
                estimatedTimeMinutes,
                distanceToNextStop * 1000, // Convertir a metros
                LocalDateTime.now(),
                confidenceLevel
        );
    }

    /**
     * Calcula ETAs para todas las paradas futuras en la ruta del bus
     *
     * @param busPosition Posición actual del bus
     * @param maxStops Máximo número de paradas futuras a calcular
     * @return Lista de ETAs
     */
    public List<BusETA> calculateMultipleETAs(BusPosition busPosition, int maxStops) {
        List<BusETA> etas = new ArrayList<>();

        // Encontrar la secuencia de paradas en la ruta del bus
        List<Arco> routeArcs = findRouteArcs(busPosition);

        if (routeArcs.isEmpty()) {
            return etas;
        }

        double cumulativeTime = 0.0;
        double cumulativeDistance = 0.0;
        LocalDateTime currentTime = LocalDateTime.now();

        // Distancia y tiempo hasta la primera parada
        Parada firstStop = grafo.getParadas().get(busPosition.getNextStopId());
        if (firstStop != null) {
            double distanceToFirst = calculateHaversineDistance(
                    busPosition.getLatitude(),
                    busPosition.getLongitude(),
                    firstStop.getDecimalLatitude(),
                    firstStop.getDecimalLongitude()
            );

            ArcIdentifier arcId = ArcIdentifier.forRoute(busPosition.getRouteId(), busPosition.getLineId());
            double velocity = getExpectedVelocity(arcId, busPosition);
            cumulativeTime = (distanceToFirst / velocity) * 60.0;
            cumulativeDistance = distanceToFirst * 1000;

            String confidence = determineConfidenceLevel(arcId, busPosition);
            etas.add(new BusETA(
                    busPosition.getBusId(),
                    busPosition.getRouteId(),
                    busPosition.getLineId(),
                    firstStop,
                    cumulativeTime,
                    cumulativeDistance,
                    currentTime,
                    confidence
            ));
        }

        // Calcular ETAs para paradas subsecuentes
        int count = 1;
        for (Arco arco : routeArcs) {
            if (count >= maxStops) break;

            // Solo considerar arcos después de la próxima parada
            if (arco.getParadaOrigen().getStopId() < busPosition.getNextStopId()) {
                continue;
            }

            Parada destination = arco.getParadaDestino();
            double arcDistance = calculateHaversineDistance(
                    arco.getParadaOrigen().getDecimalLatitude(),
                    arco.getParadaOrigen().getDecimalLongitude(),
                    destination.getDecimalLatitude(),
                    destination.getDecimalLongitude()
            );

            ArcIdentifier arcId = ArcIdentifier.forRoute(busPosition.getRouteId(), busPosition.getLineId());
            double velocity = getExpectedVelocity(arcId, busPosition);
            double arcTime = (arcDistance / velocity) * 60.0;

            cumulativeTime += arcTime;
            cumulativeDistance += arcDistance * 1000;

            String confidence = determineConfidenceLevel(arcId, busPosition);
            etas.add(new BusETA(
                    busPosition.getBusId(),
                    busPosition.getRouteId(),
                    busPosition.getLineId(),
                    destination,
                    cumulativeTime,
                    cumulativeDistance,
                    currentTime,
                    confidence
            ));

            count++;
        }

        return etas;
    }

    /**
     * Obtiene la velocidad esperada para un arco, considerando datos históricos y velocidad actual
     */
    private double getExpectedVelocity(ArcIdentifier arcId, BusPosition busPosition) {
        ArcVelocityStats stats = velocityRepository.findByArc(arcId).orElse(null);

        if (stats != null && stats.isReliable()) {
            // Si tenemos datos confiables, usar mediana histórica
            // Pero ajustar según velocidad actual si el bus está en movimiento
            double historicalVelocity = stats.getMedianVelocity();

            if (busPosition.isMoving() && busPosition.getVelocity() > 0) {
                // Promedio ponderado: 70% histórico, 30% actual
                return (historicalVelocity * 0.7) + (busPosition.getVelocity() * 0.3);
            }

            return historicalVelocity;
        }

        // Sin datos históricos, usar velocidad actual o default
        if (busPosition.isMoving() && busPosition.getVelocity() > 0) {
            return busPosition.getVelocity();
        }

        return DEFAULT_VELOCITY_KMH;
    }

    /**
     * Determina el nivel de confianza del ETA
     */
    private String determineConfidenceLevel(ArcIdentifier arcId, BusPosition busPosition) {
        ArcVelocityStats stats = velocityRepository.findByArc(arcId).orElse(null);

        if (stats == null || !stats.isReliable()) {
            return "Baja";
        }

        // Confianza alta si hay muchas muestras y baja variabilidad
        if (stats.getSampleCount() >= 100 && stats.getStdDeviation() < 10) {
            return "Alta";
        }

        // Confianza media si hay datos pero con más variabilidad
        if (stats.getSampleCount() >= 30) {
            return "Media";
        }

        return "Baja";
    }

    /**
     * Encuentra los arcos de la ruta en la que viaja el bus
     */
    private List<Arco> findRouteArcs(BusPosition busPosition) {
        List<Arco> routeArcs = new ArrayList<>();

        for (Arco arco : grafo.getArcos()) {
            if (arco.getLineId() == busPosition.getLineId()) {
                routeArcs.add(arco);
            }
        }

        // Ordenar por secuencia si es posible
        routeArcs.sort((a1, a2) -> Integer.compare(
                a1.getParadaOrigen().getStopId(),
                a2.getParadaOrigen().getStopId()
        ));

        return routeArcs;
    }

    /**
     * Calcula la distancia entre dos puntos GPS usando la fórmula de Haversine
     *
     * @return Distancia en kilómetros
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Obtiene información del servicio
     */
    public String getServiceInfo() {
        return String.format(
                "ETACalculatorService[\n" +
                "  Paradas en grafo: %d\n" +
                "  Arcos en grafo: %d\n" +
                "  Estadísticas de velocidad: %d arcos\n" +
                "  Velocidad por defecto: %.1f km/h\n" +
                "]",
                grafo.getParadas().size(),
                grafo.getArcos().size(),
                velocityRepository.size(),
                DEFAULT_VELOCITY_KMH
        );
    }
}
