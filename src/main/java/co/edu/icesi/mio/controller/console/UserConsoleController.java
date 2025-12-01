package co.edu.icesi.mio.controller.console;

import co.edu.icesi.mio.infra.csv.GrafoMIO;
import co.edu.icesi.mio.infra.csv.Parada;
import co.edu.icesi.mio.infra.ice.AdminChannelClient;
import co.edu.icesi.mio.model.realtime.BusETA;
import co.edu.icesi.mio.service.analytics.BusStatusService;
import co.edu.icesi.mio.service.realtime.ETACalculatorService;
import co.edu.icesi.mio.service.routing.RouteCalculatorService;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public class UserConsoleController {

    private final Scanner scanner;
    private final BusStatusService busStatusService;
    private final AdminChannelClient adminClient;
    private final GrafoMIO grafoMIO;
    private final RouteCalculatorService routeCalculator;
    private final ETACalculatorService etaCalculator;

    public UserConsoleController(Scanner scanner, BusStatusService busStatusService,
                                 AdminChannelClient adminClient, GrafoMIO grafoMIO,
                                 RouteCalculatorService routeCalculator,
                                 ETACalculatorService etaCalculator) {
        this.scanner = scanner;
        this.busStatusService = busStatusService;
        this.adminClient = adminClient;
        this.grafoMIO = grafoMIO;
        this.routeCalculator = routeCalculator;
        this.etaCalculator = etaCalculator;
    }



    public void run() {
        boolean exit = false;
        while (!exit) {
            System.out.println("============================");
            System.out.println(" Menú usuario ");
            System.out.println("============================");
            System.out.println("1. Ver estado de buses");
            System.out.println("2. Ver rutas");
            System.out.println("3. Ver informacion de ruta");
            System.out.println("0. Volver");
            System.out.print("Opción: ");
            String option = scanner.nextLine();

            switch (option) {
                case "1":
                    handleBusStatus();
                    break;
                case "2":
                    AdminChannelClient.GraphStats stats = adminClient.getGraphStats();
                    System.out.println("Rutas:   " + stats.lines);

                    break;
                case "3":
                    handleRouteInfo();
                    break;
                case "0":
                    exit = true;
                    break;
                default:
                    System.out.println("Opción inválida");
            }
        }
    }

    private void handleBusStatus() {
        System.out.print("Ingrese el id del bus: ");
        String code = scanner.nextLine().trim().toUpperCase();
        busStatusService.getRecentEventsForBus(code,1);
    }

    private void handleRouteInfo() {
        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("  CÁLCULO DE RUTA Y TIEMPO ESTIMADO");
        System.out.println("═══════════════════════════════════════════════════════════\n");

        // Paso 1: Buscar parada de origen
        System.out.print("Ingrese nombre o parte del nombre de la parada de ORIGEN: ");
        String origenQuery = scanner.nextLine().trim();

        if (origenQuery.isEmpty()) {
            System.out.println("Debe ingresar un nombre de parada");
            return;
        }

        List<Parada> paradasOrigen = buscarParadas(origenQuery);
        if (paradasOrigen.isEmpty()) {
            System.out.println("No se encontraron paradas con ese nombre");
            return;
        }

        Parada paradaOrigen = seleccionarParada(paradasOrigen, "ORIGEN");
        if (paradaOrigen == null) return;

        // Paso 2: Buscar parada de destino
        System.out.print("\nIngrese nombre o parte del nombre de la parada de DESTINO: ");
        String destinoQuery = scanner.nextLine().trim();

        if (destinoQuery.isEmpty()) {
            System.out.println("Debe ingresar un nombre de parada");
            return;
        }

        List<Parada> paradasDestino = buscarParadas(destinoQuery);
        if (paradasDestino.isEmpty()) {
            System.out.println("No se encontraron paradas con ese nombre");
            return;
        }

        Parada paradaDestino = seleccionarParada(paradasDestino, "DESTINO");
        if (paradaDestino == null) return;

        // Paso 3: Calcular ruta usando Dijkstra
        System.out.println("\nCalculando ruta más rápida usando algoritmo de Dijkstra...\n");

        co.edu.icesi.mio.model.routing.CalculatedRoute resultado = routeCalculator.calculateFastestRoute(
                paradaOrigen.getStopId(),
                paradaDestino.getStopId()
        );

        if (resultado == null || !resultado.isFound()) {
            System.out.println("No se encontró una ruta entre estas paradas");
            return;
        }

        // Paso 4: Mostrar resultados
        mostrarResultadoRuta(paradaOrigen, paradaDestino, resultado);
    }

    /**
     * Busca paradas que contengan el texto ingresado (case-insensitive)
     */
    private List<Parada> buscarParadas(String query) {
        String queryLower = query.toLowerCase();
        return grafoMIO.getParadas().values().stream()
                .filter(p -> p.getLongName().toLowerCase().contains(queryLower) ||
                             p.getShortName().toLowerCase().contains(queryLower))
                .limit(10) // Limitar a 10 resultados
                .collect(Collectors.toList());
    }

    /**
     * Permite al usuario seleccionar una parada de una lista
     */
    private Parada seleccionarParada(List<Parada> paradas, String tipo) {
        if (paradas.size() == 1) {
            Parada parada = paradas.get(0);
            System.out.println("✓ Parada " + tipo + " seleccionada: " + parada.getLongName());
            return parada;
        }

        System.out.println("\nSe encontraron " + paradas.size() + " paradas:");
        for (int i = 0; i < paradas.size(); i++) {
            Parada p = paradas.get(i);
            System.out.printf("%d. %s (%s)\n", i + 1, p.getLongName(), p.getShortName());
        }

        System.out.print("\nSeleccione el número de la parada de " + tipo + " [1-" + paradas.size() + "]: ");
        String input = scanner.nextLine().trim();

        try {
            int seleccion = Integer.parseInt(input);
            if (seleccion < 1 || seleccion > paradas.size()) {
                System.out.println("Opción inválida");
                return null;
            }
            Parada paradaSeleccionada = paradas.get(seleccion - 1);
            System.out.println("✓ Seleccionado: " + paradaSeleccionada.getLongName());
            return paradaSeleccionada;
        } catch (NumberFormatException e) {
            System.out.println("Debe ingresar un número");
            return null;
        }
    }

    /**
     * Muestra el resultado de la ruta calculada con Dijkstra y el tiempo estimado
     */
    private void mostrarResultadoRuta(Parada origen, Parada destino,
                                      co.edu.icesi.mio.model.routing.CalculatedRoute resultado) {
        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("  RESULTADO DE LA RUTA");
        System.out.println("═══════════════════════════════════════════════════════════");

        System.out.println("\nORIGEN:  " + origen.getLongName());
        System.out.println(" DESTINO: " + destino.getLongName());
        System.out.println();

        // Información de la ruta
        List<Parada> paradas = resultado.getStopsInOrder();
        System.out.printf("Número de paradas: %d\n", paradas.size());
        System.out.printf("Distancia total: %.2f km\n", resultado.getTotalDistance() / 1000.0);
        System.out.printf("Tiempo estimado: %.1f minutos\n", resultado.getTotalTime());
        System.out.printf("Transbordos: %d\n", resultado.getTransferCount());

        // Mostrar velocidad promedio
        System.out.printf("Velocidad promedio: %.1f km/h\n", resultado.getAverageVelocity());

        // Mostrar líneas usadas
        List<String> lineas = resultado.getLinesUsed();
        System.out.printf("Líneas a tomar: %s\n", String.join(" → ", lineas));

        // Mostrar camino
        System.out.println("\n  CAMINO A SEGUIR:");
        System.out.println("─".repeat(60));

        for (int i = 0; i < paradas.size(); i++) {
            Parada parada = paradas.get(i);
            String indicador = i == 0 ? "🟢" : (i == paradas.size() - 1 ? "🔴" : "⚪");
            System.out.printf("%s %d. %s\n", indicador, i + 1, parada.getLongName());
        }

        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("Nota: Los tiempos se calculan usando datos históricos de");
        System.out.println("velocidad y el algoritmo de Dijkstra para la ruta óptima.");
        System.out.println("═══════════════════════════════════════════════════════════\n");
    }

}
