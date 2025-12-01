package co.edu.icesi.mio.model.realtime;

import java.time.LocalDateTime;

/**
 * Representa la posición actual de un bus en tiempo real.
 * Incluye ubicación GPS, velocidad, y datos de contexto de la ruta.
 */
public class BusPosition {

    private final int busId;
    private final int routeId;
    private final int lineId;
    private final double latitude;
    private final double longitude;
    private final double velocity;        // km/h
    private final int currentStopId;      // Última parada visitada
    private final int nextStopId;         // Próxima parada en la ruta
    private final LocalDateTime timestamp;

    public BusPosition(int busId, int routeId, int lineId, double latitude,
                       double longitude, double velocity, int currentStopId,
                       int nextStopId, LocalDateTime timestamp) {
        this.busId = busId;
        this.routeId = routeId;
        this.lineId = lineId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.velocity = velocity;
        this.currentStopId = currentStopId;
        this.nextStopId = nextStopId;
        this.timestamp = timestamp;
    }

    public int getBusId() {
        return busId;
    }

    public int getRouteId() {
        return routeId;
    }

    public int getLineId() {
        return lineId;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getVelocity() {
        return velocity;
    }

    public int getCurrentStopId() {
        return currentStopId;
    }

    public int getNextStopId() {
        return nextStopId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Indica si el bus está en movimiento
     */
    public boolean isMoving() {
        return velocity > 1.0; // Más de 1 km/h se considera en movimiento
    }

    /**
     * Indica si el bus está detenido
     */
    public boolean isStopped() {
        return velocity <= 1.0;
    }

    @Override
    public String toString() {
        return String.format("Bus[id=%d, route=%d, line=%d, pos=(%.4f,%.4f), vel=%.1f km/h, next_stop=%d, time=%s]",
                busId, routeId, lineId, latitude, longitude, velocity, nextStopId, timestamp);
    }

    /**
     * Builder para crear BusPosition fácilmente
     */
    public static class Builder {
        private int busId;
        private int routeId;
        private int lineId;
        private double latitude;
        private double longitude;
        private double velocity;
        private int currentStopId;
        private int nextStopId;
        private LocalDateTime timestamp = LocalDateTime.now();

        public Builder busId(int busId) {
            this.busId = busId;
            return this;
        }

        public Builder routeId(int routeId) {
            this.routeId = routeId;
            return this;
        }

        public Builder lineId(int lineId) {
            this.lineId = lineId;
            return this;
        }

        public Builder latitude(double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder velocity(double velocity) {
            this.velocity = velocity;
            return this;
        }

        public Builder currentStopId(int currentStopId) {
            this.currentStopId = currentStopId;
            return this;
        }

        public Builder nextStopId(int nextStopId) {
            this.nextStopId = nextStopId;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public BusPosition build() {
            return new BusPosition(busId, routeId, lineId, latitude, longitude,
                    velocity, currentStopId, nextStopId, timestamp);
        }
    }
}
