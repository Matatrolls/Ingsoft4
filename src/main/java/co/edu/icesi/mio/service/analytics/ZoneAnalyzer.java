package co.edu.icesi.mio.service.analytics;

import co.edu.icesi.mio.concurrency.Master;
import co.edu.icesi.mio.concurrency.zone.ZoneAnalysisResult;
import co.edu.icesi.mio.concurrency.zone.ZoneAnalysisWorker;
import co.edu.icesi.mio.infra.csv.GrafoMIO;
import co.edu.icesi.mio.model.analytics.Zone;
import co.edu.icesi.mio.repository.ArcVelocityRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para análisis de zonas geográficas usando patrón Master-Worker.
 * Analiza múltiples zonas de la ciudad en paralelo.
 */
public class ZoneAnalyzer {

    private final GrafoMIO grafo;
    private final ArcVelocityRepository velocityRepository;
    private final int numWorkers;

    public ZoneAnalyzer(GrafoMIO grafo, ArcVelocityRepository velocityRepository, int numWorkers) {
        this.grafo = grafo;
        this.velocityRepository = velocityRepository;
        this.numWorkers = numWorkers;
    }

    /**
     * Analiza múltiples zonas en paralelo usando Master-Worker
     *
     * @param zones Lista de zonas a analizar
     * @return Mapa de resultados por zona ID
     */
    public Map<String, ZoneAnalysisResult> analyzeZones(List<Zone> zones) throws InterruptedException {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  ANÁLISIS DE ZONAS GEOGRÁFICAS (PARALELO)");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.printf("Zonas a analizar: %d\n", zones.size());
        System.out.printf("Workers: %d\n", numWorkers);
        System.out.println();

        long startTime = System.currentTimeMillis();

        // Crear Master con workers que analizan zonas
        Master<Zone, ZoneAnalysisResult, Map<String, ZoneAnalysisResult>> master = new Master<>(
                numWorkers,
                workerId -> new ZoneAnalysisWorker(workerId, grafo, velocityRepository),
                this::aggregateResults
        );

        // Procesar todas las zonas
        Map<String, ZoneAnalysisResult> results = master.process(zones);

        // Shutdown master
        master.shutdown();

        long endTime = System.currentTimeMillis();
        double durationSeconds = (endTime - startTime) / 1000.0;

        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("  ANÁLISIS COMPLETADO");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.printf("Duración total: %.2f segundos\n", durationSeconds);
        System.out.printf("Zonas analizadas: %d\n", results.size());
        System.out.println();

        return results;
    }

    /**
     * Agrega los resultados de todos los workers en un mapa
     */
    private Map<String, ZoneAnalysisResult> aggregateResults(List<ZoneAnalysisResult> results) {
        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("  AGREGANDO RESULTADOS POR ZONA");
        System.out.println("═══════════════════════════════════════════════════════════\n");

        Map<String, ZoneAnalysisResult> resultMap = new HashMap<>();

        for (ZoneAnalysisResult result : results) {
            resultMap.put(result.getZone().getZoneId(), result);
            System.out.println("✓ " + result);
        }

        return resultMap;
    }

    /**
     * Genera un reporte comparativo de todas las zonas
     */
    public CityReport generateCityReport(Map<String, ZoneAnalysisResult> zoneResults) {
        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("  GENERANDO REPORTE DE LA CIUDAD");
        System.out.println("═══════════════════════════════════════════════════════════\n");

        int totalStops = zoneResults.values().stream()
                .mapToInt(ZoneAnalysisResult::getStopCount)
                .sum();

        int totalArcs = zoneResults.values().stream()
                .mapToInt(ZoneAnalysisResult::getArcCount)
                .sum();

        double avgCityVelocity = zoneResults.values().stream()
                .mapToDouble(ZoneAnalysisResult::getAverageVelocity)
                .average()
                .orElse(0.0);

        // Encontrar zona más congestionada
        ZoneAnalysisResult mostCongested = zoneResults.values().stream()
                .min(Comparator.comparingDouble(ZoneAnalysisResult::getAverageVelocity))
                .orElse(null);

        // Encontrar zona más fluida
        ZoneAnalysisResult leastCongested = zoneResults.values().stream()
                .max(Comparator.comparingDouble(ZoneAnalysisResult::getAverageVelocity))
                .orElse(null);

        // Encontrar zona con mejor cobertura
        ZoneAnalysisResult bestCoverage = zoneResults.values().stream()
                .max(Comparator.comparingInt(ZoneAnalysisResult::getStopCount))
                .orElse(null);

        // Contar zonas congestionadas
        long congestedZones = zoneResults.values().stream()
                .filter(ZoneAnalysisResult::isCongested)
                .count();

        return new CityReport(
                zoneResults.size(),
                totalStops,
                totalArcs,
                avgCityVelocity,
                mostCongested,
                leastCongested,
                bestCoverage,
                congestedZones
        );
    }

    /**
     * Clase para reporte consolidado de la ciudad
     */
    public static class CityReport {
        public final int totalZones;
        public final int totalStops;
        public final int totalArcs;
        public final double avgCityVelocity;
        public final ZoneAnalysisResult mostCongestedZone;
        public final ZoneAnalysisResult leastCongestedZone;
        public final ZoneAnalysisResult bestCoverageZone;
        public final long congestedZonesCount;

        public CityReport(int totalZones, int totalStops, int totalArcs,
                         double avgCityVelocity, ZoneAnalysisResult mostCongestedZone,
                         ZoneAnalysisResult leastCongestedZone, ZoneAnalysisResult bestCoverageZone,
                         long congestedZonesCount) {
            this.totalZones = totalZones;
            this.totalStops = totalStops;
            this.totalArcs = totalArcs;
            this.avgCityVelocity = avgCityVelocity;
            this.mostCongestedZone = mostCongestedZone;
            this.leastCongestedZone = leastCongestedZone;
            this.bestCoverageZone = bestCoverageZone;
            this.congestedZonesCount = congestedZonesCount;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("═══════════════════════════════════════════════════════════\n");
            sb.append("  REPORTE DE LA CIUDAD - ANÁLISIS COMPLETO\n");
            sb.append("═══════════════════════════════════════════════════════════\n");
            sb.append(String.format("Total de zonas analizadas: %d\n", totalZones));
            sb.append(String.format("Total de paradas:          %d\n", totalStops));
            sb.append(String.format("Total de arcos:            %d\n", totalArcs));
            sb.append(String.format("Velocidad promedio ciudad: %.1f km/h\n", avgCityVelocity));
            sb.append(String.format("Zonas congestionadas:      %d de %d (%.1f%%)\n",
                    congestedZonesCount, totalZones, (congestedZonesCount * 100.0 / totalZones)));
            sb.append("\n");

            if (mostCongestedZone != null) {
                sb.append(String.format("Zona MÁS congestionada:    %s (%.1f km/h)\n",
                        mostCongestedZone.getZone().getZoneName(),
                        mostCongestedZone.getAverageVelocity()));
            }

            if (leastCongestedZone != null) {
                sb.append(String.format("Zona MENOS congestionada:  %s (%.1f km/h)\n",
                        leastCongestedZone.getZone().getZoneName(),
                        leastCongestedZone.getAverageVelocity()));
            }

            if (bestCoverageZone != null) {
                sb.append(String.format("Zona con mejor cobertura:  %s (%d paradas)\n",
                        bestCoverageZone.getZone().getZoneName(),
                        bestCoverageZone.getStopCount()));
            }

            sb.append("═══════════════════════════════════════════════════════════\n");
            return sb.toString();
        }
    }
}
