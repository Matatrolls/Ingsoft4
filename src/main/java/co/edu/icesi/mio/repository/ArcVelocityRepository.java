package co.edu.icesi.mio.repository;

import co.edu.icesi.mio.model.analytics.ArcIdentifier;
import co.edu.icesi.mio.model.analytics.ArcVelocityStats;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repositorio en memoria para almacenar y consultar estadísticas de velocidad de arcos.
 * Thread-safe para uso concurrente.
 */
public class ArcVelocityRepository {

    private final Map<ArcIdentifier, ArcVelocityStats> statsMap;

    public ArcVelocityRepository() {
        this.statsMap = new ConcurrentHashMap<>();
    }

    /**
     * Guarda las estadísticas de un arco
     */
    public void save(ArcVelocityStats stats) {
        statsMap.put(stats.getArcId(), stats);
    }

    /**
     * Guarda múltiples estadísticas
     */
    public void saveAll(Map<ArcIdentifier, ArcVelocityStats> stats) {
        statsMap.putAll(stats);
    }

    /**
     * Obtiene las estadísticas de un arco específico
     */
    public Optional<ArcVelocityStats> findByArc(ArcIdentifier arcId) {
        return Optional.ofNullable(statsMap.get(arcId));
    }

    /**
     * Obtiene las estadísticas de una ruta específica
     */
    public List<ArcVelocityStats> findByRoute(int routeId) {
        return statsMap.values().stream()
                .filter(stats -> stats.getArcId().getRouteId() == routeId)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene las estadísticas de una línea específica
     */
    public List<ArcVelocityStats> findByLine(int lineId) {
        return statsMap.values().stream()
                .filter(stats -> stats.getArcId().getLineId() == lineId)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene todos los arcos almacenados
     */
    public List<ArcVelocityStats> findAll() {
        return new ArrayList<>(statsMap.values());
    }

    /**
     * Obtiene solo los arcos con estadísticas confiables
     */
    public List<ArcVelocityStats> findReliableArcs() {
        return statsMap.values().stream()
                .filter(ArcVelocityStats::isReliable)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los N arcos más rápidos
     */
    public List<ArcVelocityStats> findFastestArcs(int limit) {
        return statsMap.values().stream()
                .filter(ArcVelocityStats::isReliable)
                .sorted(Comparator.comparingDouble(ArcVelocityStats::getTypicalVelocity).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los N arcos más lentos
     */
    public List<ArcVelocityStats> findSlowestArcs(int limit) {
        return statsMap.values().stream()
                .filter(ArcVelocityStats::isReliable)
                .sorted(Comparator.comparingDouble(ArcVelocityStats::getTypicalVelocity))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los arcos con mayor variabilidad (desviación estándar alta)
     */
    public List<ArcVelocityStats> findMostVariableArcs(int limit) {
        return statsMap.values().stream()
                .filter(ArcVelocityStats::isReliable)
                .sorted(Comparator.comparingDouble(ArcVelocityStats::getStdDeviation).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene estadísticas agregadas del repositorio
     */
    public RepositoryStats getStats() {
        int totalArcs = statsMap.size();
        int reliableArcs = (int) statsMap.values().stream()
                .filter(ArcVelocityStats::isReliable)
                .count();

        long totalSamples = statsMap.values().stream()
                .mapToLong(ArcVelocityStats::getSampleCount)
                .sum();

        double avgVelocity = statsMap.values().stream()
                .filter(ArcVelocityStats::isReliable)
                .mapToDouble(ArcVelocityStats::getAverageVelocity)
                .average()
                .orElse(0.0);

        return new RepositoryStats(totalArcs, reliableArcs, totalSamples, avgVelocity);
    }

    /**
     * Limpia todos los datos
     */
    public void clear() {
        statsMap.clear();
    }

    /**
     * Retorna el número de arcos almacenados
     */
    public int size() {
        return statsMap.size();
    }

    /**
     * Clase para estadísticas del repositorio
     */
    public static class RepositoryStats {
        private final int totalArcs;
        private final int reliableArcs;
        private final long totalSamples;
        private final double avgVelocity;

        public RepositoryStats(int totalArcs, int reliableArcs, long totalSamples, double avgVelocity) {
            this.totalArcs = totalArcs;
            this.reliableArcs = reliableArcs;
            this.totalSamples = totalSamples;
            this.avgVelocity = avgVelocity;
        }

        public int getTotalArcs() {
            return totalArcs;
        }

        public int getReliableArcs() {
            return reliableArcs;
        }

        public long getTotalSamples() {
            return totalSamples;
        }

        public double getAvgVelocity() {
            return avgVelocity;
        }

        @Override
        public String toString() {
            return String.format(
                    "Estadísticas del Repositorio:\n" +
                    "  Total de arcos:        %,d\n" +
                    "  Arcos confiables:      %,d (%.1f%%)\n" +
                    "  Total de muestras:     %,d\n" +
                    "  Velocidad promedio:    %.1f km/h",
                    totalArcs, reliableArcs, (reliableArcs * 100.0 / totalArcs),
                    totalSamples, avgVelocity
            );
        }
    }
}
