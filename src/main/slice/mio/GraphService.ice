module mio {

    // Estructura para representar un evento de bus
    struct BusEventData {
        string eventId;
        string busId;
        string eventType;
        string priority;
        string description;
        string timestamp;
        double latitude;
        double longitude;
    }

    // Interfaz de callback para que los clientes reciban notificaciones
    interface EventCallback {
        void onBusEvent(BusEventData event);
    }

    // Servicio principal del servidor
    interface GraphService {
        // Operaciones existentes
        string getGraphSummary();

        // Nuevas operaciones para eventos
        void reportBusEvent(BusEventData event);
        void subscribeToEvents(EventCallback* callback, string clientId);
        void unsubscribeFromEvents(string clientId);

        // Obtener eventos recientes
        sequence<BusEventData> getRecentEvents(int limit);
    }
}


