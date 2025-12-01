package co.edu.icesi.mio.app;

import co.edu.icesi.mio.model.analytics.ArcVelocityStats;
import co.edu.icesi.mio.repository.ArcVelocityRepository;
import co.edu.icesi.mio.service.analytics.ArcVelocityCalculator;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Programa de prueba para calcular velocidades de arcos desde datos históricos.
 */
public class TestArcVelocityCalculation {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  TEST DE CÁLCULO DE VELOCIDADES DE ARCOS - SITM-MIO");
        System.out.println("═══════════════════════════════════════════════════════════\n");

        // Usar el archivo de streaming (más pequeño) para pruebas
        String dataPath = "src/main/resources/data/datagrams4streaming.csv";

        try {
            // Fase 1: Procesar datagramas y calcular velocidades
            System.out.println("FASE 1: Procesando datagramas...\n");
            ArcVelocityCalculator calculator = new ArcVelocityCalculator();
            // Procesa máximo 15,000 datagramas (límite por defecto para evitar sobrecarga)
            ArcVelocityCalculator.CalculationStats calcStats = calculator.processDatagramFile(dataPath);

            // Fase 2: Calcular estadísticas
            System.out.println("\nFASE 2: Calculando estadísticas por arco...\n");
            Map<co.edu.icesi.mio.model.analytics.ArcIdentifier, ArcVelocityStats> stats =
                    calculator.calculateStatistics();

            // Fase 3: Almacenar en repositorio
            System.out.println("\nFASE 3: Almacenando en repositorio...\n");
            ArcVelocityRepository repository = new ArcVelocityRepository();
            repository.saveAll(stats);

            // Mostrar estadísticas del repositorio
            System.out.println("═══════════════════════════════════════════════════════════");
            System.out.println("  ESTADÍSTICAS DEL REPOSITORIO");
            System.out.println("═══════════════════════════════════════════════════════════");
            System.out.println(repository.getStats());
            System.out.println();

            // Análisis: Top 10 arcos más rápidos
            System.out.println("═══════════════════════════════════════════════════════════");
            System.out.println("  TOP 10 ARCOS MÁS RÁPIDOS");
            System.out.println("═══════════════════════════════════════════════════════════");
            List<ArcVelocityStats> fastestArcs = repository.findFastestArcs(10);
            printArcStats(fastestArcs);

            // Análisis: Top 10 arcos más lentos
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  TOP 10 ARCOS MÁS LENTOS");
            System.out.println("═══════════════════════════════════════════════════════════");
            List<ArcVelocityStats> slowestArcs = repository.findSlowestArcs(10);
            printArcStats(slowestArcs);

            // Análisis: Top 10 arcos con mayor variabilidad
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  TOP 10 ARCOS CON MAYOR VARIABILIDAD");
            System.out.println("═══════════════════════════════════════════════════════════");
            List<ArcVelocityStats> mostVariableArcs = repository.findMostVariableArcs(10);
            printVariableArcStats(mostVariableArcs);

            // Ejemplo: Consultar velocidad de una línea específica
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  EJEMPLO: ESTADÍSTICAS PARA LÍNEA 31");
            System.out.println("═══════════════════════════════════════════════════════════");
            List<ArcVelocityStats> line31Stats = repository.findByLine(31);
            System.out.printf("Total de arcos en línea 31: %d\n", line31Stats.size());
            if (!line31Stats.isEmpty()) {
                System.out.println("\nAlgunos ejemplos:");
                line31Stats.stream().limit(5).forEach(stat ->
                        System.out.printf("  %s: %.1f km/h (mediana), %d muestras\n",
                                stat.getArcId(), stat.getMedianVelocity(), stat.getSampleCount())
                );
            }

            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  PRUEBA COMPLETADA EXITOSAMENTE");
            System.out.println("═══════════════════════════════════════════════════════════");

        } catch (IOException e) {
            System.err.println("✗ Error procesando archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printArcStats(List<ArcVelocityStats> arcStats) {
        System.out.println();
        System.out.printf("%-30s %10s %10s %10s %10s\n",
                "Arco", "Promedio", "Mediana", "Muestras", "Confiable");
        System.out.println("─".repeat(80));

        for (ArcVelocityStats stats : arcStats) {
            System.out.printf("%-30s %9.1f  %9.1f  %9d  %10s\n",
                    stats.getArcId().toString(),
                    stats.getAverageVelocity(),
                    stats.getMedianVelocity(),
                    stats.getSampleCount(),
                    stats.isReliable() ? "✓" : "✗"
            );
        }
    }

    private static void printVariableArcStats(List<ArcVelocityStats> arcStats) {
        System.out.println();
        System.out.printf("%-30s %10s %10s %10s %10s\n",
                "Arco", "Promedio", "Std Dev", "Min-Max", "Muestras");
        System.out.println("─".repeat(80));

        for (ArcVelocityStats stats : arcStats) {
            System.out.printf("%-30s %9.1f  %9.1f  %4.0f-%-4.0f  %9d\n",
                    stats.getArcId().toString(),
                    stats.getAverageVelocity(),
                    stats.getStdDeviation(),
                    stats.getMinVelocity(),
                    stats.getMaxVelocity(),
                    stats.getSampleCount()
            );
        }
    }
}
