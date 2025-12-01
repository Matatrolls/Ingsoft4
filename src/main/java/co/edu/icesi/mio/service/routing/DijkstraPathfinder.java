package co.edu.icesi.mio.service.routing;

import co.edu.icesi.mio.infra.csv.Arco;
import co.edu.icesi.mio.infra.csv.GrafoMIO;
import co.edu.icesi.mio.infra.csv.Parada;
import co.edu.icesi.mio.model.routing.CalculatedRoute;
import co.edu.icesi.mio.model.routing.RouteSegment;

import java.util.*;

/**
 * Implementación del algoritmo de Dijkstra para encontrar rutas óptimas
 * en el grafo del MIO.
 */
public class DijkstraPathfinder {

    private final GrafoMIO grafo;
    private final CostStrategy costStrategy;

    // Mapeo de paradas a sus arcos salientes (para performance)
    private final Map<Integer, List<Arco>> arcsByOrigin;

    public DijkstraPathfinder(GrafoMIO grafo, CostStrategy costStrategy) {
        this.grafo = grafo;
        this.costStrategy = costStrategy;
        this.arcsByOrigin = buildArcIndex();
    }

    /**
     * Construye un índice de arcos por parada origen para acceso rápido
     */
    private Map<Integer, List<Arco>> buildArcIndex() {
        Map<Integer, List<Arco>> index = new HashMap<>();

        for (Arco arco : grafo.getArcos()) {
            int originId = arco.getParadaOrigen().getStopId();
            index.computeIfAbsent(originId, k -> new ArrayList<>()).add(arco);
        }

        return index;
    }

    /**
     * Calcula la ruta óptima entre dos paradas usando Dijkstra
     *
     * @param originId ID de la parada origen
     * @param destinationId ID de la parada destino
     * @return Ruta calculada (puede estar vacía si no se encontró ruta)
     */
    public CalculatedRoute findRoute(int originId, int destinationId) {
        Parada origin = grafo.getParadas().get(originId);
        Parada destination = grafo.getParadas().get(destinationId);

        if (origin == null || destination == null) {
            return new CalculatedRoute(origin, destination, Collections.emptyList());
        }

        // Dijkstra
        Map<Integer, Double> distances = new HashMap<>();
        Map<Integer, Arco> previousArc = new HashMap<>();
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>();
        Set<Integer> visited = new HashSet<>();

        // Inicializar
        distances.put(originId, 0.0);
        queue.offer(new NodeDistance(originId, 0.0));

        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();
            int currentStopId = current.stopId;

            // Si ya visitamos este nodo, saltarlo
            if (visited.contains(currentStopId)) {
                continue;
            }

            visited.add(currentStopId);

            // Si llegamos al destino, terminamos
            if (currentStopId == destinationId) {
                break;
            }

            // Explorar arcos vecinos
            List<Arco> outgoingArcs = arcsByOrigin.getOrDefault(currentStopId, Collections.emptyList());

            for (Arco arco : outgoingArcs) {
                int neighborId = arco.getParadaDestino().getStopId();

                if (visited.contains(neighborId)) {
                    continue;
                }

                double arcCost = costStrategy.calculateCost(arco);
                double newDistance = distances.get(currentStopId) + arcCost;

                if (!distances.containsKey(neighborId) || newDistance < distances.get(neighborId)) {
                    distances.put(neighborId, newDistance);
                    previousArc.put(neighborId, arco);
                    queue.offer(new NodeDistance(neighborId, newDistance));
                }
            }
        }

        // Reconstruir el camino
        List<RouteSegment> segments = reconstructPath(originId, destinationId, previousArc);

        return new CalculatedRoute(origin, destination, segments);
    }

    /**
     * Reconstruye el camino desde el origen al destino
     */
    private List<RouteSegment> reconstructPath(int originId, int destinationId,
                                                Map<Integer, Arco> previousArc) {
        List<RouteSegment> segments = new ArrayList<>();

        if (!previousArc.containsKey(destinationId)) {
            // No se encontró ruta
            return segments;
        }

        // Reconstruir desde el destino hacia el origen
        int current = destinationId;
        List<Arco> pathArcs = new ArrayList<>();

        while (current != originId) {
            Arco arco = previousArc.get(current);
            if (arco == null) break;

            pathArcs.add(arco);
            current = arco.getParadaOrigen().getStopId();
        }

        // Invertir para tener el orden correcto (origen → destino)
        Collections.reverse(pathArcs);

        // Convertir arcos a segmentos de ruta
        for (Arco arco : pathArcs) {
            RouteSegment segment = costStrategy.createSegment(arco);
            segments.add(segment);
        }

        return segments;
    }

    /**
     * Clase interna para representar un nodo con su distancia en la priority queue
     */
    private static class NodeDistance implements Comparable<NodeDistance> {
        final int stopId;
        final double distance;

        NodeDistance(int stopId, double distance) {
            this.stopId = stopId;
            this.distance = distance;
        }

        @Override
        public int compareTo(NodeDistance other) {
            return Double.compare(this.distance, other.distance);
        }
    }

    /**
     * Interfaz para estrategias de cálculo de costo
     */
    public interface CostStrategy {
        /**
         * Calcula el costo de atravesar un arco
         */
        double calculateCost(Arco arco);

        /**
         * Crea un segmento de ruta a partir de un arco
         */
        RouteSegment createSegment(Arco arco);
    }
}
