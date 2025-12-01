package co.edu.icesi.mio.infra.ice;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.OperationMode;
import com.zeroc.Ice.OutputStream;
import com.zeroc.Ice.InputStream;
import com.zeroc.Ice.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Cliente Ice para comunicación con el servidor.
 * Ahora incluye funcionalidad para enviar y recibir eventos de buses.
 */
public class AdminChannelClient {

    public static final class GraphStats {
        public final int stops;
        public final int lines;
        public final int arcs;

        public GraphStats(int stops, int lines, int arcs) {
            this.stops = stops;
            this.lines = lines;
            this.arcs = arcs;
        }
    }

    private final Communicator communicator;
    private final ObjectPrx proxy;

    public AdminChannelClient(String host) {
        communicator = Util.initialize();
        String proxyStr = IceConfig.proxyString(host);
        proxy = communicator.stringToProxy(proxyStr);
    }

    /**
     * Obtiene estadísticas del grafo del MIO
     */
    public GraphStats getGraphStats() {
        OutputStream out = new OutputStream(communicator);
        out.startEncapsulation();
        out.endEncapsulation();
        byte[] inParams = out.finished();

        com.zeroc.Ice.Object.Ice_invokeResult result =
                proxy.ice_invoke("getGraphStats", OperationMode.Normal, inParams);

        if (!result.returnValue) {
            throw new RuntimeException("getGraphStats falló del lado del servidor");
        }

        InputStream in = new InputStream(communicator, result.outParams);
        in.startEncapsulation();
        int stops = in.readInt();
        int lines = in.readInt();
        int arcs = in.readInt();
        in.endEncapsulation();

        return new GraphStats(stops, lines, arcs);
    }

    /**
     * Reporta un evento de bus al servidor centralizado.
     * Esto permite que otros clientes vean el evento.
     *
     * @param event Datos del evento a reportar
     */
    public void reportBusEvent(BusEventData event) {
        OutputStream out = new OutputStream(communicator);
        out.startEncapsulation();

        // Serializar datos del evento
        out.writeString(event.getEventId());
        out.writeString(event.getBusId());
        out.writeString(event.getEventType());
        out.writeString(event.getPriority());
        out.writeString(event.getDescription());
        out.writeString(event.getTimestamp());
        out.writeDouble(event.getLatitude());
        out.writeDouble(event.getLongitude());

        out.endEncapsulation();
        byte[] inParams = out.finished();

        com.zeroc.Ice.Object.Ice_invokeResult result =
                proxy.ice_invoke("reportBusEvent", OperationMode.Normal, inParams);

        if (!result.returnValue) {
            throw new RuntimeException("reportBusEvent falló del lado del servidor");
        }
    }

    /**
     * Obtiene los eventos más recientes del servidor.
     * Los controladores pueden usar esto para ver eventos de todos los clientes.
     *
     * @param limit Número máximo de eventos a obtener
     * @return Lista de eventos recientes
     */
    public List<BusEventData> getRecentEvents(int limit) {
        OutputStream out = new OutputStream(communicator);
        out.startEncapsulation();
        out.writeInt(limit);
        out.endEncapsulation();
        byte[] inParams = out.finished();

        com.zeroc.Ice.Object.Ice_invokeResult result =
                proxy.ice_invoke("getRecentEvents", OperationMode.Normal, inParams);

        if (!result.returnValue) {
            throw new RuntimeException("getRecentEvents falló del lado del servidor");
        }

        InputStream in = new InputStream(communicator, result.outParams);
        in.startEncapsulation();
        int size = in.readSize();

        List<BusEventData> events = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String eventId = in.readString();
            String busId = in.readString();
            String eventType = in.readString();
            String priority = in.readString();
            String description = in.readString();
            String timestamp = in.readString();
            double latitude = in.readDouble();
            double longitude = in.readDouble();

            BusEventData event = new BusEventData(eventId, busId, eventType, priority,
                    description, latitude, longitude);
            event.setTimestamp(timestamp);
            events.add(event);
        }

        in.endEncapsulation();
        return events;
    }

    public void close() {
        communicator.destroy();
    }
}
