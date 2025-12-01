package co.edu.icesi.mio.infra.csv;

import co.edu.icesi.mio.model.streaming.Datagram;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Lector eficiente de archivos CSV de datagramas.
 * Diseñado para manejar archivos muy grandes (>60GB) sin cargar todo en memoria.
 *
 * Características:
 * - Lectura línea por línea (streaming)
 * - Procesamiento en batches
 * - Filtrado durante la lectura
 * - Manejo de errores robusto
 * - Estadísticas de lectura
 */
public class DatagramReader {

    private final Path filePath;
    private boolean skipInvalidLines;
    private int batchSize;

    public DatagramReader(Path filePath) {
        this.filePath = filePath;
        this.skipInvalidLines = true;  // Por defecto, ignorar líneas inválidas
        this.batchSize = 1000;         // Tamaño de batch por defecto
    }

    public DatagramReader(String filePath) {
        this(Path.of(filePath));
    }

    // Configuración
    public DatagramReader skipInvalidLines(boolean skip) {
        this.skipInvalidLines = skip;
        return this;
    }

    public DatagramReader batchSize(int size) {
        this.batchSize = size;
        return this;
    }

    /**
     * Lee el archivo completo y procesa cada datagrama con el consumer proporcionado.
     * Método más eficiente para archivos grandes.
     *
     * @param consumer Función que procesa cada datagrama
     * @return Estadísticas de la lectura
     */
    public ReadStats readAll(Consumer<Datagram> consumer) throws IOException {
        return readWithFilter(consumer, datagram -> true);
    }

    /**
     * Lee el archivo aplicando un filtro y procesa solo los datagramas que cumplen la condición.
     *
     * @param consumer Función que procesa cada datagrama válido
     * @param filter Predicado para filtrar datagramas
     * @return Estadísticas de la lectura
     */
    public ReadStats readWithFilter(Consumer<Datagram> consumer, Predicate<Datagram> filter) throws IOException {
        ReadStats stats = new ReadStats();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            long lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                stats.totalLines++;

                // Saltar líneas vacías
                if (line.trim().isEmpty()) {
                    stats.skippedLines++;
                    continue;
                }

                try {
                    Datagram datagram = Datagram.fromCsvLine(line);

                    // Aplicar filtro
                    if (filter.test(datagram)) {
                        consumer.accept(datagram);
                        stats.processedRecords++;
                    } else {
                        stats.filteredRecords++;
                    }

                } catch (Exception e) {
                    stats.errorRecords++;
                    if (!skipInvalidLines) {
                        throw new IOException("Error parsing line " + lineNumber + ": " + line, e);
                    }
                    // Si skipInvalidLines es true, solo registramos el error y continuamos
                }

                // Progreso cada millón de líneas
                if (lineNumber % 1_000_000 == 0) {
                    System.out.println("Procesadas " + lineNumber + " líneas...");
                }
            }
        }

        return stats;
    }

    /**
     * Lee el archivo en batches y procesa cada batch con el consumer proporcionado.
     * Útil cuando se necesita procesar grupos de datagramas a la vez.
     *
     * @param batchConsumer Función que procesa cada batch de datagramas
     * @return Estadísticas de la lectura
     */
    public ReadStats readInBatches(Consumer<List<Datagram>> batchConsumer) throws IOException {
        return readInBatchesWithFilter(batchConsumer, datagram -> true);
    }

    /**
     * Lee el archivo en batches aplicando un filtro.
     *
     * @param batchConsumer Función que procesa cada batch
     * @param filter Predicado para filtrar datagramas
     * @return Estadísticas de la lectura
     */
    public ReadStats readInBatchesWithFilter(Consumer<List<Datagram>> batchConsumer,
                                             Predicate<Datagram> filter) throws IOException {
        ReadStats stats = new ReadStats();
        List<Datagram> batch = new ArrayList<>(batchSize);

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            long lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                stats.totalLines++;

                if (line.trim().isEmpty()) {
                    stats.skippedLines++;
                    continue;
                }

                try {
                    Datagram datagram = Datagram.fromCsvLine(line);

                    if (filter.test(datagram)) {
                        batch.add(datagram);
                        stats.processedRecords++;

                        // Procesar batch cuando alcanza el tamaño configurado
                        if (batch.size() >= batchSize) {
                            batchConsumer.accept(new ArrayList<>(batch));
                            batch.clear();
                        }
                    } else {
                        stats.filteredRecords++;
                    }

                } catch (Exception e) {
                    stats.errorRecords++;
                    if (!skipInvalidLines) {
                        throw new IOException("Error parsing line " + lineNumber + ": " + line, e);
                    }
                }

                if (lineNumber % 1_000_000 == 0) {
                    System.out.println("Procesadas " + lineNumber + " líneas...");
                }
            }

            // Procesar batch final (si quedaron registros)
            if (!batch.isEmpty()) {
                batchConsumer.accept(batch);
            }
        }

        return stats;
    }

    /**
     * Lee solo las primeras N líneas del archivo.
     * Útil para pruebas y muestreo.
     *
     * @param limit Número máximo de registros a leer
     * @return Lista con los datagramas leídos
     */
    public List<Datagram> readSample(int limit) throws IOException {
        List<Datagram> sample = new ArrayList<>(limit);
        AtomicLong count = new AtomicLong(0);

        readWithFilter(datagram -> {
            sample.add(datagram);
            count.incrementAndGet();
        }, datagram -> count.get() < limit);

        return sample;
    }

    /**
     * Lee datagramas para un bus específico.
     *
     * @param busCode Código del bus a filtrar
     * @param consumer Función que procesa cada datagrama del bus
     * @return Estadísticas de la lectura
     */
    public ReadStats readByBus(int busCode, Consumer<Datagram> consumer) throws IOException {
        return readWithFilter(consumer, datagram -> datagram.getBusCode() == busCode);
    }

    /**
     * Lee datagramas para una línea específica.
     *
     * @param lineId ID de la línea a filtrar
     * @param consumer Función que procesa cada datagrama de la línea
     * @return Estadísticas de la lectura
     */
    public ReadStats readByLine(int lineId, Consumer<Datagram> consumer) throws IOException {
        return readWithFilter(consumer, datagram -> datagram.getLineId() == lineId);
    }

    /**
     * Clase que almacena estadísticas de la operación de lectura.
     */
    public static class ReadStats {
        private long totalLines = 0;
        private long processedRecords = 0;
        private long filteredRecords = 0;
        private long errorRecords = 0;
        private long skippedLines = 0;

        public long getTotalLines() {
            return totalLines;
        }

        public long getProcessedRecords() {
            return processedRecords;
        }

        public long getFilteredRecords() {
            return filteredRecords;
        }

        public long getErrorRecords() {
            return errorRecords;
        }

        public long getSkippedLines() {
            return skippedLines;
        }

        public double getErrorRate() {
            return totalLines > 0 ? (errorRecords * 100.0 / totalLines) : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                    "ReadStats[total=%d, processed=%d, filtered=%d, errors=%d (%.2f%%), skipped=%d]",
                    totalLines, processedRecords, filteredRecords, errorRecords, getErrorRate(), skippedLines
            );
        }
    }
}
