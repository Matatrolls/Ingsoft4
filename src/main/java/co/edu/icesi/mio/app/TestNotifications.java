package co.edu.icesi.mio.app;

import co.edu.icesi.mio.concurrency.ConcurrencyManager;
import co.edu.icesi.mio.model.events.BusEvent;
import co.edu.icesi.mio.model.events.BusEventStore;
import co.edu.icesi.mio.model.events.EventType;
import co.edu.icesi.mio.model.notifications.Notification;
import co.edu.icesi.mio.model.notifications.NotificationListener;
import co.edu.icesi.mio.service.ingestion.BusEventHandler;
import co.edu.icesi.mio.service.notifications.NotificationService;

/**
 * Programa de prueba para verificar el sistema de notificaciones.
 * Simula el envío de eventos desde un conductor y la recepción por parte de controladores.
 */
public class TestNotifications {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║   PRUEBA DE SISTEMA DE NOTIFICACIONES SITM-MIO        ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        // 1. Inicializar componentes
        ConcurrencyManager concurrencyManager = new ConcurrencyManager(4, 2);
        BusEventStore eventStore = new BusEventStore();
        NotificationService notificationService = new NotificationService(concurrencyManager);
        BusEventHandler eventHandler = new BusEventHandler(concurrencyManager, eventStore, notificationService);

        System.out.println("✓ Componentes inicializados\n");

        // 2. Crear controladores simulados
        TestControllerListener controlador1 = new TestControllerListener("CTRL-001");
        TestControllerListener controlador2 = new TestControllerListener("CTRL-002");

        // 3. Suscribir controladores
        notificationService.subscribe(controlador1);
        notificationService.subscribe(controlador2);

        System.out.println("✓ Controladores suscritos: " + notificationService.getListenerCount() + "\n");

        // 4. Simular eventos desde conductores
        System.out.println("════════════════════════════════════════════════════════");
        System.out.println("SIMULANDO EVENTOS DESDE CONDUCTORES");
        System.out.println("════════════════════════════════════════════════════════\n");

        // Evento 1: Emergencia médica (CRÍTICA)
        System.out.println(">>> Conductor reporta EMERGENCIA MÉDICA...");
        BusEvent evento1 = new BusEvent("BUS-101", EventType.EMERGENCIA, "Pasajero con dolor de pecho");
        eventHandler.onBusEvent(evento1);
        Thread.sleep(500); // Dar tiempo al procesamiento asíncrono

        // Evento 2: Actualización GPS (BAJA - NO debe notificar)
        System.out.println(">>> Conductor envía actualización GPS...");
        BusEvent evento2 = new BusEvent("BUS-102", EventType.GPS_UPDATE, "Posición actualizada");
        eventHandler.onBusEvent(evento2);
        Thread.sleep(500);

        // Evento 3: Choque (CRÍTICA)
        System.out.println(">>> Conductor reporta CHOQUE...");
        BusEvent evento3 = new BusEvent("BUS-103", EventType.CHOQUE, "Colisión con vehículo particular");
        eventHandler.onBusEvent(evento3);
        Thread.sleep(500);

        // Evento 4: Pinchazo (ALTA)
        System.out.println(">>> Conductor reporta PINCHAZO...");
        BusEvent evento4 = new BusEvent("BUS-104", EventType.PINCHADO, "Llanta trasera derecha");
        eventHandler.onBusEvent(evento4);
        Thread.sleep(500);

        // Evento 5: Apertura de puerta (BAJA - NO debe notificar)
        System.out.println(">>> Conductor abre puerta...");
        BusEvent evento5 = new BusEvent("BUS-105", EventType.PUERTA_ABIERTA, "Estación Terminal");
        eventHandler.onBusEvent(evento5);
        Thread.sleep(500);

        // Evento 6: Trancón (MEDIA - NO debe notificar, solo ALTA/CRÍTICA)
        System.out.println(">>> Conductor reporta TRANCÓN...");
        BusEvent evento6 = new BusEvent("BUS-106", EventType.TRANCON, "Calle 5 con Carrera 100");
        eventHandler.onBusEvent(evento6);
        Thread.sleep(500);

        // 5. Mostrar resumen
        System.out.println("\n════════════════════════════════════════════════════════");
        System.out.println("RESUMEN DE PRUEBA");
        System.out.println("════════════════════════════════════════════════════════");
        System.out.println("Eventos enviados: 6");
        System.out.println("Eventos almacenados: " + eventStore.getLastEvents(10).size());
        System.out.println("Notificaciones generadas: " + notificationService.getNotificationCount());
        System.out.println("Notificaciones críticas: " + notificationService.getCriticalNotifications(10).size());
        System.out.println();
        System.out.println("Notificaciones recibidas por CTRL-001: " + controlador1.getNotificationCount());
        System.out.println("Notificaciones recibidas por CTRL-002: " + controlador2.getNotificationCount());
        System.out.println();

        // 6. Verificar resultados esperados
        System.out.println("════════════════════════════════════════════════════════");
        System.out.println("VERIFICACIÓN DE RESULTADOS");
        System.out.println("════════════════════════════════════════════════════════");

        int expectedNotifications = 3; // EMERGENCIA (CRÍTICA) + CHOQUE (CRÍTICA) + PINCHADO (ALTA)
        // GPS_UPDATE (BAJA), PUERTA_ABIERTA (BAJA), TRANCON (MEDIA) NO deben notificar

        if (controlador1.getNotificationCount() == expectedNotifications) {
            System.out.println("✅ CTRL-001: Recibió " + expectedNotifications + " notificaciones (correcto)");
        } else {
            System.out.println("❌ CTRL-001: Esperado " + expectedNotifications + ", recibido " + controlador1.getNotificationCount());
        }

        if (controlador2.getNotificationCount() == expectedNotifications) {
            System.out.println("✅ CTRL-002: Recibió " + expectedNotifications + " notificaciones (correcto)");
        } else {
            System.out.println("❌ CTRL-002: Esperado " + expectedNotifications + ", recibido " + controlador2.getNotificationCount());
        }

        if (eventStore.getLastEvents(10).size() == 6) {
            System.out.println("✅ Todos los eventos fueron almacenados (correcto)");
        } else {
            System.out.println("❌ Eventos almacenados: " + eventStore.getLastEvents(10).size() + " de 6");
        }

        if (notificationService.getNotificationCount() == expectedNotifications) {
            System.out.println("✅ Solo se notificaron eventos de prioridad ALTA/CRÍTICA (correcto)");
        } else {
            System.out.println("⚠️  Notificaciones generadas: " + notificationService.getNotificationCount());
        }

        System.out.println("\n════════════════════════════════════════════════════════");
        System.out.println("RESULTADO FINAL");
        System.out.println("════════════════════════════════════════════════════════");

        boolean success = controlador1.getNotificationCount() == expectedNotifications &&
                         controlador2.getNotificationCount() == expectedNotifications &&
                         eventStore.getLastEvents(10).size() == 6;

        if (success) {
            System.out.println("✅✅✅ PRUEBA EXITOSA ✅✅✅");
            System.out.println("\nEl sistema de notificaciones funciona correctamente:");
            System.out.println("  • Conductores pueden enviar eventos ✓");
            System.out.println("  • Eventos se almacenan correctamente ✓");
            System.out.println("  • Solo eventos ALTA/CRÍTICA generan notificaciones ✓");
            System.out.println("  • Múltiples controladores reciben notificaciones ✓");
            System.out.println("  • Procesamiento asíncrono funciona ✓");
        } else {
            System.out.println("❌ PRUEBA FALLIDA - Revisar implementación");
        }

        System.out.println("════════════════════════════════════════════════════════\n");

        // 7. Cleanup
        concurrencyManager.shutdown();
        System.out.println("✓ Sistema cerrado correctamente");
    }

    /**
     * Listener de prueba que simula un controlador
     */
    static class TestControllerListener implements NotificationListener {
        private final String id;
        private int notificationCount = 0;

        public TestControllerListener(String id) {
            this.id = id;
        }

        @Override
        public void onNotification(Notification notification) {
            notificationCount++;
            System.out.println();
            System.out.println("  🔔 [" + id + "] NOTIFICACIÓN RECIBIDA #" + notificationCount);
            System.out.println("      " + notification);
            System.out.println();
        }

        @Override
        public String getListenerId() {
            return id;
        }

        public int getNotificationCount() {
            return notificationCount;
        }
    }
}
