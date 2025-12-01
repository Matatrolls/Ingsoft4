package co.edu.icesi.mio.app;

import co.edu.icesi.mio.infra.csv.GrafoMIO;
import co.edu.icesi.mio.infra.csv.Parada;
import co.edu.icesi.mio.model.analytics.ArcIdentifier;
import co.edu.icesi.mio.model.analytics.ArcVelocityStats;
import co.edu.icesi.mio.model.routing.CalculatedRoute;
import co.edu.icesi.mio.repository.ArcVelocityRepository;
import co.edu.icesi.mio.service.analytics.ArcVelocityCalculator;
import co.edu.icesi.mio.service.routing.RouteCalculatorService;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

/**
 * Programa de prueba para el cálculo de rutas óptimas.
 * Integra Fase 1 (lectura de datos), Fase 2 (cálculo de velocidades) y Fase 3 (cálculo de rutas).
 */
public class TestRouteCalculation {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  TEST DE CÁLCULO DE RUTAS - SITM-MIO");
        System.out.println("═══════════════════════════════════════════════════════════\n");

        try {
            // FASE 1: Cargar grafo del MIO
            System.out.println("FASE 1: Cargando grafo del MIO...\n");
            GrafoMIO grafo = loadGrafo();

            // FASE 2: Calcular velocidades de arcos
            System.out.println("\nFASE 2: Calculando velocidades de arcos...\n");
            ArcVelocityRepository velocityRepo = calculateVelocities();

            // FASE 3: Crear servicio de cálculo de rutas
            System.out.println("\nFASE 3: Inicializando servicio de rutas...\n");
            RouteCalculatorService routeService = new RouteCalculatorService(grafo, velocityRepo);
            System.out.println(routeService.getServiceInfo());

            // Ejemplos de cálculo de rutas
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  EJEMPLOS DE CÁLCULO DE RUTAS");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            // Obtener algunas paradas de ejemplo
            Map<Integer, Parada> paradas = grafo.getParadas();
            if (paradas.size() >= 2) {
                // Tomar dos paradas aleatorias
                int[] stopIds = paradas.keySet().stream().limit(2).mapToInt(Integer::intValue).toArray();

                if (stopIds.length >= 2) {
                    int originId = stopIds[0];
                    int destId = stopIds[1];

                    Parada origin = paradas.get(originId);
                    Parada dest = paradas.get(destId);

                    System.out.println("Calculando rutas entre:");
                    System.out.println("  Origen: " + origin);
                    System.out.println("  Destino: " + dest);
                    System.out.println();

                    // Estrategia 1: Ruta más rápida
                    System.out.println("─".repeat(80));
                    System.out.println("ESTRATEGIA 1: Ruta más rápida (minimiza tiempo)");
                    System.out.println("─".repeat(80));
                    CalculatedRoute fastestRoute = routeService.calculateFastestRoute(originId, destId);
                    System.out.println(fastestRoute.toDetailedString());

                    // Estrategia 2: Ruta más corta
                    System.out.println("\n─".repeat(80));
                    System.out.println("ESTRATEGIA 2: Ruta más corta (minimiza distancia)");
                    System.out.println("─".repeat(80));
                    CalculatedRoute shortestRoute = routeService.calculateShortestRoute(originId, destId);
                    System.out.println(shortestRoute.toDetailedString());

                    // Estrategia 3: Menos transbordos
                    System.out.println("\n─".repeat(80));
                    System.out.println("ESTRATEGIA 3: Menos transbordos");
                    System.out.println("─".repeat(80));
                    CalculatedRoute fewestTransfersRoute = routeService.calculateFewestTransfersRoute(originId, destId);
                    System.out.println(fewestTransfersRoute.toDetailedString());

                    // Comparación
                    System.out.println("\n═══════════════════════════════════════════════════════════");
                    System.out.println("  COMPARACIÓN DE ESTRATEGIAS");
                    System.out.println("═══════════════════════════════════════════════════════════");
                    compareRoutes(fastestRoute, shortestRoute, fewestTransfersRoute);
                }
            }

            // Modo interactivo (opcional)
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  MODO INTERACTIVO");
            System.out.println("═══════════════════════════════════════════════════════════");
            System.out.println("¿Desea calcular una ruta personalizada? (s/n): ");

            Scanner scanner = new Scanner(System.in);
            String response = scanner.nextLine().trim().toLowerCase();

            if (response.equals("s") || response.equals("si")) {
                interactiveMode(grafo, routeService, scanner);
            }

            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  PRUEBA COMPLETADA");
            System.out.println("═══════════════════════════════════════════════════════════");

        } catch (IOException e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static GrafoMIO loadGrafo() throws IOException {
        GrafoMIO grafo = new GrafoMIO();

        String basePath = "src/main/resources/data/";
        grafo.cargarParadas(basePath + "stops-241.csv");
        grafo.cargarRutas(basePath + "lines-241.csv");
        grafo.cargarLineStopsYConstruirArcos(basePath + "linestops-241.csv");

        System.out.println("✓ Grafo cargado exitosamente");
        System.out.printf("  Paradas: %d\n", grafo.getParadas().size());
        System.out.printf("  Rutas: %d\n", grafo.getRutas().size());
        System.out.printf("  Arcos: %d\n", grafo.getArcos().size());

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

    private static void compareRoutes(CalculatedRoute fastest, CalculatedRoute shortest,
                                      CalculatedRoute fewestTransfers) {
        System.out.println();
        System.out.printf("%-25s %15s %15s %15s %15s\n",
                "Estrategia", "Tiempo (min)", "Distancia (m)", "Segmentos", "Transbordos");
        System.out.println("─".repeat(95));

        System.out.printf("%-25s %15.1f %15.0f %15d %15d\n",
                "Más rápida",
                fastest.getTotalTime(),
                fastest.getTotalDistance(),
                fastest.getSegmentCount(),
                fastest.getTransferCount());

        System.out.printf("%-25s %15.1f %15.0f %15d %15d\n",
                "Más corta",
                shortest.getTotalTime(),
                shortest.getTotalDistance(),
                shortest.getSegmentCount(),
                shortest.getTransferCount());

        System.out.printf("%-25s %15.1f %15.0f %15d %15d\n",
                "Menos transbordos",
                fewestTransfers.getTotalTime(),
                fewestTransfers.getTotalDistance(),
                fewestTransfers.getSegmentCount(),
                fewestTransfers.getTransferCount());

        System.out.println("═".repeat(95));
    }

    private static void interactiveMode(GrafoMIO grafo, RouteCalculatorService routeService,
                                        Scanner scanner) {
        System.out.println("\nIngrese el ID de la parada de origen:");
        int originId = scanner.nextInt();

        System.out.println("Ingrese el ID de la parada de destino:");
        int destId = scanner.nextInt();

        scanner.nextLine(); // Consumir newline

        Parada origin = grafo.getParadas().get(originId);
        Parada dest = grafo.getParadas().get(destId);

        if (origin == null || dest == null) {
            System.out.println("✗ Una o ambas paradas no existen");
            return;
        }

        System.out.println("\nCalculando ruta...\n");
        CalculatedRoute route = routeService.calculateFastestRoute(originId, destId);
        System.out.println(route.toDetailedString());
    }
}
