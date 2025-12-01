package co.edu.icesi.mio.model.events;

import co.edu.icesi.mio.concurrency.ConcurrencyManager;

public class BusEventSource {

    private final ConcurrencyManager concurrencyManager;
    private final BusEventStore store;

    public BusEventSource(ConcurrencyManager concurrencyManager, BusEventStore store) {
        this.concurrencyManager = concurrencyManager;
        this.store = store;
    }

    public void publishEvent(BusEvent event) {
        concurrencyManager.run(() -> {
            System.out.println("Procesando evento de bus en background:");
            System.out.println(event);
            store.add(event);
        });
    }
}
