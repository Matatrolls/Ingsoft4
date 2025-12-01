package co.edu.icesi.mio.controller.console;

import co.edu.icesi.mio.infra.ice.AdminChannelClient;
import co.edu.icesi.mio.infra.ice.AdminChannelServant;

import java.util.Scanner;

public class AdminConsoleController {

    private final Scanner scanner;
    private final AdminChannelClient adminClient;

    public AdminConsoleController(Scanner scanner, AdminChannelClient adminClient) {
        this.scanner = scanner;
        this.adminClient = adminClient;
    }

    public void run() {
        boolean exit = false;
        while (!exit) {
            System.out.println("=== Admin ===");
            System.out.println("1. Enviar broadcast");
            System.out.println("0. Volver");
            System.out.print("Opción: ");
            String opt = scanner.nextLine();
            switch (opt) {
                case "1" -> {
                    System.out.print("Mensaje: ");
                    String msg = scanner.nextLine();
                    //adminClient.sendBroadcast(msg);
                }
                case "0" -> exit = true;
                default -> System.out.println("Opción inválida");
            }
        }
    }
}
