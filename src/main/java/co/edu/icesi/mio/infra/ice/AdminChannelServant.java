package co.edu.icesi.mio.infra.ice;

import co.edu.icesi.mio.infra.csv.GrafoMIO;
import com.zeroc.Ice.Blobject;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.InputStream;
import com.zeroc.Ice.OutputStream;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Servidor Ice que maneja:
 * - Estadísticas del grafo
 * - Eventos de buses centralizados (comunicación entre clientes)
 */
public class AdminChannelServant implements Blobject {

    private final GrafoMIO grafo;

    // Almacenamiento centralizado de eventos (compartido entre todos los clientes)
    private final LinkedList<BusEventData> centralEventStore;
    private static final int MAX_EVENTS = 100; // Máximo de eventos almacenados

    public AdminChannelServant(GrafoMIO grafo) {
        this.grafo = grafo;
        this.centralEventStore = new LinkedList<>();
    }

    @Override
    public com.zeroc.Ice.Object.Ice_invokeResult ice_invoke(byte[] inParams, Current current) {

        String op = current.operation;
        OutputStream out = new OutputStream(current.adapter.getCommunicator());

        switch (op) {
            case "getGraphStats":
                return handleGetGraphStats(current, out);

            case "reportBusEvent":
                return handleReportBusEvent(inParams, current, out);

            case "getRecentEvents":
                return handleGetRecentEvents(inParams, current, out);

            default:
                out.startEncapsulation();
                out.endEncapsulation();
                return new com.zeroc.Ice.Object.Ice_invokeResult(false, out.finished());
        }
    }

    /**
     * Obtiene estadísticas del grafo
     */
    private com.zeroc.Ice.Object.Ice_invokeResult handleGetGraphStats(Current current, OutputStream out) {
        int stops = grafo.getParadas().size();
        int lines = grafo.getRutas().size();
        int arcs = grafo.getArcos().size();

        out.startEncapsulation();
        out.writeInt(stops);
        out.writeInt(lines);
        out.writeInt(arcs);
        out.endEncapsulation();

        byte[] outParams = out.finished();
        return new com.zeroc.Ice.Object.Ice_invokeResult(true, outParams);
    }

    /**
     * Reporta un evento de bus (desde cualquier cliente)
     * Almacena el evento centralizadamente para que otros clientes puedan verlo
     */
    private com.zeroc.Ice.Object.Ice_invokeResult handleReportBusEvent(byte[] inParams,
                                                                        Current current,
                                                                        OutputStream out) {
        InputStream in = new InputStream(current.adapter.getCommunicator(), inParams);
        in.startEncapsulation();

        // Leer datos del evento
        String eventId = in.readString();
        String busId = in.readString();
        String eventType = in.readString();
        String priority = in.readString();
        String description = in.readString();
        String timestamp = in.readString();
        double latitude = in.readDouble();
        double longitude = in.readDouble();

        in.endEncapsulation();

        // Crear y almacenar evento
        BusEventData event = new BusEventData(eventId, busId, eventType, priority,
                description, latitude, longitude);
        event.setTimestamp(timestamp);

        synchronized (centralEventStore) {
            centralEventStore.addFirst(event); // Agregar al inicio (más reciente)

            // Limitar tamaño
            while (centralEventStore.size() > MAX_EVENTS) {
                centralEventStore.removeLast();
            }
        }

        System.out.printf("[ICE][SERVER] 📡 Evento recibido: %s - %s - %s\n",
                busId, eventType, priority);

        // Respuesta vacía (operación void)
        out.startEncapsulation();
        out.endEncapsulation();

        return new com.zeroc.Ice.Object.Ice_invokeResult(true, out.finished());
    }

    /**
     * Obtiene los eventos recientes (para que los clientes hagan polling)
     */
    private com.zeroc.Ice.Object.Ice_invokeResult handleGetRecentEvents(byte[] inParams,
                                                                         Current current,
                                                                         OutputStream out) {
        InputStream in = new InputStream(current.adapter.getCommunicator(), inParams);
        in.startEncapsulation();
        int limit = in.readInt();
        in.endEncapsulation();

        List<BusEventData> events;
        synchronized (centralEventStore) {
            int count = Math.min(limit, centralEventStore.size());
            events = new ArrayList<>(centralEventStore.subList(0, count));
        }

        // Serializar eventos
        out.startEncapsulation();
        out.writeSize(events.size());

        for (BusEventData event : events) {
            out.writeString(event.getEventId());
            out.writeString(event.getBusId());
            out.writeString(event.getEventType());
            out.writeString(event.getPriority());
            out.writeString(event.getDescription());
            out.writeString(event.getTimestamp());
            out.writeDouble(event.getLatitude());
            out.writeDouble(event.getLongitude());
        }

        out.endEncapsulation();

        return new com.zeroc.Ice.Object.Ice_invokeResult(true, out.finished());
    }

    /**
     * Obtiene el número de eventos almacenados
     */
    public int getEventCount() {
        synchronized (centralEventStore) {
            return centralEventStore.size();
        }
    }
}
