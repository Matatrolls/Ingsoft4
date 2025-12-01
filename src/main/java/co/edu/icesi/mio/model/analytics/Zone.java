package co.edu.icesi.mio.model.analytics;

/**
 * Representa una zona geográfica de la ciudad con límites rectangulares (bounding box).
 * Usada para análisis de tráfico por sectores.
 */
public class Zone {

    private final String zoneId;
    private final String zoneName;
    private final double minLatitude;   // Límite sur
    private final double maxLatitude;   // Límite norte
    private final double minLongitude;  // Límite oeste
    private final double maxLongitude;  // Límite este

    public Zone(String zoneId, String zoneName, double minLatitude, double maxLatitude,
                double minLongitude, double maxLongitude) {
        this.zoneId = zoneId;
        this.zoneName = zoneName;
        this.minLatitude = minLatitude;
        this.maxLatitude = maxLatitude;
        this.minLongitude = minLongitude;
        this.maxLongitude = maxLongitude;
    }

    /**
     * Verifica si un punto GPS está dentro de esta zona
     */
    public boolean contains(double latitude, double longitude) {
        return latitude >= minLatitude && latitude <= maxLatitude &&
               longitude >= minLongitude && longitude <= maxLongitude;
    }

    /**
     * Calcula el área aproximada de la zona en km²
     */
    public double getApproximateAreaKm2() {
        // Aproximación simple usando diferencias de coordenadas
        double latDiff = maxLatitude - minLatitude;
        double lonDiff = maxLongitude - minLongitude;

        // 1 grado de latitud ≈ 111 km
        // 1 grado de longitud ≈ 111 * cos(latitud) km
        double avgLatitude = (minLatitude + maxLatitude) / 2;
        double latKm = latDiff * 111.0;
        double lonKm = lonDiff * 111.0 * Math.cos(Math.toRadians(avgLatitude));

        return latKm * lonKm;
    }

    /**
     * Obtiene el centro de la zona
     */
    public double[] getCenter() {
        double centerLat = (minLatitude + maxLatitude) / 2;
        double centerLon = (minLongitude + maxLongitude) / 2;
        return new double[]{centerLat, centerLon};
    }

    public String getZoneId() {
        return zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public double getMinLatitude() {
        return minLatitude;
    }

    public double getMaxLatitude() {
        return maxLatitude;
    }

    public double getMinLongitude() {
        return minLongitude;
    }

    public double getMaxLongitude() {
        return maxLongitude;
    }

    @Override
    public String toString() {
        return String.format("Zone[%s - %s, Lat: %.4f-%.4f, Lon: %.4f-%.4f, Area: %.2f km²]",
                zoneId, zoneName, minLatitude, maxLatitude, minLongitude, maxLongitude,
                getApproximateAreaKm2());
    }

    /**
     * Crea zonas predefinidas de Cali
     */
    public static Zone[] createCaliZones() {
        // Coordenadas aproximadas de Cali, Colombia
        // Cali está aproximadamente entre:
        // Latitud: 3.35° N - 3.52° N
        // Longitud: -76.58° W - -76.48° W

        return new Zone[]{
                // Zona Norte
                new Zone("NORTE", "Cali Norte",
                        3.47, 3.52,
                        -76.58, -76.48),

                // Zona Centro
                new Zone("CENTRO", "Cali Centro",
                        3.42, 3.47,
                        -76.56, -76.52),

                // Zona Sur
                new Zone("SUR", "Cali Sur",
                        3.35, 3.42,
                        -76.56, -76.50),

                // Zona Oeste
                new Zone("OESTE", "Cali Oeste",
                        3.40, 3.48,
                        -76.58, -76.54),

                // Zona Este
                new Zone("ESTE", "Cali Este",
                        3.40, 3.48,
                        -76.54, -76.48)
        };
    }

    /**
     * Crea una zona personalizada con límites específicos
     */
    public static Zone createCustomZone(String id, String name,
                                        double southLat, double northLat,
                                        double westLon, double eastLon) {
        return new Zone(id, name, southLat, northLat, westLon, eastLon);
    }
}
