package co.edu.icesi.mio.service.analytics;

import co.edu.icesi.mio.infra.csv.GrafoMIO;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MioGraphRepository {

    private final GrafoMIO grafo;

    public MioGraphRepository() throws IOException {
        this("data");
    }

    public MioGraphRepository(String directoryOnClasspath) throws IOException {
        this.grafo = new GrafoMIO();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL url = cl.getResource(directoryOnClasspath);
        if (url == null) {
            throw new IllegalArgumentException("Directorio de recursos no encontrado en classpath: " + directoryOnClasspath);
        }
        File dir;
        try {
            dir = new File(url.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("No se pudo resolver ruta de recursos: " + url, e);
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("No es un directorio: " + dir.getAbsolutePath());
        }

        File[] csvFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".csv"));
        if (csvFiles == null) {
            throw new IllegalStateException("No se pudieron listar archivos en: " + dir.getAbsolutePath());
        }

        List<File> stopFiles = Arrays.stream(csvFiles)
                .filter(f -> f.getName().toLowerCase().startsWith("stops-"))
                .sorted(Comparator.comparing(File::getName))
                .collect(Collectors.toList());

        List<File> lineFiles = Arrays.stream(csvFiles)
                .filter(f -> f.getName().toLowerCase().startsWith("lines-"))
                .sorted(Comparator.comparing(File::getName))
                .collect(Collectors.toList());

        List<File> lineStopsFiles = Arrays.stream(csvFiles)
                .filter(f -> f.getName().toLowerCase().startsWith("linestops-"))
                .sorted(Comparator.comparing(File::getName))
                .collect(Collectors.toList());

        for (File f : stopFiles) {
            grafo.cargarParadas(f.getAbsolutePath());
        }
        for (File f : lineFiles) {
            grafo.cargarRutas(f.getAbsolutePath());
        }
        for (File f : lineStopsFiles) {
            grafo.cargarLineStopsYConstruirArcos(f.getAbsolutePath());
        }
    }

    public GrafoMIO getGrafo() {
        return grafo;
    }
}
