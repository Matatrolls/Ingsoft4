package co.edu.icesi.mio.view.console;

import co.edu.icesi.mio.controller.console.ConsoleRouter;
import co.edu.icesi.mio.model.core.RoleType;

import java.util.Scanner;

public class MainConsoleView {

    private final ConsoleRouter router;
    private final Scanner scanner = new Scanner(System.in);

    public MainConsoleView(ConsoleRouter router) {
        this.router = router;
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("============================");
            System.out.println("  SITM-MIO - Cliente");
            System.out.println("============================");
            System.out.println("Seleccione su rol:");
            System.out.println("1. Usuario");
            System.out.println("2. Conductor");
            System.out.println("3. Controlador");
            System.out.println("0. Salir");
            System.out.print("Opción: ");

            String option = scanner.nextLine();
            switch (option) {
                case "1" -> running = router.handleRole(RoleType.USUARIO, scanner);
                case "2" -> running = router.handleRole(RoleType.CONDUCTOR, scanner);
                case "3" -> running = router.handleRole(RoleType.CONTROLADOR, scanner);
                case "4" -> running = router.handleRole(RoleType.ADMIN, scanner);
                case "0" -> running = false;
                default -> System.out.println("Opción inválida.");
            }
        }
        System.out.println("Cerrando cliente SITM-MIO.");
        System.exit(0);
    }
}
