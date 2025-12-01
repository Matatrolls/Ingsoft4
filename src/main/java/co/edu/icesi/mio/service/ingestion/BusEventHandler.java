package co.edu.icesi.mio.service.ingestion;

import co.edu.icesi.mio.concurrency.ConcurrencyManager;
import co.edu.icesi.mio.model.events.BusEvent;
import co.edu.icesi.mio.model.events.BusEventListener;
import co.edu.icesi.mio.model.events.BusEventStore;
import co.edu.icesi.mio.service.notifications.NotificationService;

/**
 * Manejador de eventos de buses.
 * Procesa eventos entrantes, los almacena y notifica a los controladores suscritos.
 */
public class BusEventHandler implements BusEventListener {

    private final ConcurrencyManager concurrencyManager;
    private final BusEventStore store;
    private final NotificationService notificationService;

    public BusEventHandler(ConcurrencyManager concurrencyManager,
                          BusEventStore store,
                          NotificationService notificationService) {
        this.concurrencyManager = concurrencyManager;
        this.store = store;
        this.notificationService = notificationService;
    }

    @Override
    public void onBusEvent(BusEvent event) {
        concurrencyManager.run(() -> processEvent(event));
    }

    private void processEvent(BusEvent event) {
        // 1. Almacenar el evento
        store.add(event);

        // 2. Log del evento
        System.out.println();
        System.out.println("═══════════════════════════════════════");
        System.out.println("📡 EVENTO DE BUS RECIBIDO");
        System.out.println("═══════════════════════════════════════");
        System.out.println("🚍 Bus: " + event.getBusId());
        System.out.println("📋 Tipo: " + event.getType().getDescription());
        System.out.println("⚠️  Prioridad: " + event.getPriority().getDescription());
        System.out.println("📁 Categoría: " + event.getCategory().getDescription());
        System.out.println("📝 Descripción: " + event.getDescription());
        System.out.println("🕒 Fecha: " + event.getTimestamp());
        System.out.println("═══════════════════════════════════════");
        System.out.println();

        // 3. Enviar notificación a controladores (solo eventos de alta prioridad)
        notificationService.notifyEvent(event);
    }
}
