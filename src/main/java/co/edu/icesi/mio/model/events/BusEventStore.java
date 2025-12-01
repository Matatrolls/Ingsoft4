package co.edu.icesi.mio.model.events;

import java.util.*;
import java.util.stream.Collectors;

public class BusEventStore {

    private final List<BusEvent> events = new ArrayList<>();

    public synchronized void add(BusEvent event) {
        events.add(event);
        if (events.size() > 1000) {
            int toRemove = events.size() - 1000;
            events.subList(0, toRemove).clear();
        }
    }

    public synchronized List<BusEvent> getLastEvents(int limit) {
        int size = events.size();
        if (size == 0) {
            return Collections.emptyList();
        }
        int from = Math.max(0, size - limit);
        return new ArrayList<>(events.subList(from, size));
    }

    public synchronized List<BusEvent> getLastEmergencies(int limit) {
        List<BusEvent> emergencies = events.stream()
                .filter(BusEvent::isEmergency)
                .collect(Collectors.toList());
        int size = emergencies.size();
        if (size == 0) {
            return Collections.emptyList();
        }
        int from = Math.max(0, size - limit);
        return new ArrayList<>(emergencies.subList(from, size));
    }

    public synchronized Map<String, Long> getEventCountByType() {
        return events.stream()
                .collect(Collectors.groupingBy(
                        event -> event.getType().name(),
                        Collectors.counting()
                ));
    }

    public synchronized int getTotalEventCount() {
        return events.size();
    }
}
