package co.edu.icesi.mio.service.routing;

import co.edu.icesi.mio.concurrency.Master;
import co.edu.icesi.mio.concurrency.route.RouteCalculationResult;
import co.edu.icesi.mio.concurrency.route.RouteCalculationWorker;
import co.edu.icesi.mio.infra.csv.GrafoMIO;
import co.edu.icesi.mio.infra.csv.Parada;
import co.edu.icesi.mio.model.routing.CalculatedRoute;
import co.edu.icesi.mio.model.routing.RoutePair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para cálculo masivo de rutas usando patrón Master-Worker.
 * Calcula rutas entre múltiples pares de paradas en paralelo.
 */
public class MassiveRouteCalculator {

    private final RouteCalculatorService routeService;
    private final GrafoMIO grafo;
    private final int numWorkers;

    public MassiveRouteCalculator(RouteCalculatorService routeService, GrafoMIO grafo, int numWorkers) {
        this.routeService = routeService;
        this.grafo = grafo;
        this.numWorkers = numWorkers;
    }

    /**
     * Calcula rutas para una lista de pares origen-destino en paralelo
     *
     * @param routePairs Lista de pares a procesar
     * @return Mapa de resultados indexado por pair ID
     */
    public Map<String, RouteCalculationResult> calculateRoutes(List<RoutePair> routePairs)
            throws InterruptedException {

        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  CÁLCULO MASIVO DE RUTAS (PARALELO)");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.printf("Pares de rutas: %d\n", routePairs.size());
        System.out.printf("Workers: %d\n", numWorkers);
        System.out.println();

        long startTime = System.currentTimeMillis();

        // Crear Master con workers que calculan rutas
        Master<RoutePair, List<RouteCalculationResult>, Map<String, RouteCalculationResult>> master =
                new Master<>(
                        numWorkers,
                        workerId -> new RouteCalculationWorker(workerId, routeService),
                        this::aggregateResults
                );

        // Procesar todos los pares
        Map<String, RouteCalculationResult> results = master.process(routePairs);

        // Shutdown master
        master.shutdown();

        long endTime = System.currentTimeMillis();
        double durationSeconds = (endTime - startTime) / 1000.0;

        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("  CÁLCULO MASIVO COMPLETADO");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.printf("Duración total: %.2f segundos\n", durationSeconds);
        System.out.printf("Rutas calculadas: %d\n", results.size());
        System.out.printf("Rutas/segundo: %.1f\n", results.size() / durationSeconds);
        System.out.println();

        return results;
    }

    /**
     * Genera pares aleatorios de paradas para pruebas
     *
     * @param count Número de pares a generar
     * @return Lista de pares aleatorios
     */
    public List<RoutePair> generateRandomPairs(int count) {
        List<RoutePair> pairs = new ArrayList<>();
        List<Integer> stopIds = new ArrayList<>(grafo.getParadas().keySet());
        Random random = new Random();

        for (int i = 0; i < count; i++) {
            int originIdx = random.nextInt(stopIds.size());
            int destIdx = random.nextInt(stopIds.size());

            // Asegurar que origen y destino sean diferentes
            while (originIdx == destIdx) {
                destIdx = random.nextInt(stopIds.size());
            }

            int originId = stopIds.get(originIdx);
            int destId = stopIds.get(destIdx);

            pairs.add(new RoutePair(originId, destId, "Par aleatorio #" + (i + 1)));
        }

        return pairs;
    }

    /**
     * Genera pares para las rutas más populares (paradas con más conexiones)
     *
     * @param topN Número de paradas más populares a considerar
     * @return Lista de pares entre paradas populares
     */
    public List<RoutePair> generatePopularPairs(int topN) {
        // Obtener paradas con más conexiones
        List<Integer> popularStops = grafo.getParadas().keySet().stream()
                .sorted((a, b) -> {
                    int connectionsA = countConnections(a);
                    int connectionsB = countConnections(b);
                    return Integer.compare(connectionsB, connectionsA);
                })
                .limit(topN)
                .collect(Collectors.toList());

        List<RoutePair> pairs = new ArrayList<>();

        // Generar pares entre todas las paradas populares
        for (int i = 0; i < popularStops.size(); i++) {
            for (int j = i + 1; j < popularStops.size(); j++) {
                int originId = popularStops.get(i);
                int destId = popularStops.get(j);

                Parada origin = grafo.getParadas().get(originId);
                Parada dest = grafo.getParadas().get(destId);

                String desc = String.format("%s -> %s", origin.getShortName(), dest.getShortName());
                pairs.add(new RoutePair(originId, destId, desc));
            }
        }

        return pairs;
    }

    /**
     * Cuenta las conexiones (arcos) que tiene una parada
     */
    private int countConnections(int stopId) {
        return (int) grafo.getArcos().stream()
                .filter(arco -> arco.getParadaOrigen().getStopId() == stopId ||
                               arco.getParadaDestino().getStopId() == stopId)
                .count();
    }

    /**
     * Agrega los resultados de todos los workers en un mapa
     */
    private Map<String, RouteCalculationResult> aggregateResults(
            List<List<RouteCalculationResult>> workerResults) {

        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("  AGREGANDO RESULTADOS");
        System.out.println("═══════════════════════════════════════════════════════════");

        Map<String, RouteCalculationResult> allResults = new HashMap<>();
        int successCount = 0;
        int failureCount = 0;
        long totalCalcTime = 0;

        for (List<RouteCalculationResult> results : workerResults) {
            for (RouteCalculationResult result : results) {
                allResults.put(result.getRoutePair().getPairId(), result);
                totalCalcTime += result.getCalculationTimeMs();

                if (result.isSuccess()) {
                    successCount++;
                } else {
                    failureCount++;
                }
            }
        }

        double avgCalcTime = allResults.isEmpty() ? 0 : (totalCalcTime / (double) allResults.size());

        System.out.printf("✓ Rutas exitosas: %d\n", successCount);
        System.out.printf("✗ Rutas fallidas: %d\n", failureCount);
        System.out.printf("⏱ Tiempo promedio/ruta: %.1f ms\n", avgCalcTime);
        System.out.println();

        return allResults;
    }

    /**
     * Genera estadísticas de las rutas calculadas
     */
    public RouteStatistics generateStatistics(Map<String, RouteCalculationResult> results) {
        List<CalculatedRoute> successfulRoutes = results.values().stream()
                .filter(RouteCalculationResult::isSuccess)
                .map(RouteCalculationResult::getRoute)
                .collect(Collectors.toList());

        if (successfulRoutes.isEmpty()) {
            return new RouteStatistics(0, 0, 0, 0, 0, 0, 0, 0);
        }

        double avgTime = successfulRoutes.stream()
                .mapToDouble(CalculatedRoute::getTotalTime)
                .average().orElse(0);

        double avgDistance = successfulRoutes.stream()
                .mapToDouble(CalculatedRoute::getTotalDistance)
                .average().orElse(0);

        double avgTransfers = successfulRoutes.stream()
                .mapToInt(CalculatedRoute::getTransferCount)
                .average().orElse(0);

        double avgSegments = successfulRoutes.stream()
                .mapToInt(CalculatedRoute::getSegmentCount)
                .average().orElse(0);

        double maxTime = successfulRoutes.stream()
                .mapToDouble(CalculatedRoute::getTotalTime)
                .max().orElse(0);

        double maxDistance = successfulRoutes.stream()
                .mapToDouble(CalculatedRoute::getTotalDistance)
                .max().orElse(0);

        int maxTransfers = successfulRoutes.stream()
                .mapToInt(CalculatedRoute::getTransferCount)
                .max().orElse(0);

        int maxSegments = successfulRoutes.stream()
                .mapToInt(CalculatedRoute::getSegmentCount)
                .max().orElse(0);

        return new RouteStatistics(avgTime, avgDistance, avgTransfers, avgSegments,
                                   maxTime, maxDistance, maxTransfers, maxSegments);
    }

    /**
     * Clase para almacenar estadísticas de rutas
     */
    public static class RouteStatistics {
        public final double avgTime;
        public final double avgDistance;
        public final double avgTransfers;
        public final double avgSegments;
        public final double maxTime;
        public final double maxDistance;
        public final int maxTransfers;
        public final int maxSegments;

        public RouteStatistics(double avgTime, double avgDistance, double avgTransfers,
                              double avgSegments, double maxTime, double maxDistance,
                              int maxTransfers, int maxSegments) {
            this.avgTime = avgTime;
            this.avgDistance = avgDistance;
            this.avgTransfers = avgTransfers;
            this.avgSegments = avgSegments;
            this.maxTime = maxTime;
            this.maxDistance = maxDistance;
            this.maxTransfers = maxTransfers;
            this.maxSegments = maxSegments;
        }

        @Override
        public String toString() {
            return String.format(
                    "Estadísticas de Rutas:\n" +
                    "  Tiempo promedio:      %.1f min (máx: %.1f min)\n" +
                    "  Distancia promedio:   %.0f m (máx: %.0f m)\n" +
                    "  Transbordos promedio: %.1f (máx: %d)\n" +
                    "  Segmentos promedio:   %.1f (máx: %d)",
                    avgTime, maxTime, avgDistance, maxDistance,
                    avgTransfers, maxTransfers, avgSegments, maxSegments
            );
        }
    }
}
