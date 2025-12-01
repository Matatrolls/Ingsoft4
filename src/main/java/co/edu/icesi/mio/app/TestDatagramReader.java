package co.edu.icesi.mio.app;

import co.edu.icesi.mio.infra.csv.DatagramReader;
import co.edu.icesi.mio.model.streaming.Datagram;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Programa de prueba para verificar la lectura de datagramas.
 * Realiza análisis básicos sobre los datos del archivo CSV.
 */
public class TestDatagramReader {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  TEST DE LECTURA DE DATAGRAMAS - SITM-MIO");
        System.out.println("═══════════════════════════════════════════════════════════\n");

        // Ruta al archivo de datos (puedes cambiarla según necesites)
        String dataPath = "src/main/resources/data/datagrams4streaming.csv";

        // Test 1: Leer una muestra de datos
        testReadSample(dataPath, 100);

        // Test 2: Análisis de validez de datos
        testDataValidity(dataPath, 10000);

        // Test 3: Estadísticas de buses y líneas
        testBusAndLineStats(dataPath, 50000);

        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("  PRUEBAS COMPLETADAS");
        System.out.println("═══════════════════════════════════════════════════════════");
    }

    /**
     * Test 1: Lee una muestra pequeña de datos
     */
    private static void testReadSample(String filePath, int sampleSize) {
        System.out.println("\n▶ TEST 1: Leyendo muestra de " + sampleSize + " registros");
        System.out.println("─────────────────────────────────────────────────────────\n");

        try {
            DatagramReader reader = new DatagramReader(filePath);
            List<Datagram> sample = reader.readSample(sampleSize);

            System.out.println("✓ Registros leídos: " + sample.size());

            if (!sample.isEmpty()) {
                System.out.println("\nPrimeros 5 registros:");
                for (int i = 0; i < Math.min(5, sample.size()); i++) {
                    Datagram d = sample.get(i);
                    System.out.printf("  %d. Bus: %d, Línea: %d, Lat: %.6f, Lon: %.6f, Vel: %d, Tiempo: %s\n",
                            i + 1, d.getBusCode(), d.getLineId(),
                            d.getLatitude(), d.getLongitude(), d.getVelocity(), d.getTimestamp());
                }
            }

        } catch (IOException e) {
            System.err.println("✗ Error leyendo archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 2: Analiza la validez de los datos
     */
    private static void testDataValidity(String filePath, int limit) {
        System.out.println("\n▶ TEST 2: Análisis de validez de datos (primeros " + limit + " registros)");
        System.out.println("─────────────────────────────────────────────────────────\n");

        try {
            DatagramReader reader = new DatagramReader(filePath);

            int[] counts = new int[4]; // valid, invalidBus, invalidLine, invalidCoords

            DatagramReader.ReadStats stats = reader.readWithFilter(datagram -> {
                if (datagram.isValid()) {
                    counts[0]++;
                } else {
                    if (!datagram.hasValidBus()) counts[1]++;
                    if (!datagram.hasValidLine()) counts[2]++;
                    if (!datagram.hasValidCoordinates()) counts[3]++;
                }
            }, datagram -> {
                // Solo procesar los primeros 'limit' registros válidos
                return counts[0] + counts[1] + counts[2] + counts[3] < limit;
            });

            System.out.println("Estadísticas de lectura:");
            System.out.println(stats);
            System.out.println();
            System.out.println("Análisis de validez:");
            System.out.printf("  ✓ Registros válidos:      %d (%.1f%%)\n",
                    counts[0], counts[0] * 100.0 / (counts[0] + counts[1]));
            System.out.printf("  ✗ Sin bus válido:         %d\n", counts[1]);
            System.out.printf("  ✗ Sin línea válida:       %d\n", counts[2]);
            System.out.printf("  ✗ Sin coordenadas válidas: %d\n", counts[3]);

        } catch (IOException e) {
            System.err.println("✗ Error en test de validez: " + e.getMessage());
        }
    }

    /**
     * Test 3: Estadísticas de buses y líneas
     */
    private static void testBusAndLineStats(String filePath, int limit) {
        System.out.println("\n▶ TEST 3: Estadísticas de buses y líneas (primeros " + limit + " registros)");
        System.out.println("─────────────────────────────────────────────────────────\n");

        try {
            DatagramReader reader = new DatagramReader(filePath);

            Map<Integer, Integer> busCount = new HashMap<>();
            Map<Integer, Integer> lineCount = new HashMap<>();
            final int[] processedCount = {0};

            DatagramReader.ReadStats stats = reader.readWithFilter(datagram -> {
                if (datagram.hasValidBus()) {
                    busCount.merge(datagram.getBusCode(), 1, Integer::sum);
                }
                if (datagram.hasValidLine()) {
                    lineCount.merge(datagram.getLineId(), 1, Integer::sum);
                }
                processedCount[0]++;
            }, datagram -> processedCount[0] < limit);

            System.out.println("Estadísticas de lectura:");
            System.out.println(stats);
            System.out.println();
            System.out.println("Análisis de datos:");
            System.out.printf("  🚍 Buses únicos identificados: %d\n", busCount.size());
            System.out.printf("  🚌 Líneas únicas identificadas: %d\n", lineCount.size());

            // Top 5 buses con más registros
            System.out.println("\nTop 5 buses con más registros:");
            busCount.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(5)
                    .forEach(entry -> System.out.printf("  Bus %d: %d registros\n",
                            entry.getKey(), entry.getValue()));

            // Top 5 líneas con más registros
            System.out.println("\nTop 5 líneas con más registros:");
            lineCount.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(5)
                    .forEach(entry -> System.out.printf("  Línea %d: %d registros\n",
                            entry.getKey(), entry.getValue()));

        } catch (IOException e) {
            System.err.println("✗ Error en test de estadísticas: " + e.getMessage());
        }
    }
}
