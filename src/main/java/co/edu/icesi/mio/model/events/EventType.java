package co.edu.icesi.mio.model.events;

public enum EventType {
    PINCHADO("Pinchazo de llanta", EventCategory.MECANICO, EventPriority.ALTA),
    INCIDENTE("Incidente de tránsito", EventCategory.SEGURIDAD, EventPriority.CRITICA),
    RETRASO("Retraso del bus", EventCategory.OPERACIONAL, EventPriority.MEDIA),
    EMERGENCIA("Emergencia médica", EventCategory.EMERGENCIA, EventPriority.CRITICA),
    MARCHA("Manifestación/marcha", EventCategory.TRANSITO, EventPriority.ALTA),
    FALTAGASOLINA("Falta de gasolina", EventCategory.OPERACIONAL, EventPriority.ALTA),
    SOBRECALENTAMIENTO("Sobrecalentamiento del motor", EventCategory.MECANICO, EventPriority.ALTA),
    CHOQUE("Choque del bus", EventCategory.SEGURIDAD, EventPriority.CRITICA),
    ATRACO("Atraco en el bus", EventCategory.SEGURIDAD, EventPriority.CRITICA),
    AVERIA_MOTOR("Avería grave de motor", EventCategory.MECANICO, EventPriority.ALTA),
    TRANCON("Trancón en la vía", EventCategory.TRANSITO, EventPriority.MEDIA),
    GPS_UPDATE("Actualización de posición GPS", EventCategory.OPERACIONAL, EventPriority.BAJA),
    PUERTA_ABIERTA("Apertura de puerta", EventCategory.OPERACIONAL, EventPriority.BAJA),
    PUERTA_CERRADA("Cierre de puerta", EventCategory.OPERACIONAL, EventPriority.BAJA),

    // Eventos de streaming en tiempo real
    BUS_SPEEDING("Bus excede velocidad segura", EventCategory.OPERACIONAL, EventPriority.ALTA),
    BUS_STOPPED("Bus detenido", EventCategory.OPERACIONAL, EventPriority.BAJA),
    POSITION_UPDATE("Actualización de posición", EventCategory.OPERACIONAL, EventPriority.BAJA);

    private final String description;
    private final EventCategory category;
    private final EventPriority priority;

    EventType(String description, EventCategory category, EventPriority priority) {
        this.description = description;
        this.category = category;
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public EventCategory getCategory() {
        return category;
    }

    public EventPriority getPriority() {
        return priority;
    }
}

