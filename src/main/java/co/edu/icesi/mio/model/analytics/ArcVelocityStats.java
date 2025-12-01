package co.edu.icesi.mio.model.analytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Estadísticas de velocidad para un arco específico del sistema MIO.
 * Incluye métricas calculadas a partir de datos históricos.
 */
public class ArcVelocityStats {

    private final ArcIdentifier arcId;
    private final int sampleCount;          // Número de muestras
    private final double averageVelocity;   // Velocidad promedio
    private final double medianVelocity;    // Velocidad mediana
    private final double minVelocity;       // Velocidad mínima observada
    private final double maxVelocity;       // Velocidad máxima observada
    private final double stdDeviation;      // Desviación estándar
    private final double percentile90;      // Percentil 90
    private final double percentile95;      // Percentil 95

    public ArcVelocityStats(ArcIdentifier arcId, int sampleCount, double averageVelocity,
                            double medianVelocity, double minVelocity, double maxVelocity,
                            double stdDeviation, double percentile90, double percentile95) {
        this.arcId = arcId;
        this.sampleCount = sampleCount;
        this.averageVelocity = averageVelocity;
        this.medianVelocity = medianVelocity;
        this.minVelocity = minVelocity;
        this.maxVelocity = maxVelocity;
        this.stdDeviation = stdDeviation;
        this.percentile90 = percentile90;
        this.percentile95 = percentile95;
    }

    // Getters
    public ArcIdentifier getArcId() {
        return arcId;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public double getAverageVelocity() {
        return averageVelocity;
    }

    public double getMedianVelocity() {
        return medianVelocity;
    }

    public double getMinVelocity() {
        return minVelocity;
    }

    public double getMaxVelocity() {
        return maxVelocity;
    }

    public double getStdDeviation() {
        return stdDeviation;
    }

    public double getPercentile90() {
        return percentile90;
    }

    public double getPercentile95() {
        return percentile95;
    }

    /**
     * Indica si hay suficientes muestras para considerar las estadísticas confiables
     */
    public boolean isReliable() {
        return sampleCount >= 10; // Mínimo 10 muestras para considerar confiable
    }

    /**
     * Obtiene la velocidad típica (usa mediana para evitar outliers)
     */
    public double getTypicalVelocity() {
        return medianVelocity;
    }

    @Override
    public String toString() {
        return String.format("ArcStats[%s, samples=%d, avg=%.1f, median=%.1f, range=[%.1f-%.1f]]",
                arcId, sampleCount, averageVelocity, medianVelocity, minVelocity, maxVelocity);
    }

    /**
     * Builder para construir estadísticas a partir de una lista de velocidades
     */
    public static class Builder {
        private final ArcIdentifier arcId;
        private final List<Double> velocities;

        public Builder(ArcIdentifier arcId) {
            this.arcId = arcId;
            this.velocities = new ArrayList<>();
        }

        public Builder addVelocity(double velocity) {
            // Filtrar velocidades negativas o cero (datos inválidos)
            if (velocity > 0) {
                velocities.add(velocity);
            }
            return this;
        }

        public Builder addVelocities(List<Double> velocities) {
            velocities.forEach(this::addVelocity);
            return this;
        }

        public ArcVelocityStats build() {
            if (velocities.isEmpty()) {
                // Sin datos, retornar estadísticas vacías
                return new ArcVelocityStats(arcId, 0, 0, 0, 0, 0, 0, 0, 0);
            }

            // Ordenar para calcular percentiles
            List<Double> sorted = new ArrayList<>(velocities);
            Collections.sort(sorted);

            int n = sorted.size();
            double average = sorted.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double median = calculateMedian(sorted);
            double min = sorted.get(0);
            double max = sorted.get(n - 1);
            double stdDev = calculateStdDeviation(sorted, average);
            double p90 = calculatePercentile(sorted, 0.90);
            double p95 = calculatePercentile(sorted, 0.95);

            return new ArcVelocityStats(arcId, n, average, median, min, max, stdDev, p90, p95);
        }

        private double calculateMedian(List<Double> sorted) {
            int n = sorted.size();
            if (n % 2 == 0) {
                return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
            } else {
                return sorted.get(n / 2);
            }
        }

        private double calculateStdDeviation(List<Double> values, double mean) {
            double sumSquaredDiffs = values.stream()
                    .mapToDouble(v -> Math.pow(v - mean, 2))
                    .sum();
            return Math.sqrt(sumSquaredDiffs / values.size());
        }

        private double calculatePercentile(List<Double> sorted, double percentile) {
            int n = sorted.size();
            int index = (int) Math.ceil(percentile * n) - 1;
            index = Math.max(0, Math.min(index, n - 1));
            return sorted.get(index);
        }
    }
}
