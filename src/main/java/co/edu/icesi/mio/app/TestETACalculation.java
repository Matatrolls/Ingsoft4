package co.edu.icesi.mio.app;

import co.edu.icesi.mio.infra.csv.Arco;
import co.edu.icesi.mio.infra.csv.GrafoMIO;
import co.edu.icesi.mio.infra.csv.Parada;
import co.edu.icesi.mio.model.analytics.ArcIdentifier;
import co.edu.icesi.mio.model.analytics.ArcVelocityStats;
import co.edu.icesi.mio.model.realtime.BusETA;
import co.edu.icesi.mio.model.realtime.BusPosition;
import co.edu.icesi.mio.repository.ArcVelocityRepository;
import co.edu.icesi.mio.service.analytics.ArcVelocityCalculator;
import co.edu.icesi.mio.service.realtime.ETACalculatorService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Programa de prueba para el servicio de cálculo de ETA.
 * Simula buses en tránsito y calcula sus tiempos estimados de llegada.
 */
public class TestETACalculation {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  TEST DE CÁLCULO DE ETA (TIEMPO ESTIMADO DE LLEGADA)");
        System.out.println("═══════════════════════════════════════════════════════════\n");

        try {
            // FASE 1: Cargar grafo del MIO
            System.out.println("FASE 1: Cargando grafo del MIO...\n");
            GrafoMIO grafo = loadGrafo();

            // FASE 2: Calcular velocidades de arcos
            System.out.println("\nFASE 2: Calculando velocidades de arcos...\n");
            ArcVelocityRepository velocityRepo = calculateVelocities();

            // FASE 3: Crear servicio de ETA
            System.out.println("\nFASE 3: Inicializando servicio de ETA...\n");
            ETACalculatorService etaService = new ETACalculatorService(grafo, velocityRepo);
            System.out.println(etaService.getServiceInfo());
            System.out.println();

            // ESCENARIO 1: Bus en movimiento aproximándose a su próxima parada
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  ESCENARIO 1: Bus en Movimiento");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            BusPosition bus1 = createSimulatedBus(grafo, 101, 25.5, true);
            System.out.println("Posición actual: " + bus1);
            System.out.println();

            BusETA eta1 = etaService.calculateETA(bus1);
            System.out.println(eta1.toDetailedString());

            // ESCENARIO 2: Bus detenido en una parada
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  ESCENARIO 2: Bus Detenido en Parada");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            BusPosition bus2 = createSimulatedBus(grafo, 102, 0.5, false);
            System.out.println("Posición actual: " + bus2);
            System.out.println();

            BusETA eta2 = etaService.calculateETA(bus2);
            System.out.println(eta2.toDetailedString());

            // ESCENARIO 3: Bus en tráfico lento
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  ESCENARIO 3: Bus en Tráfico Lento");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            BusPosition bus3 = createSimulatedBus(grafo, 103, 8.2, true);
            System.out.println("Posición actual: " + bus3);
            System.out.println();

            BusETA eta3 = etaService.calculateETA(bus3);
            System.out.println(eta3.toDetailedString());

            // ESCENARIO 4: ETAs múltiples para próximas 5 paradas
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  ESCENARIO 4: ETAs para Próximas 5 Paradas");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            BusPosition bus4 = createSimulatedBus(grafo, 104, 22.0, true);
            System.out.println("Posición actual: " + bus4);
            System.out.println();

            List<BusETA> multipleETAs = etaService.calculateMultipleETAs(bus4, 5);
            System.out.println("Calculados " + multipleETAs.size() + " ETAs:\n");
            System.out.printf("%-5s %-30s %15s %15s %15s\n",
                    "Num", "Parada", "Tiempo (min)", "Distancia (m)", "Llegada");
            System.out.println("─".repeat(90));

            for (int i = 0; i < multipleETAs.size(); i++) {
                BusETA eta = multipleETAs.get(i);
                System.out.printf("%-5d %-30s %15.1f %15.0f %15s\n",
                        i + 1,
                        eta.getTargetStop().getShortName(),
                        eta.getEstimatedTimeMinutes(),
                        eta.getDistanceMeters(),
                        eta.getEstimatedArrival().toLocalTime());
            }
            System.out.println("═".repeat(90));

            // ESCENARIO 5: Simular flota de buses
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  ESCENARIO 5: Simulación de Flota (10 buses)");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            System.out.printf("%-8s %-12s %-15s %-20s %15s\n",
                    "Bus ID", "Estado", "Velocidad", "Próxima Parada", "ETA (min)");
            System.out.println("─".repeat(85));

            for (int i = 201; i <= 210; i++) {
                boolean moving = (i % 3 != 0); // Algunos buses detenidos
                double velocity = moving ? (15 + Math.random() * 20) : 0.5;

                BusPosition bus = createSimulatedBus(grafo, i, velocity, moving);
                BusETA eta = etaService.calculateETA(bus);

                String status = bus.isMoving() ? "En movimiento" : "Detenido";
                System.out.printf("%-8d %-12s %14.1f km/h %-20s %15.1f\n",
                        bus.getBusId(),
                        status,
                        bus.getVelocity(),
                        eta.getTargetStop().getShortName().substring(0,
                                Math.min(20, eta.getTargetStop().getShortName().length())),
                        eta.getEstimatedTimeMinutes());
            }
            System.out.println("═".repeat(85));

            // ANÁLISIS DE CONFIABILIDAD
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  ANÁLISIS DE CONFIABILIDAD DE ETAs");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            int totalETAs = 10;
            int highConfidence = 0;
            int mediumConfidence = 0;
            int lowConfidence = 0;

            for (int i = 301; i <= 310; i++) {
                BusPosition bus = createSimulatedBus(grafo, i, 20 + Math.random() * 15, true);
                BusETA eta = etaService.calculateETA(bus);

                switch (eta.getConfidenceLevel()) {
                    case "Alta" -> highConfidence++;
                    case "Media" -> mediumConfidence++;
                    case "Baja" -> lowConfidence++;
                }
            }

            System.out.printf("Confiabilidad Alta:  %d/%d (%.1f%%)\n",
                    highConfidence, totalETAs, (highConfidence * 100.0 / totalETAs));
            System.out.printf("Confiabilidad Media: %d/%d (%.1f%%)\n",
                    mediumConfidence, totalETAs, (mediumConfidence * 100.0 / totalETAs));
            System.out.printf("Confiabilidad Baja:  %d/%d (%.1f%%)\n",
                    lowConfidence, totalETAs, (lowConfidence * 100.0 / totalETAs));
            System.out.println();

            // CASOS DE USO
            System.out.println("═══════════════════════════════════════════════════════════");
            System.out.println("  CASOS DE USO DEL SERVICIO ETA");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            System.out.println("1. INFORMACIÓN EN TIEMPO REAL PARA PASAJEROS");
            System.out.println("   Mostrar en app móvil cuándo llegará el próximo bus");
            System.out.println("   Ejemplo: 'Bus 101 llegará en 3.5 minutos (confianza: Alta)'");

            System.out.println("\n2. OPTIMIZACIÓN DE TIEMPOS DE ESPERA");
            System.out.println("   Alertar a pasajeros cuando el bus esté cerca");
            System.out.println("   Ejemplo: 'Su bus llegará en menos de 5 minutos'");

            System.out.println("\n3. PLANIFICACIÓN DE CONEXIONES");
            System.out.println("   Calcular si hay tiempo suficiente para transbordo");
            System.out.println("   Ejemplo: 'Tiene 8 minutos para cambiar al bus 205'");

            System.out.println("\n4. MONITOREO DE FLOTA");
            System.out.println("   Control central puede ver ETAs de todos los buses");
            System.out.println("   Ejemplo: Dashboard con 300 buses y sus ETAs actualizados");

            System.out.println("\n5. AJUSTE DINÁMICO DE FRECUENCIA");
            System.out.println("   Detectar buses retrasados y ajustar operación");
            System.out.println("   Ejemplo: 'Bus 103 con retraso de 10 min, enviar bus de apoyo'");

            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  PRUEBA COMPLETADA");
            System.out.println("═══════════════════════════════════════════════════════════");

        } catch (IOException e) {
            System.err.println("✗ Error de I/O: " + e.getMessage());
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

    /**
     * Crea un bus simulado con datos realistas
     */
    private static BusPosition createSimulatedBus(GrafoMIO grafo, int busId,
                                                   double velocity, boolean moving) {
        Random random = new Random(busId);

        // Seleccionar un arco aleatorio del grafo
        List<Arco> arcos = grafo.getArcos();
        if (arcos.isEmpty()) {
            throw new IllegalStateException("No hay arcos en el grafo");
        }

        Arco randomArco = arcos.get(random.nextInt(arcos.size()));

        // Simular posición entre origen y destino
        Parada origin = randomArco.getParadaOrigen();
        Parada dest = randomArco.getParadaDestino();

        double lat = origin.getDecimalLatitude() + (dest.getDecimalLatitude() - origin.getDecimalLatitude()) * 0.3;
        double lon = origin.getDecimalLongitude() + (dest.getDecimalLongitude() - origin.getDecimalLongitude()) * 0.3;

        return new BusPosition.Builder()
                .busId(busId)
                .routeId(busId % 100)  // Simular routeId basado en busId
                .lineId(randomArco.getLineId())
                .latitude(lat)
                .longitude(lon)
                .velocity(moving ? velocity : 0.5)
                .currentStopId(origin.getStopId())
                .nextStopId(dest.getStopId())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
