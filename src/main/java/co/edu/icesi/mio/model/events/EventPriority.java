package co.edu.icesi.mio.model.events;

/**
 * Prioridad de eventos en el sistema SITM-MIO.
 * Define la urgencia con la que debe procesarse un evento.
 */
public enum EventPriority {
    CRITICA(1, "Crítica"),      // Requiere atención inmediata (emergencias, choques)
    ALTA(2, "Alta"),            // Requiere atención pronta (averías graves, trancones)
    MEDIA(3, "Media"),          // Requiere atención normal (retrasos, marchas)
    BAJA(4, "Baja");            // Informativo (actualizaciones de posición)

    private final int level;
    private final String description;

    EventPriority(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Determina si esta prioridad es mayor que otra
     */
    public boolean isHigherThan(EventPriority other) {
        return this.level < other.level;
    }
}
