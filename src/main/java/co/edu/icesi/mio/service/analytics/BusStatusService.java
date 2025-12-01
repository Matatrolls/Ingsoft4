package co.edu.icesi.mio.service.analytics;

import co.edu.icesi.mio.model.events.BusEvent;
import co.edu.icesi.mio.model.events.BusEventStore;

import java.util.List;
import java.util.stream.Collectors;

public class BusStatusService {

    private final BusEventStore store;

    public BusStatusService(BusEventStore store) {
        this.store = store;
    }

    public List<BusEvent> getRecentEventsForBus(String busId, int limit) {
        return store.getLastEvents(limit).stream()
                .filter(e -> e.getBusId().equals(busId))
                .collect(Collectors.toList());
    }
}
