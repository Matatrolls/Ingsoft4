package co.edu.icesi.mio.controller.console;

import co.edu.icesi.mio.infra.csv.GrafoMIO;
import co.edu.icesi.mio.infra.ice.AdminChannelClient;
import co.edu.icesi.mio.model.core.RoleType;
import co.edu.icesi.mio.model.events.BusEventSource;
import co.edu.icesi.mio.model.events.BusEventStore;
import co.edu.icesi.mio.service.analytics.BusStatusService;
import co.edu.icesi.mio.service.ingestion.BusEventHandler;
import co.edu.icesi.mio.service.notifications.NotificationService;
import co.edu.icesi.mio.service.realtime.ETACalculatorService;
import co.edu.icesi.mio.service.routing.RouteCalculatorService;

import java.util.Scanner;

/**
 * Enrutador de consola que dirige a los diferentes controladores según el rol del usuario.
 */
public class ConsoleRouter {

    private final BusEventSource busEventSource;
    private final BusEventStore busEventStore;
    private final BusStatusService busStatusService;
    private final NotificationService notificationService;
    private final AdminChannelClient adminClient;
    private final BusEventHandler eventHandler;
    private final GrafoMIO grafoMIO;
    private final RouteCalculatorService routeCalculator;
    private final ETACalculatorService etaCalculator;

    public ConsoleRouter(BusEventSource busEventSource,
                         BusEventStore busEventStore,
                         BusStatusService busStatusService,
                         NotificationService notificationService,
                         AdminChannelClient adminClient,
                         BusEventHandler eventHandler,
                         GrafoMIO grafoMIO,
                         RouteCalculatorService routeCalculator,
                         ETACalculatorService etaCalculator) {
        this.busEventSource = busEventSource;
        this.busEventStore = busEventStore;
        this.busStatusService = busStatusService;
        this.notificationService = notificationService;
        this.adminClient = adminClient;
        this.eventHandler = eventHandler;
        this.grafoMIO = grafoMIO;
        this.routeCalculator = routeCalculator;
        this.etaCalculator = etaCalculator;
    }

    public boolean handleRole(RoleType role, Scanner scanner) {
        return switch (role) {
            case USUARIO -> handleUsuario(scanner);
            case CONDUCTOR -> handleConductor(scanner);
            case CONTROLADOR -> handleControlador(scanner);
            case ADMIN -> handleAdmin(scanner);
        };
    }

    private boolean handleUsuario(Scanner scanner) {
        UserConsoleController controller =
                new UserConsoleController(scanner, busStatusService, adminClient,
                        grafoMIO, routeCalculator, etaCalculator);
        controller.run();
        return true;
    }

    private boolean handleControlador(Scanner scanner) {
        // Solicitar ID del controlador
        System.out.print("\n🆔 Ingrese su ID de controlador: ");
        String controllerId = scanner.nextLine().trim().toUpperCase();

        if (controllerId.isEmpty()) {
            controllerId = "CTRL-" + System.currentTimeMillis() % 1000;
            System.out.println("ID generado automáticamente: " + controllerId);
        }

        ControllerConsoleController controller = new ControllerConsoleController(
                controllerId,
                scanner,
                busStatusService,
                adminClient,
                notificationService
        );
        controller.run();
        return true;
    }

    private boolean handleConductor(Scanner scanner) {
        DriverConsoleController controller =
                new DriverConsoleController(scanner, busEventSource, adminClient, eventHandler);
        controller.run();
        return true;
    }

    private boolean handleAdmin(Scanner scanner) {
        AdminConsoleController controller = new AdminConsoleController(scanner, adminClient);
        controller.run();
        return true;
    }
}
