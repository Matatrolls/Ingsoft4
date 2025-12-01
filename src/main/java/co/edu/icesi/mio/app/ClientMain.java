package co.edu.icesi.mio.app;

import co.edu.icesi.mio.concurrency.ConcurrencyManager;
import co.edu.icesi.mio.controller.console.ConsoleRouter;
import co.edu.icesi.mio.infra.ice.AdminChannelClient;
import co.edu.icesi.mio.model.events.BusEventSource;
import co.edu.icesi.mio.model.events.BusEventStore;
import co.edu.icesi.mio.service.analytics.BusStatusService;
import co.edu.icesi.mio.service.ingestion.BusEventHandler;
import co.edu.icesi.mio.service.notifications.NotificationService;
import co.edu.icesi.mio.service.zone.ZoneService;
import co.edu.icesi.mio.view.console.MainConsoleView;

/**
 * Punto de entrada del cliente del sistema SITM-MIO.
 * Inicializa todos los servicios y componentes necesarios para la aplicación.
 */
public class ClientMain {

    public static void main(String[] args) throws java.io.IOException {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║        SISTEMA SITM-MIO - CLIENTE                      ║");
        System.out.println("║        Sistema de Gestión de Transporte Masivo         ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println();

        // 1. Inicializar gestor de concurrencia
        ConcurrencyManager concurrencyManager = new ConcurrencyManager(4, 2);
        System.out.println("✓ Gestor de concurrencia inicializado (4 threads)");

        // 2. Inicializar almacén de eventos
        BusEventStore eventStore = new BusEventStore();
        System.out.println("✓ Almacén de eventos inicializado");

        // 3. Inicializar servicio de notificaciones (Patrón Observer)
        NotificationService notificationService = new NotificationService(concurrencyManager);
        System.out.println("✓ Servicio de notificaciones inicializado");

        // 4. Inicializar manejador de eventos de buses
        BusEventHandler eventHandler = new BusEventHandler(
                concurrencyManager,
                eventStore,
                notificationService
        );
        System.out.println("✓ Manejador de eventos de buses inicializado");

        // 5. Inicializar fuente de eventos
        BusEventSource eventSource = new BusEventSource(concurrencyManager, eventStore);
        System.out.println("✓ Fuente de eventos inicializada");

        // 6. Inicializar servicios de negocio
        BusStatusService busStatusService = new BusStatusService(eventStore);
        ZoneService zoneService = new ZoneService();
        System.out.println("✓ Servicios de negocio inicializados");

        // 7. Conectar con servidor Ice
        AdminChannelClient adminClient = new AdminChannelClient("localhost");
        System.out.println("✓ Cliente Ice conectado");

        // 8. Cargar grafo local y servicios de routing
        System.out.println("✓ Cargando grafo del MIO y servicios de routing...");
        co.edu.icesi.mio.service.analytics.MioGraphRepository mioGraphRepo =
                new co.edu.icesi.mio.service.analytics.MioGraphRepository();
        co.edu.icesi.mio.infra.csv.GrafoMIO grafoMIO = mioGraphRepo.getGrafo();

        // Calcular velocidades históricas (limitado a 15,000 datagramas)
        co.edu.icesi.mio.service.analytics.ArcVelocityCalculator velocityCalc =
                new co.edu.icesi.mio.service.analytics.ArcVelocityCalculator();
        try {
            velocityCalc.processDatagramFile("src/main/resources/data/datagrams4streaming.csv");
        } catch (java.io.IOException e) {
            System.err.println("  No se pudieron cargar velocidades históricas: " + e.getMessage());
        }

        co.edu.icesi.mio.repository.ArcVelocityRepository velocityRepo =
                new co.edu.icesi.mio.repository.ArcVelocityRepository();
        velocityRepo.saveAll(velocityCalc.calculateStatistics());

        co.edu.icesi.mio.service.routing.RouteCalculatorService routeCalculator =
                new co.edu.icesi.mio.service.routing.RouteCalculatorService(grafoMIO, velocityRepo);

        co.edu.icesi.mio.service.realtime.ETACalculatorService etaCalculator =
                new co.edu.icesi.mio.service.realtime.ETACalculatorService(grafoMIO, velocityRepo);

        System.out.println("✓ Grafo cargado: " + grafoMIO.getParadas().size() + " paradas, "
                + grafoMIO.getRutas().size() + " rutas");
        System.out.println("✓ Servicios de routing y ETA inicializados");

        // 9. Inicializar enrutador de consola
        ConsoleRouter router = new ConsoleRouter(
                eventSource,
                eventStore,
                busStatusService,
                notificationService,
                adminClient,
                eventHandler,
                grafoMIO,
                routeCalculator,
                etaCalculator
        );
        System.out.println("✓ Enrutador de consola inicializado");

        System.out.println();
        System.out.println("════════════════════════════════════════════════════════");
        System.out.println(" Sistema listo. Iniciando interfaz de usuario...");
        System.out.println("════════════════════════════════════════════════════════");
        System.out.println();

        // 10. Iniciar vista principal
        MainConsoleView mainConsoleView = new MainConsoleView(router);
        mainConsoleView.start();

        // 11. Cleanup al cerrar
        System.out.println("\n✓ Cerrando aplicación...");
        adminClient.close();
        concurrencyManager.shutdown();
        System.out.println("✓ Sistema cerrado correctamente");
    }
}
