package co.edu.icesi.mio.infra.csv;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class GrafoMIO {
    private Map<Integer, Parada> paradas;
    private Map<Integer, Ruta> rutas;
    private List<Arco> arcos;
    private Map<Integer, List<ParadaEnRuta>> paradaPorRuta; // lineId -> lista de paradas ordenadas

    public GrafoMIO() {
        this.paradas = new HashMap<>();
        this.rutas = new HashMap<>();
        this.arcos = new ArrayList<>();
        this.paradaPorRuta = new HashMap<>();
    }


    private static class ParadaEnRuta implements Comparable<ParadaEnRuta> {
        int stopId;
        int sequence;
        int orientation;
        int lineId;

        public ParadaEnRuta(int stopId, int sequence, int orientation, int lineId) {
            this.stopId = stopId;
            this.sequence = sequence;
            this.orientation = orientation;
            this.lineId = lineId;
        }

        @Override
        public int compareTo(ParadaEnRuta other) {
            // Primero ordenar por orientación, luego por secuencia
            if (this.orientation != other.orientation) {
                return Integer.compare(this.orientation, other.orientation);
            }
            return Integer.compare(this.sequence, other.sequence);
        }
    }


    public void cargarParadas(String filePath) throws IOException {
        System.out.println("Cargando paradas desde: " + filePath);
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line;
        boolean isFirstLine = true;

        while ((line = br.readLine()) != null) {
            if (isFirstLine) {
                isFirstLine = false;
                continue; // Saltar encabezado
            }

            String[] fields = parseCsvLine(line);
            if (fields.length >= 8) {
                try {
                    int stopId = Integer.parseInt(fields[0].trim());
                    int planVersionId = Integer.parseInt(fields[1].trim());
                    String shortName = fields[2].trim();
                    String longName = fields[3].trim();
                    double decimalLongitude = Double.parseDouble(fields[6].trim());
                    double decimalLatitude = Double.parseDouble(fields[7].trim());

                    Parada parada = new Parada(stopId, planVersionId, shortName, longName,
                            decimalLongitude, decimalLatitude);
                    paradas.put(stopId, parada);
                } catch (NumberFormatException e) {
                    System.err.println("Error parseando línea de parada: " + line);
                }
            }
        }
        br.close();
        System.out.println("Total paradas cargadas: " + paradas.size());
    }


    public void cargarRutas(String filePath) throws IOException {
        System.out.println("Cargando rutas desde: " + filePath);
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line;
        boolean isFirstLine = true;

        while ((line = br.readLine()) != null) {
            if (isFirstLine) {
                isFirstLine = false;
                continue; // Saltar encabezado
            }

            String[] fields = parseCsvLine(line);
            if (fields.length >= 4) {
                try {
                    int lineId = Integer.parseInt(fields[0].trim());
                    int planVersionId = Integer.parseInt(fields[1].trim());
                    String shortName = fields[2].trim();
                    String description = fields[3].trim();

                    Ruta ruta = new Ruta(lineId, planVersionId, shortName, description);
                    rutas.put(lineId, ruta);
                } catch (NumberFormatException e) {
                    System.err.println("Error parseando línea de ruta: " + line);
                }
            }
        }
        br.close();
        System.out.println("Total rutas cargadas: " + rutas.size());
    }


    public void cargarLineStopsYConstruirArcos(String filePath) throws IOException {
        System.out.println("Cargando paradas por ruta y construyendo arcos desde: " + filePath);
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line;
        boolean isFirstLine = true;

        while ((line = br.readLine()) != null) {
            if (isFirstLine) {
                isFirstLine = false;
                continue; // Saltar encabezado
            }

            String[] fields = parseCsvLine(line);
            if (fields.length >= 5) {
                try {
                    int stopSequence = Integer.parseInt(fields[1].trim());
                    int orientation = Integer.parseInt(fields[2].trim());
                    int lineId = Integer.parseInt(fields[3].trim());
                    int stopId = Integer.parseInt(fields[4].trim());

                    // Verificar que la parada y la ruta existan
                    if (!paradas.containsKey(stopId)) {
                        continue; // Saltar si la parada no existe
                    }
                    if (!rutas.containsKey(lineId)) {
                        continue; // Saltar si la ruta no existe
                    }

                    // Agregar a la lista de paradas por ruta
                    if (!paradaPorRuta.containsKey(lineId)) {
                        paradaPorRuta.put(lineId, new ArrayList<>());
                    }
                    paradaPorRuta.get(lineId).add(
                            new ParadaEnRuta(stopId, stopSequence, orientation, lineId));

                } catch (NumberFormatException e) {
                    System.err.println("Error parseando línea de linestops: " + line);
                }
            }
        }
        br.close();

        // Construir arcos
        construirArcos();
        System.out.println("Total arcos construidos: " + arcos.size());
    }


    private void construirArcos() {
        for (Map.Entry<Integer, List<ParadaEnRuta>> entry : paradaPorRuta.entrySet()) {
            int lineId = entry.getKey();
            List<ParadaEnRuta> paradasEnRuta = entry.getValue();

            // Ordenar las paradas por orientación y secuencia
            Collections.sort(paradasEnRuta);

            Ruta ruta = rutas.get(lineId);
            if (ruta == null) continue;

            // Separar por orientación
            List<ParadaEnRuta> ida = new ArrayList<>();
            List<ParadaEnRuta> vuelta = new ArrayList<>();

            for (ParadaEnRuta per : paradasEnRuta) {
                if (per.orientation == 0) {
                    ida.add(per);
                } else {
                    vuelta.add(per);
                }
            }

            // Crear arcos para ida
            crearArcosParaOrientacion(ida, lineId, ruta.getShortName(), 0);

            // Crear arcos para vuelta
            crearArcosParaOrientacion(vuelta, lineId, ruta.getShortName(), 1);
        }
    }


    private void crearArcosParaOrientacion(List<ParadaEnRuta> paradasOrdenadas,
                                           int lineId, String lineShortName, int orientation) {
        for (int i = 0; i < paradasOrdenadas.size() - 1; i++) {
            ParadaEnRuta actual = paradasOrdenadas.get(i);
            ParadaEnRuta siguiente = paradasOrdenadas.get(i + 1);

            Parada paradaOrigen = paradas.get(actual.stopId);
            Parada paradaDestino = paradas.get(siguiente.stopId);

            if (paradaOrigen != null && paradaDestino != null) {
                Arco arco = new Arco(paradaOrigen, paradaDestino, lineId, lineShortName,
                        orientation, actual.sequence, siguiente.sequence);
                arcos.add(arco);
            }
        }
    }


    public void mostrarArcosOrdenados() {
        // Agrupar arcos por lineId y orientation
        Map<String, List<Arco>> arcosPorRutaOrientacion = new TreeMap<>();

        for (Arco arco : arcos) {
            String key = String.format("%04d_%d_%s", arco.getLineId(), arco.getOrientation(),
                    arco.getLineShortName());
            if (!arcosPorRutaOrientacion.containsKey(key)) {
                arcosPorRutaOrientacion.put(key, new ArrayList<>());
            }
            arcosPorRutaOrientacion.get(key).add(arco);
        }

        // Mostrar arcos agrupados
        System.out.println("\n" + "=".repeat(100));
        System.out.println("GRAFO DE ARCOS DEL SITM-MIO");
        System.out.println("=".repeat(100));

        int totalArcos = 0;
        for (Map.Entry<String, List<Arco>> entry : arcosPorRutaOrientacion.entrySet()) {
            List<Arco> arcosRuta = entry.getValue();
            if (arcosRuta.isEmpty()) continue;

            // Ordenar por secuencia
            arcosRuta.sort(Comparator.comparingInt(Arco::getSequenceOrigen));

            Arco primerArco = arcosRuta.get(0);
            Ruta ruta = rutas.get(primerArco.getLineId());

            System.out.println("\n" + "-".repeat(100));
            System.out.println(String.format("RUTA: %s - %s | ORIENTACIÓN: %s | TOTAL ARCOS: %d",
                    primerArco.getLineShortName(),
                    ruta != null ? ruta.getDescription() : "N/A",
                    primerArco.getOrientationName(),
                    arcosRuta.size()));
            System.out.println("-".repeat(100));

            for (int i = 0; i < arcosRuta.size(); i++) {
                System.out.println(String.format("  %3d. %s", i + 1, arcosRuta.get(i)));
            }

            totalArcos += arcosRuta.size();
        }

        System.out.println("\n" + "=".repeat(100));
        System.out.println(String.format("RESUMEN: Total de %d arcos en %d rutas",
                totalArcos, rutas.size()));
        System.out.println("=".repeat(100));
    }


    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());

        return result.toArray(new String[0]);
    }

    public Map<Integer, Parada> getParadas() {
        return paradas;
    }

    public Map<Integer, Ruta> getRutas() {
        return rutas;
    }

    public List<Arco> getArcos() {
        return arcos;
    }

    public List<Arco> getArcosPorRuta(String lineShortName) {
        List<Arco> resultado = new ArrayList<>();
        if (lineShortName == null) {
            return resultado;
        }
        for (Arco arco : arcos) {
            if (lineShortName.equalsIgnoreCase(arco.getLineShortName())) {
                resultado.add(arco);
            }
        }
        return resultado;
    }

    public List<Arco> getArcosPorRutaYOrientacion(String lineShortName, int orientation) {
        List<Arco> resultado = new ArrayList<>();
        if (lineShortName == null) {
            return resultado;
        }
        for (Arco arco : arcos) {
            if (lineShortName.equalsIgnoreCase(arco.getLineShortName())
                    && arco.getOrientation() == orientation) {
                resultado.add(arco);
            }
        }
        return resultado;
    }

}
