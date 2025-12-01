package co.edu.icesi.mio.controller.console;

import co.edu.icesi.mio.infra.ice.AdminChannelClient;
import co.edu.icesi.mio.model.events.BusEvent;
import co.edu.icesi.mio.model.events.BusEventSource;
import co.edu.icesi.mio.model.events.EventType;
import co.edu.icesi.mio.service.ingestion.BusEventHandler;

import java.util.Scanner;

/**
 * Controlador de consola para el rol de Conductor.
 * Permite a los conductores reportar eventos desde sus buses.
 */
public class DriverConsoleController {

    private final Scanner scanner;
    private final BusEventSource eventSource;
    private final AdminChannelClient adminClient;
    private final BusEventHandler eventHandler;
    private String currentBusId;

    public DriverConsoleController(Scanner scanner,
                                  BusEventSource busEventSource,
                                  AdminChannelClient adminClient,
                                  BusEventHandler eventHandler) {
        this.scanner = scanner;
        this.eventSource = busEventSource;
        this.adminClient = adminClient;
        this.eventHandler = eventHandler;
    }

    public void run() {
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║          INTERFAZ DE CONDUCTOR - BUS MIO              ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");

        // Solicitar ID del bus al inicio
        System.out.print("\n🚍 Ingrese el ID de su bus: ");
        currentBusId = scanner.nextLine().trim().toUpperCase();

        if (currentBusId.isEmpty()) {
            currentBusId = "BUS-" + (int)(Math.random() * 1000);
            System.out.println(" ID generado automáticamente: " + currentBusId);
        }

        System.out.println("✓ Conductor asignado al bus: " + currentBusId);
        System.out.println();

        boolean exit = false;

        while (!exit) {
            System.out.println("════════════════════════════════════");
            System.out.println("   MENÚ CONDUCTOR - Bus " + currentBusId);
            System.out.println("════════════════════════════════════");
            System.out.println("1. Reportar evento");
            System.out.println("2. Reportar emergencia rápida");
            System.out.println("3. Enviar actualización GPS");
            System.out.println("0. ← Volver");
            System.out.println("════════════════════════════════════");
            System.out.print("Opción: ");
            String option = scanner.nextLine();

            switch (option) {
                case "1" -> handleNewEvent();
                case "2" -> handleQuickEmergency();
                case "3" -> handleGPSUpdate();
                case "0" -> exit = true;
                default -> System.out.println("Opción inválida");
            }
        }

        System.out.println("✓ Conductor desconectado del bus " + currentBusId);
    }

    private void handleNewEvent() {
        System.out.println("\n════════════════════════════════════");
        System.out.println("   REPORTAR EVENTO");
        System.out.println("════════════════════════════════════");
        System.out.println("Seleccione el tipo de evento:");
        System.out.println();
        System.out.println("═══ EMERGENCIAS (Prioridad CRÍTICA) ═══");
        System.out.println("1.  Emergencia médica");
        System.out.println("2.  Choque del bus");
        System.out.println("3.  Atraco");
        System.out.println();
        System.out.println("═══ MECÁNICO (Prioridad ALTA) ═══");
        System.out.println("4.  Pinchazo de llanta");
        System.out.println("5.  Avería grave de motor");
        System.out.println("6.  Sobrecalentamiento");
        System.out.println();
        System.out.println("═══ TRÁNSITO/OPERACIONAL (Prioridad MEDIA/ALTA) ═══");
        System.out.println("7.  Trancón en la vía");
        System.out.println("8.  Manifestación/marcha");
        System.out.println("9.  Retraso del bus");
        System.out.println("10. Falta de gasolina");
        System.out.println();
        System.out.println("═══ OPERACIONAL (Prioridad BAJA) ═══");
        System.out.println("11. Apertura de puerta");
        System.out.println("12. Cierre de puerta");
        System.out.println();
        System.out.print("Opción: ");
        String opt = scanner.nextLine();

        EventType type = switch (opt) {
            case "1" -> EventType.EMERGENCIA;
            case "2" -> EventType.CHOQUE;
            case "3" -> EventType.ATRACO;
            case "4" -> EventType.PINCHADO;
            case "5" -> EventType.AVERIA_MOTOR;
            case "6" -> EventType.SOBRECALENTAMIENTO;
            case "7" -> EventType.TRANCON;
            case "8" -> EventType.MARCHA;
            case "9" -> EventType.RETRASO;
            case "10" -> EventType.FALTAGASOLINA;
            case "11" -> EventType.PUERTA_ABIERTA;
            case "12" -> EventType.PUERTA_CERRADA;
            default -> {
                System.out.println("Opción inválida. Usando 'Incidente' por defecto.");
                yield EventType.INCIDENTE;
            }
        };

        System.out.print("\n📝 Descripción adicional (opcional): ");
        String desc = scanner.nextLine().trim();

        if (desc.isEmpty()) {
            desc = "Evento reportado por conductor";
        }

        // Crear y enviar evento
        BusEvent event = new BusEvent(currentBusId, type, desc);

        // Procesar evento localmente
        eventHandler.onBusEvent(event);

        // ✅ NUEVO: Enviar evento al servidor Ice para que otros clientes lo vean
        try {
            co.edu.icesi.mio.infra.ice.BusEventData iceEvent =
                    new co.edu.icesi.mio.infra.ice.BusEventData(
                            event.getId(),
                            currentBusId,
                            type.name(),
                            type.getPriority().name(),
                            desc,
                            0.0, // Latitud (por ahora 0, en producción sería GPS real)
                            0.0  // Longitud
                    );

            adminClient.reportBusEvent(iceEvent);
            System.out.println("📡 Evento enviado al servidor centralizado");
        } catch (Exception e) {
            System.err.println("No se pudo enviar al servidor: " + e.getMessage());
        }

        System.out.println();
        System.out.println("════════════════════════════════════");
        System.out.println("   EVENTO ENVIADO EXITOSAMENTE");
        System.out.println("════════════════════════════════════");
        System.out.println("Bus: " + currentBusId);
        System.out.println("Tipo: " + type.getDescription());
        System.out.println("Prioridad: " + type.getPriority().getDescription());
        System.out.println("Categoría: " + type.getCategory().getDescription());
        System.out.println("════════════════════════════════════");
        System.out.println();

        pauseForUser();
    }

    private void handleQuickEmergency() {
        System.out.println("\n════════════════════════════════════");
        System.out.println("   REPORTE DE EMERGENCIA RÁPIDO");
        System.out.println("════════════════════════════════════");
        System.out.println("1. Emergencia médica");
        System.out.println("2. Choque");
        System.out.println("3. Atraco");
        System.out.println("4. Avería crítica");
        System.out.print("\nSeleccione emergencia: ");
        String opt = scanner.nextLine();

        EventType type = switch (opt) {
            case "1" -> EventType.EMERGENCIA;
            case "2" -> EventType.CHOQUE;
            case "3" -> EventType.ATRACO;
            case "4" -> EventType.AVERIA_MOTOR;
            default -> EventType.EMERGENCIA;
        };

        BusEvent event = new BusEvent(currentBusId, type, "EMERGENCIA - Atención inmediata requerida");
        eventHandler.onBusEvent(event);

        // Enviar al servidor Ice
        try {
            co.edu.icesi.mio.infra.ice.BusEventData iceEvent =
                    new co.edu.icesi.mio.infra.ice.BusEventData(
                            event.getId(),
                            currentBusId,
                            type.name(),
                            type.getPriority().name(),
                            "EMERGENCIA - Atención inmediata requerida",
                            0.0, 0.0
                    );
            adminClient.reportBusEvent(iceEvent);
        } catch (Exception e) {
            System.err.println("Error enviando al servidor: " + e.getMessage());
        }

        System.out.println("\nALERTA DE EMERGENCIA ENVIADA");
        System.out.println("Los controladores han sido notificados en todos los clientes\n");
        pauseForUser();
    }

    private void handleGPSUpdate() {
        BusEvent event = new BusEvent(
                currentBusId,
                EventType.GPS_UPDATE,
                "Actualización de posición GPS"
        );
        eventHandler.onBusEvent(event);
        System.out.println("\n✓ Actualización GPS enviada\n");
    }

    private void pauseForUser() {
        System.out.print("Presione Enter para continuar...");
        scanner.nextLine();
    }
}
