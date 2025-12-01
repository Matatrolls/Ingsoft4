package co.edu.icesi.mio.model.events;

/**
 * Categoría de eventos en el sistema SITM-MIO.
 * Clasifica los eventos según su naturaleza.
 */
public enum EventCategory {
    OPERACIONAL("Operacional"),           // Eventos de operación normal
    MECANICO("Mecánico"),                 // Averías y problemas mecánicos
    TRANSITO("Tránsito"),                 // Condiciones de tráfico
    SEGURIDAD("Seguridad"),               // Eventos de seguridad
    EMERGENCIA("Emergencia"),             // Emergencias médicas o de seguridad
    MANTENIMIENTO("Mantenimiento");       // Eventos de mantenimiento

    private final String description;

    EventCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
