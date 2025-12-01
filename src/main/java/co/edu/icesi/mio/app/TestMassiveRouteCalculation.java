package co.edu.icesi.mio.app;

import co.edu.icesi.mio.concurrency.route.RouteCalculationResult;
import co.edu.icesi.mio.infra.csv.GrafoMIO;
import co.edu.icesi.mio.model.analytics.ArcIdentifier;
import co.edu.icesi.mio.model.analytics.ArcVelocityStats;
import co.edu.icesi.mio.model.routing.RoutePair;
import co.edu.icesi.mio.repository.ArcVelocityRepository;
import co.edu.icesi.mio.service.analytics.ArcVelocityCalculator;
import co.edu.icesi.mio.service.routing.MassiveRouteCalculator;
import co.edu.icesi.mio.service.routing.RouteCalculatorService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Programa de prueba para cálculo masivo de rutas.
 * Demuestra el uso del patrón Master-Worker para calcular cientos de rutas en paralelo.
 */
public class TestMassiveRouteCalculation {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  TEST DE CÁLCULO MASIVO DE RUTAS - SITM-MIO");
        System.out.println("═══════════════════════════════════════════════════════════\n");

        try {
            // FASE 1: Cargar grafo del MIO
            System.out.println("FASE 1: Cargando grafo del MIO...\n");
            GrafoMIO grafo = loadGrafo();

            // FASE 2: Calcular velocidades de arcos (versión rápida)
            System.out.println("\nFASE 2: Calculando velocidades de arcos...\n");
            ArcVelocityRepository velocityRepo = calculateVelocities();

            // FASE 3: Crear servicio de cálculo de rutas
            System.out.println("\nFASE 3: Inicializando servicio de rutas...\n");
            RouteCalculatorService routeService = new RouteCalculatorService(grafo, velocityRepo);
            System.out.println("✓ Servicio de rutas inicializado");
            System.out.println();

            // Configuración de pruebas
            int numPairs = 100; // Número de pares a calcular

            // TEST 1: Generar pares aleatorios
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  GENERANDO PARES DE RUTAS");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            MassiveRouteCalculator calculator4 = new MassiveRouteCalculator(routeService, grafo, 4);
            List<RoutePair> randomPairs = calculator4.generateRandomPairs(numPairs);
            System.out.printf("✓ Generados %d pares aleatorios\n", randomPairs.size());

            // Mostrar algunos ejemplos
            System.out.println("\nEjemplos de pares generados:");
            for (int i = 0; i < Math.min(5, randomPairs.size()); i++) {
                RoutePair pair = randomPairs.get(i);
                System.out.println("  " + pair);
            }
            System.out.println();

            // TEST 2: Calcular rutas con diferentes cantidades de workers
            System.out.println("\n╔══════════════════════════════════════════════════════════╗");
            System.out.println("║  TEST 1: CÁLCULO CON 1 WORKER (SECUENCIAL)              ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");

            long seq1Start = System.currentTimeMillis();
            MassiveRouteCalculator calculator1 = new MassiveRouteCalculator(routeService, grafo, 1);
            Map<String, RouteCalculationResult> results1 = calculator1.calculateRoutes(randomPairs);
            long seq1End = System.currentTimeMillis();
            double seq1Duration = (seq1End - seq1Start) / 1000.0;

            MassiveRouteCalculator.RouteStatistics stats1 = calculator1.generateStatistics(results1);
            System.out.println(stats1);

            // TEST 3: Calcular con 2 workers
            System.out.println("\n╔══════════════════════════════════════════════════════════╗");
            System.out.println("║  TEST 2: CÁLCULO CON 2 WORKERS                          ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");

            long par2Start = System.currentTimeMillis();
            MassiveRouteCalculator calculator2 = new MassiveRouteCalculator(routeService, grafo, 2);
            Map<String, RouteCalculationResult> results2 = calculator2.calculateRoutes(randomPairs);
            long par2End = System.currentTimeMillis();
            double par2Duration = (par2End - par2Start) / 1000.0;

            MassiveRouteCalculator.RouteStatistics stats2 = calculator2.generateStatistics(results2);
            System.out.println(stats2);

            // TEST 4: Calcular con 4 workers
            System.out.println("\n╔══════════════════════════════════════════════════════════╗");
            System.out.println("║  TEST 3: CÁLCULO CON 4 WORKERS                          ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");

            long par4Start = System.currentTimeMillis();
            Map<String, RouteCalculationResult> results4 = calculator4.calculateRoutes(randomPairs);
            long par4End = System.currentTimeMillis();
            double par4Duration = (par4End - par4Start) / 1000.0;

            MassiveRouteCalculator.RouteStatistics stats4 = calculator4.generateStatistics(results4);
            System.out.println(stats4);

            // TEST 5: Calcular con 8 workers
            System.out.println("\n╔══════════════════════════════════════════════════════════╗");
            System.out.println("║  TEST 4: CÁLCULO CON 8 WORKERS                          ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");

            long par8Start = System.currentTimeMillis();
            MassiveRouteCalculator calculator8 = new MassiveRouteCalculator(routeService, grafo, 8);
            Map<String, RouteCalculationResult> results8 = calculator8.calculateRoutes(randomPairs);
            long par8End = System.currentTimeMillis();
            double par8Duration = (par8End - par8Start) / 1000.0;

            MassiveRouteCalculator.RouteStatistics stats8 = calculator8.generateStatistics(results8);
            System.out.println(stats8);

            // COMPARACIÓN DE RENDIMIENTO
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  COMPARACIÓN DE RENDIMIENTO");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            System.out.printf("%-20s %15s %20s %15s %20s\n",
                    "Configuración", "Duración (s)", "Rutas/segundo", "Speedup", "Eficiencia");
            System.out.println("─".repeat(95));

            double routesPerSec1 = numPairs / seq1Duration;
            System.out.printf("%-20s %15.2f %20.1f %15s %20s\n",
                    "1 Worker", seq1Duration, routesPerSec1, "1.00x", "100%");

            double speedup2 = seq1Duration / par2Duration;
            double efficiency2 = (speedup2 / 2) * 100;
            double routesPerSec2 = numPairs / par2Duration;
            System.out.printf("%-20s %15.2f %20.1f %14.2fx %19.1f%%\n",
                    "2 Workers", par2Duration, routesPerSec2, speedup2, efficiency2);

            double speedup4 = seq1Duration / par4Duration;
            double efficiency4 = (speedup4 / 4) * 100;
            double routesPerSec4 = numPairs / par4Duration;
            System.out.printf("%-20s %15.2f %20.1f %14.2fx %19.1f%%\n",
                    "4 Workers", par4Duration, routesPerSec4, speedup4, efficiency4);

            double speedup8 = seq1Duration / par8Duration;
            double efficiency8 = (speedup8 / 8) * 100;
            double routesPerSec8 = numPairs / par8Duration;
            System.out.printf("%-20s %15.2f %20.1f %14.2fx %19.1f%%\n",
                    "8 Workers", par8Duration, routesPerSec8, speedup8, efficiency8);

            System.out.println("═".repeat(95));

            // ANÁLISIS
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  ANÁLISIS DE RESULTADOS");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            double bestSpeedup = Math.max(Math.max(speedup2, speedup4), speedup8);
            double bestDuration = Math.min(Math.min(par2Duration, par4Duration), par8Duration);
            String bestConfig = speedup8 > speedup4 ?
                    (speedup8 > speedup2 ? "8 workers" : "2 workers") :
                    (speedup4 > speedup2 ? "4 workers" : "2 workers");

            System.out.printf("Mejor configuración: %s (%.2fx más rápido)\n", bestConfig, bestSpeedup);
            System.out.printf("Tiempo ahorrado: %.2f segundos\n", seq1Duration - bestDuration);
            System.out.printf("\nEscalabilidad proyectada para 1000 rutas:\n");
            System.out.printf("  1 worker:  ~%.0f segundos\n", (seq1Duration / numPairs) * 1000);
            System.out.printf("  8 workers: ~%.0f segundos\n", (par8Duration / numPairs) * 1000);

            // CASOS DE USO PRÁCTICOS
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  CASOS DE USO PRÁCTICOS");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            System.out.println("1. MATRIZ DE CONECTIVIDAD");
            System.out.println("   Calcular rutas entre las 50 paradas más importantes:");
            System.out.println("   - Pares necesarios: 50 × 49 / 2 = 1,225 rutas");
            System.out.printf("   - Tiempo estimado (8 workers): ~%.0f segundos\n",
                    (par8Duration / numPairs) * 1225);

            System.out.println("\n2. PRECÁLCULO DE RUTAS POPULARES");
            System.out.println("   Calcular top 500 rutas más solicitadas:");
            System.out.printf("   - Tiempo estimado (8 workers): ~%.0f segundos\n",
                    (par8Duration / numPairs) * 500);

            System.out.println("\n3. ANÁLISIS DE COBERTURA");
            System.out.println("   Validar conectividad entre todas las paradas:");
            int totalStops = grafo.getParadas().size();
            int totalPairs = totalStops * (totalStops - 1) / 2;
            System.out.printf("   - Total de pares posibles: %,d\n", totalPairs);
            System.out.printf("   - Tiempo estimado (8 workers): ~%.0f minutos\n",
                    ((par8Duration / numPairs) * totalPairs) / 60);

            // MOSTRAR ALGUNAS RUTAS CALCULADAS
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  EJEMPLOS DE RUTAS CALCULADAS");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            results8.values().stream()
                    .filter(RouteCalculationResult::isSuccess)
                    .limit(5)
                    .forEach(result -> {
                        System.out.println(result.getRoute().toDetailedString());
                        System.out.println();
                    });

            System.out.println("═══════════════════════════════════════════════════════════");
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
        calculator.processDatagramFile(dataPath);

        Map<ArcIdentifier, ArcVelocityStats> stats = calculator.calculateStatistics();

        ArcVelocityRepository repository = new ArcVelocityRepository();
        repository.saveAll(stats);

        System.out.println("✓ Velocidades calculadas y almacenadas");
        return repository;
    }
}
