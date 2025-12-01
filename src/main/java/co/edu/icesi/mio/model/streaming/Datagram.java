package co.edu.icesi.mio.model.streaming;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Representa un datagrama de telemetría de un bus del sistema MIO.
 * Contiene información de posición GPS, velocidad, ruta y timestamp.
 *
 * Formato del CSV:
 * type,date,busCode,lineId,latitude,longitude,velocity,routeId,state,eventId,timestamp,sequence
 *
 * Nota: Las coordenadas están en microgrados (multiplicadas por 1,000,000)
 */
public class Datagram {

    private final int type;
    private final LocalDate date;
    private final int busCode;
    private final int lineId;
    private final int latitudeMicro;  // Latitud en microgrados
    private final int longitudeMicro; // Longitud en microgrados
    private final int velocity;
    private final int routeId;
    private final int state;
    private final long eventId;
    private final LocalDateTime timestamp;
    private final int sequence;

    // Constante para conversión de microgrados a grados decimales
    private static final double MICRO_TO_DEGREES = 1_000_000.0;

    // Formatter para parsear el timestamp (formato: "2019-05-27 20:14:43")
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Parsea una fecha en formato "dd-MMM-yy" (ej: "28-MAY-19")
     * Maneja el formato manualmente para mayor robustez
     */
    private static LocalDate parseDate(String dateStr) {
        String[] parts = dateStr.split("-");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid date format: " + dateStr);
        }

        int day = Integer.parseInt(parts[0]);
        String monthStr = parts[1].toUpperCase();
        int year = Integer.parseInt("20" + parts[2]); // Asume siglo 21

        // Mapeo manual de meses en inglés
        int month = switch (monthStr) {
            case "JAN" -> 1;
            case "FEB" -> 2;
            case "MAR" -> 3;
            case "APR" -> 4;
            case "MAY" -> 5;
            case "JUN" -> 6;
            case "JUL" -> 7;
            case "AUG" -> 8;
            case "SEP" -> 9;
            case "OCT" -> 10;
            case "NOV" -> 11;
            case "DEC" -> 12;
            default -> throw new IllegalArgumentException("Invalid month: " + monthStr);
        };

        return LocalDate.of(year, month, day);
    }

    public Datagram(int type, LocalDate date, int busCode, int lineId,
                    int latitudeMicro, int longitudeMicro, int velocity,
                    int routeId, int state, long eventId,
                    LocalDateTime timestamp, int sequence) {
        this.type = type;
        this.date = date;
        this.busCode = busCode;
        this.lineId = lineId;
        this.latitudeMicro = latitudeMicro;
        this.longitudeMicro = longitudeMicro;
        this.velocity = velocity;
        this.routeId = routeId;
        this.state = state;
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.sequence = sequence;
    }

    // Getters básicos
    public int getType() {
        return type;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getBusCode() {
        return busCode;
    }

    public int getLineId() {
        return lineId;
    }

    public int getLatitudeMicro() {
        return latitudeMicro;
    }

    public int getLongitudeMicro() {
        return longitudeMicro;
    }

    public int getVelocity() {
        return velocity;
    }

    public int getRouteId() {
        return routeId;
    }

    public int getState() {
        return state;
    }

    public long getEventId() {
        return eventId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getSequence() {
        return sequence;
    }

    // Métodos de conversión de coordenadas
    public double getLatitude() {
        return latitudeMicro / MICRO_TO_DEGREES;
    }

    public double getLongitude() {
        return longitudeMicro / MICRO_TO_DEGREES;
    }

    // Métodos de validación
    public boolean hasValidBus() {
        return busCode > 0;
    }

    public boolean hasValidLine() {
        return lineId > 0;
    }

    public boolean hasValidCoordinates() {
        return latitudeMicro > 0 && longitudeMicro < 0;
    }

    public boolean isValid() {
        return hasValidBus() && hasValidLine() && hasValidCoordinates();
    }

    // Método toString para debugging
    @Override
    public String toString() {
        return String.format("Datagram[bus=%d, line=%d, route=%d, lat=%.6f, lon=%.6f, vel=%d, time=%s]",
                busCode, lineId, routeId, getLatitude(), getLongitude(), velocity, timestamp);
    }

    // Factory method para parsear desde línea CSV
    public static Datagram fromCsvLine(String line) {
        String[] fields = line.split(",");

        if (fields.length < 12) {
            throw new IllegalArgumentException("Invalid datagram format: expected 12 fields, got " + fields.length);
        }

        try {
            int type = Integer.parseInt(fields[0].trim());
            LocalDate date = parseDate(fields[1].trim());
            int busCode = Integer.parseInt(fields[2].trim());
            int lineId = Integer.parseInt(fields[3].trim());
            int latitudeMicro = Integer.parseInt(fields[4].trim());
            int longitudeMicro = Integer.parseInt(fields[5].trim());
            int velocity = Integer.parseInt(fields[6].trim());
            int routeId = Integer.parseInt(fields[7].trim());
            int state = Integer.parseInt(fields[8].trim());
            long eventId = Long.parseLong(fields[9].trim());
            LocalDateTime timestamp = LocalDateTime.parse(fields[10].trim(), TIMESTAMP_FORMATTER);
            int sequence = Integer.parseInt(fields[11].trim());

            return new Datagram(type, date, busCode, lineId, latitudeMicro, longitudeMicro,
                    velocity, routeId, state, eventId, timestamp, sequence);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing datagram: " + line, e);
        }
    }

    // Builder pattern para facilitar la creación
    public static class Builder {
        private int type;
        private LocalDate date;
        private int busCode;
        private int lineId;
        private int latitudeMicro;
        private int longitudeMicro;
        private int velocity;
        private int routeId;
        private int state;
        private long eventId;
        private LocalDateTime timestamp;
        private int sequence;

        public Builder type(int type) {
            this.type = type;
            return this;
        }

        public Builder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public Builder busCode(int busCode) {
            this.busCode = busCode;
            return this;
        }

        public Builder lineId(int lineId) {
            this.lineId = lineId;
            return this;
        }

        public Builder latitudeMicro(int latitudeMicro) {
            this.latitudeMicro = latitudeMicro;
            return this;
        }

        public Builder longitudeMicro(int longitudeMicro) {
            this.longitudeMicro = longitudeMicro;
            return this;
        }

        public Builder velocity(int velocity) {
            this.velocity = velocity;
            return this;
        }

        public Builder routeId(int routeId) {
            this.routeId = routeId;
            return this;
        }

        public Builder state(int state) {
            this.state = state;
            return this;
        }

        public Builder eventId(long eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder sequence(int sequence) {
            this.sequence = sequence;
            return this;
        }

        public Datagram build() {
            return new Datagram(type, date, busCode, lineId, latitudeMicro, longitudeMicro,
                    velocity, routeId, state, eventId, timestamp, sequence);
        }
    }
}
