package co.edu.icesi.mio.infra.ice;

/**
 * Interfaz de callback para recibir eventos de buses desde el servidor Ice.
 * Los clientes implementan esta interfaz para ser notificados de eventos.
 */
public interface IceEventCallback {

    /**
     * Llamado cuando llega un nuevo evento de bus desde el servidor.
     *
     * @param event Datos del evento de bus
     */
    void onBusEvent(BusEventData event);
}
