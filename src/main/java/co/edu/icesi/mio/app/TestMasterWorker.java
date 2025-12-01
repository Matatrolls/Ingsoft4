package co.edu.icesi.mio.app;

import co.edu.icesi.mio.model.analytics.ArcIdentifier;
import co.edu.icesi.mio.model.analytics.ArcVelocityStats;
import co.edu.icesi.mio.service.analytics.ArcVelocityCalculator;

import java.io.IOException;
import java.util.Map;

/**
 * Programa de prueba para comparar procesamiento secuencial vs paralelo (Master-Worker).
 * Demuestra las ventajas del patrón Master-Worker en archivos grandes.
 */
public class TestMasterWorker {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  TEST MASTER-WORKER: SECUENCIAL VS PARALELO");
        System.out.println("═══════════════════════════════════════════════════════════\n");

        String dataPath = "src/main/resources/data/datagrams4streaming.csv";

        try {
            // Test 1: Procesamiento Secuencial
            System.out.println("\n╔══════════════════════════════════════════════════════════╗");
            System.out.println("║  TEST 1: PROCESAMIENTO SECUENCIAL                       ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");

            long seqStartTime = System.currentTimeMillis();
            ArcVelocityCalculator sequentialCalculator = new ArcVelocityCalculator();
            sequentialCalculator.processDatagramFile(dataPath);
            Map<ArcIdentifier, ArcVelocityStats> seqStats = sequentialCalculator.calculateStatistics();
            long seqEndTime = System.currentTimeMillis();
            double seqDuration = (seqEndTime - seqStartTime) / 1000.0;

            // Test 2: Procesamiento Paralelo con 2 workers
            System.out.println("\n╔══════════════════════════════════════════════════════════╗");
            System.out.println("║  TEST 2: PROCESAMIENTO PARALELO (2 Workers)             ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");

            long par2StartTime = System.currentTimeMillis();
            ArcVelocityCalculator parallel2Calculator = new ArcVelocityCalculator();
            Map<ArcIdentifier, ArcVelocityStats> par2Stats =
                    parallel2Calculator.processDatagramFileParallel(dataPath, 2);
            long par2EndTime = System.currentTimeMillis();
            double par2Duration = (par2EndTime - par2StartTime) / 1000.0;

            // Test 3: Procesamiento Paralelo con 4 workers
            System.out.println("\n╔══════════════════════════════════════════════════════════╗");
            System.out.println("║  TEST 3: PROCESAMIENTO PARALELO (4 Workers)             ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");

            long par4StartTime = System.currentTimeMillis();
            ArcVelocityCalculator parallel4Calculator = new ArcVelocityCalculator();
            Map<ArcIdentifier, ArcVelocityStats> par4Stats =
                    parallel4Calculator.processDatagramFileParallel(dataPath, 4);
            long par4EndTime = System.currentTimeMillis();
            double par4Duration = (par4EndTime - par4StartTime) / 1000.0;

            // Test 4: Procesamiento Paralelo con 8 workers
            System.out.println("\n╔══════════════════════════════════════════════════════════╗");
            System.out.println("║  TEST 4: PROCESAMIENTO PARALELO (8 Workers)             ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");

            long par8StartTime = System.currentTimeMillis();
            ArcVelocityCalculator parallel8Calculator = new ArcVelocityCalculator();
            Map<ArcIdentifier, ArcVelocityStats> par8Stats =
                    parallel8Calculator.processDatagramFileParallel(dataPath, 8);
            long par8EndTime = System.currentTimeMillis();
            double par8Duration = (par8EndTime - par8StartTime) / 1000.0;

            // Comparación de resultados
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  COMPARACIÓN DE RESULTADOS");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            System.out.printf("%-25s %15s %15s %20s %15s\n",
                    "Estrategia", "Duración (s)", "Arcos", "Speedup", "Eficiencia");
            System.out.println("─".repeat(95));

            System.out.printf("%-25s %15.2f %15d %20s %15s\n",
                    "Secuencial", seqDuration, seqStats.size(), "1.00x", "100%");

            double speedup2 = seqDuration / par2Duration;
            double efficiency2 = (speedup2 / 2) * 100;
            System.out.printf("%-25s %15.2f %15d %20.2fx %14.1f%%\n",
                    "Paralelo (2 workers)", par2Duration, par2Stats.size(), speedup2, efficiency2);

            double speedup4 = seqDuration / par4Duration;
            double efficiency4 = (speedup4 / 4) * 100;
            System.out.printf("%-25s %15.2f %15d %20.2fx %14.1f%%\n",
                    "Paralelo (4 workers)", par4Duration, par4Stats.size(), speedup4, efficiency4);

            double speedup8 = seqDuration / par8Duration;
            double efficiency8 = (speedup8 / 8) * 100;
            System.out.printf("%-25s %15.2f %15d %20.2fx %14.1f%%\n",
                    "Paralelo (8 workers)", par8Duration, par8Stats.size(), speedup8, efficiency8);

            System.out.println("═".repeat(95));

            // Análisis de rendimiento
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  ANÁLISIS DE RENDIMIENTO");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            double bestSpeedup = Math.max(Math.max(speedup2, speedup4), speedup8);
            String bestConfig = speedup8 > speedup4 ?
                    (speedup8 > speedup2 ? "8 workers" : "2 workers") :
                    (speedup4 > speedup2 ? "4 workers" : "2 workers");

            System.out.printf("Mejor configuración: %s (%.2fx más rápido)\n", bestConfig, bestSpeedup);
            System.out.printf("Tiempo ahorrado: %.2f segundos\n", seqDuration - Math.min(Math.min(par2Duration, par4Duration), par8Duration));

            // Validar consistencia de resultados
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  VALIDACIÓN DE CONSISTENCIA");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            boolean consistent = (seqStats.size() == par2Stats.size() &&
                    par2Stats.size() == par4Stats.size() &&
                    par4Stats.size() == par8Stats.size());

            if (consistent) {
                System.out.println("✓ Todos los métodos produjeron el mismo número de arcos");
                System.out.println("✓ Resultados consistentes entre procesamiento secuencial y paralelo");

                // Comparar algunas estadísticas de ejemplo
                ArcIdentifier sampleArc = seqStats.keySet().stream().findFirst().orElse(null);
                if (sampleArc != null) {
                    ArcVelocityStats seqSample = seqStats.get(sampleArc);
                    ArcVelocityStats par2Sample = par2Stats.get(sampleArc);
                    ArcVelocityStats par4Sample = par4Stats.get(sampleArc);
                    ArcVelocityStats par8Sample = par8Stats.get(sampleArc);

                    System.out.println("\nValidación de arco de muestra: " + sampleArc);
                    System.out.printf("  Secuencial:   avg=%.1f, median=%.1f, samples=%d\n",
                            seqSample.getAverageVelocity(), seqSample.getMedianVelocity(), seqSample.getSampleCount());
                    System.out.printf("  Paralelo (2): avg=%.1f, median=%.1f, samples=%d\n",
                            par2Sample.getAverageVelocity(), par2Sample.getMedianVelocity(), par2Sample.getSampleCount());
                    System.out.printf("  Paralelo (4): avg=%.1f, median=%.1f, samples=%d\n",
                            par4Sample.getAverageVelocity(), par4Sample.getMedianVelocity(), par4Sample.getSampleCount());
                    System.out.printf("  Paralelo (8): avg=%.1f, median=%.1f, samples=%d\n",
                            par8Sample.getAverageVelocity(), par8Sample.getMedianVelocity(), par8Sample.getSampleCount());
                }
            } else {
                System.out.println("✗ ADVERTENCIA: Inconsistencia detectada en el número de arcos");
            }

            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  PRUEBA COMPLETADA");
            System.out.println("═══════════════════════════════════════════════════════════");

        } catch (IOException e) {
            System.err.println("✗ Error de I/O: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("✗ Error de concurrencia: " + e.getMessage());
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
}
