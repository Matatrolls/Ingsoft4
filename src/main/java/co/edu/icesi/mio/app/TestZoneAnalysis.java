package co.edu.icesi.mio.app;

import co.edu.icesi.mio.concurrency.zone.ZoneAnalysisResult;
import co.edu.icesi.mio.infra.csv.GrafoMIO;
import co.edu.icesi.mio.model.analytics.ArcIdentifier;
import co.edu.icesi.mio.model.analytics.ArcVelocityStats;
import co.edu.icesi.mio.model.analytics.Zone;
import co.edu.icesi.mio.repository.ArcVelocityRepository;
import co.edu.icesi.mio.service.analytics.ArcVelocityCalculator;
import co.edu.icesi.mio.service.analytics.ZoneAnalyzer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Programa de prueba para análisis de zonas geográficas.
 * Demuestra el uso del patrón Master-Worker para análisis paralelo por sectores.
 */
public class TestZoneAnalysis {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  TEST DE ANÁLISIS DE ZONAS GEOGRÁFICAS - SITM-MIO");
        System.out.println("═══════════════════════════════════════════════════════════\n");

        try {
            // FASE 1: Cargar grafo del MIO
            System.out.println("FASE 1: Cargando grafo del MIO...\n");
            GrafoMIO grafo = loadGrafo();

            // FASE 2: Calcular velocidades de arcos
            System.out.println("\nFASE 2: Calculando velocidades de arcos...\n");
            ArcVelocityRepository velocityRepo = calculateVelocities();

            // FASE 3: Definir zonas de Cali
            System.out.println("\nFASE 3: Definiendo zonas de análisis...\n");
            Zone[] zones = Zone.createCaliZones();

            System.out.println("Zonas definidas:");
            for (Zone zone : zones) {
                System.out.println("  " + zone);
            }
            System.out.println();

            // FASE 4: Análisis paralelo de zonas
            System.out.println("\nFASE 4: Ejecutando análisis de zonas...\n");

            ZoneAnalyzer analyzer = new ZoneAnalyzer(grafo, velocityRepo, zones.length);
            Map<String, ZoneAnalysisResult> results = analyzer.analyzeZones(Arrays.asList(zones));

            // FASE 5: Mostrar resultados detallados
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  RESULTADOS DETALLADOS POR ZONA");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            for (Zone zone : zones) {
                ZoneAnalysisResult result = results.get(zone.getZoneId());
                if (result != null) {
                    System.out.println(result.toDetailedReport());
                    System.out.println();
                }
            }

            // FASE 6: Generar reporte de ciudad
            ZoneAnalyzer.CityReport cityReport = analyzer.generateCityReport(results);
            System.out.println(cityReport);

            // FASE 7: Comparación de zonas
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  COMPARACIÓN ENTRE ZONAS");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            System.out.printf("%-15s %12s %12s %12s %15s %15s\n",
                    "Zona", "Paradas", "Arcos", "Rutas", "Vel. Prom", "Estado");
            System.out.println("─".repeat(90));

            for (Zone zone : zones) {
                ZoneAnalysisResult result = results.get(zone.getZoneId());
                if (result != null) {
                    System.out.printf("%-15s %12d %12d %12d %14.1f km/h %15s\n",
                            result.getZone().getZoneName(),
                            result.getStopCount(),
                            result.getArcCount(),
                            result.getUniqueRoutesCount(),
                            result.getAverageVelocity(),
                            result.isCongested() ? "⚠ Congestionado" : "✓ Fluido");
                }
            }
            System.out.println("═".repeat(90));

            // FASE 8: Análisis de densidad
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  ANÁLISIS DE DENSIDAD");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            System.out.printf("%-15s %15s %15s %20s\n",
                    "Zona", "Paradas/km²", "Arcos/km²", "Cobertura");
            System.out.println("─".repeat(70));

            for (Zone zone : zones) {
                ZoneAnalysisResult result = results.get(zone.getZoneId());
                if (result != null) {
                    System.out.printf("%-15s %15.1f %15.1f %20s\n",
                            result.getZone().getZoneName(),
                            result.getStopDensity(),
                            result.getArcDensity(),
                            result.hasGoodCoverage() ? "✓ Buena" : "⚠ Limitada");
                }
            }
            System.out.println("═".repeat(70));

            // FASE 9: Recomendaciones
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  RECOMENDACIONES DE MEJORA");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            int recommendationNum = 1;
            for (Zone zone : zones) {
                ZoneAnalysisResult result = results.get(zone.getZoneId());
                if (result != null) {
                    if (result.isCongested()) {
                        System.out.printf("%d. ZONA %s:\n", recommendationNum++, result.getZone().getZoneName());
                        System.out.println("   → Implementar carriles exclusivos para MIO");
                        System.out.println("   → Ajustar sincronización de semáforos");
                        System.out.println("   → Considerar rutas alternativas");
                        System.out.println();
                    }

                    if (!result.hasGoodCoverage()) {
                        System.out.printf("%d. ZONA %s:\n", recommendationNum++, result.getZone().getZoneName());
                        System.out.println("   → Aumentar número de paradas");
                        System.out.println("   → Agregar rutas adicionales");
                        System.out.println("   → Mejorar conectividad con otras zonas");
                        System.out.println();
                    }
                }
            }

            if (recommendationNum == 1) {
                System.out.println("✓ Todas las zonas tienen buena cobertura y flujo de tráfico");
            }

            // CASOS DE USO
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  CASOS DE USO DEL ANÁLISIS DE ZONAS");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            System.out.println("1. PLANIFICACIÓN URBANA");
            System.out.println("   Identificar zonas con baja cobertura de transporte");
            System.out.println("   Priorizar expansión del sistema MIO");

            System.out.println("\n2. GESTIÓN DE TRÁFICO");
            System.out.println("   Detectar zonas congestionadas en tiempo real");
            System.out.println("   Redirigir buses a rutas alternativas");

            System.out.println("\n3. INVERSIÓN INTELIGENTE");
            System.out.println("   Determinar dónde construir nuevas estaciones");
            System.out.println("   Optimizar asignación de presupuesto por zona");

            System.out.println("\n4. ANÁLISIS DE DEMANDA");
            System.out.println("   Comparar densidad de paradas vs población");
            System.out.println("   Ajustar frecuencia de buses por sector");

            System.out.println("\n5. MONITOREO DE CALIDAD");
            System.out.println("   Medir velocidad promedio por zona");
            System.out.println("   Evaluar cumplimiento de estándares de servicio");

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

    private static GrafoMIO loadGrafo() throws IOException {
        GrafoMIO grafo = new GrafoMIO();

        String basePath = "src/main/resources/data/";
        grafo.cargarParadas(basePath + "stops-241.csv");
        grafo.cargarRutas(basePath + "lines-241.csv");
        grafo.cargarLineStopsYConstruirArcos(basePath + "linestops-241.csv");

        System.out.println("✓ Grafo cargado exitosamente");
        System.out.printf("  Paradas: %,d\n", grafo.getParadas().size());
        System.out.printf("  Rutas: %,d\n", grafo.getRutas().size());
        System.out.printf("  Arcos: %,d\n", grafo.getArcos().size());

        return grafo;
    }

    private static ArcVelocityRepository calculateVelocities() throws IOException {
        String dataPath = "src/main/resources/data/datagrams4streaming.csv";

        ArcVelocityCalculator calculator = new ArcVelocityCalculator();
        // Procesa máximo 15,000 datagramas (límite por defecto para evitar sobrecarga)
        calculator.processDatagramFile(dataPath);

        Map<ArcIdentifier, ArcVelocityStats> stats = calculator.calculateStatistics();

        ArcVelocityRepository repository = new ArcVelocityRepository();
        repository.saveAll(stats);

        System.out.println("✓ Velocidades calculadas y almacenadas");
        return repository;
    }
}
