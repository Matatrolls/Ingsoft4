package co.edu.icesi.mio.concurrency.zone;

import co.edu.icesi.mio.concurrency.Worker;
import co.edu.icesi.mio.infra.csv.Arco;
import co.edu.icesi.mio.infra.csv.GrafoMIO;
import co.edu.icesi.mio.infra.csv.Parada;
import co.edu.icesi.mio.model.analytics.ArcIdentifier;
import co.edu.icesi.mio.model.analytics.ArcVelocityStats;
import co.edu.icesi.mio.model.analytics.Zone;
import co.edu.icesi.mio.repository.ArcVelocityRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Worker que analiza una zona geográfica específica.
 * Examina paradas, arcos, rutas y estadísticas de velocidad en la zona.
 */
public class ZoneAnalysisWorker implements Worker<Zone, ZoneAnalysisResult> {

    private final int workerId;
    private final GrafoMIO grafo;
    private final ArcVelocityRepository velocityRepository;
    private final Queue<Zone> workQueue;
    private ZoneAnalysisResult result;
    private volatile boolean done;

    public ZoneAnalysisWorker(int workerId, GrafoMIO grafo, ArcVelocityRepository velocityRepository) {
        this.workerId = workerId;
        this.grafo = grafo;
        this.velocityRepository = velocityRepository;
        this.workQueue = new LinkedList<>();
        this.done = false;
    }

    @Override
    public void assignWork(Zone work) {
        workQueue.offer(work);
    }

    @Override
    public void run() {
        // Procesar todas las zonas asignadas (normalmente solo una)
        while (!workQueue.isEmpty()) {
            Zone zone = workQueue.poll();
            result = analyzeZone(zone);
        }
        done = true;
    }

    /**
     * Analiza una zona completa
     */
    private ZoneAnalysisResult analyzeZone(Zone zone) {
        long startTime = System.currentTimeMillis();

        // 1. Encontrar paradas en la zona
        List<Parada> stopsInZone = findStopsInZone(zone);

        // 2. Encontrar arcos en la zona (arcos donde origen o destino está en la zona)
        List<Arco> arcsInZone = findArcsInZone(zone, stopsInZone);

        // 3. Contar rutas únicas
        Set<Integer> uniqueRoutes = new HashSet<>();
        for (Arco arco : arcsInZone) {
            uniqueRoutes.add(arco.getLineId());
        }

        // 4. Obtener estadísticas de velocidad para arcos en la zona
        List<ArcVelocityStats> arcStats = getVelocityStatsForArcs(arcsInZone);

        // 5. Calcular estadísticas agregadas
        double avgVelocity = arcStats.isEmpty() ? 0.0 :
                arcStats.stream()
                        .mapToDouble(ArcVelocityStats::getTypicalVelocity)
                        .average()
                        .orElse(0.0);

        double minVelocity = arcStats.isEmpty() ? 0.0 :
                arcStats.stream()
                        .mapToDouble(ArcVelocityStats::getTypicalVelocity)
                        .min()
                        .orElse(0.0);

        double maxVelocity = arcStats.isEmpty() ? 0.0 :
                arcStats.stream()
                        .mapToDouble(ArcVelocityStats::getTypicalVelocity)
                        .max()
                        .orElse(0.0);

        // 6. Obtener top arcos más lentos y más rápidos
        List<ArcVelocityStats> slowestArcs = arcStats.stream()
                .sorted(Comparator.comparingDouble(ArcVelocityStats::getTypicalVelocity))
                .limit(5)
                .collect(Collectors.toList());

        List<ArcVelocityStats> fastestArcs = arcStats.stream()
                .sorted(Comparator.comparingDouble(ArcVelocityStats::getTypicalVelocity).reversed())
                .limit(5)
                .collect(Collectors.toList());

        long endTime = System.currentTimeMillis();
        long analysisTime = endTime - startTime;

        return new ZoneAnalysisResult(
                workerId,
                zone,
                stopsInZone.size(),
                arcsInZone.size(),
                uniqueRoutes.size(),
                avgVelocity,
                minVelocity,
                maxVelocity,
                slowestArcs,
                fastestArcs,
                analysisTime
        );
    }

    /**
     * Encuentra todas las paradas dentro de una zona
     */
    private List<Parada> findStopsInZone(Zone zone) {
        List<Parada> stops = new ArrayList<>();

        for (Parada parada : grafo.getParadas().values()) {
            if (zone.contains(parada.getDecimalLatitude(), parada.getDecimalLongitude())) {
                stops.add(parada);
            }
        }

        return stops;
    }

    /**
     * Encuentra todos los arcos que pasan por una zona
     * (arcos donde el origen o destino está en la zona)
     */
    private List<Arco> findArcsInZone(Zone zone, List<Parada> stopsInZone) {
        List<Arco> arcs = new ArrayList<>();
        Set<Integer> stopIdsInZone = stopsInZone.stream()
                .map(Parada::getStopId)
                .collect(Collectors.toSet());

        for (Arco arco : grafo.getArcos()) {
            int originId = arco.getParadaOrigen().getStopId();
            int destId = arco.getParadaDestino().getStopId();

            // Si el origen o destino está en la zona, el arco pertenece a la zona
            if (stopIdsInZone.contains(originId) || stopIdsInZone.contains(destId)) {
                arcs.add(arco);
            }
        }

        return arcs;
    }

    /**
     * Obtiene estadísticas de velocidad para una lista de arcos
     */
    private List<ArcVelocityStats> getVelocityStatsForArcs(List<Arco> arcs) {
        List<ArcVelocityStats> stats = new ArrayList<>();

        for (Arco arco : arcs) {
            // Crear identificador de arco (usando routeId como lineId ya que Arco no tiene routeId)
            ArcIdentifier arcId = ArcIdentifier.forRoute(arco.getLineId(), arco.getLineId());

            Optional<ArcVelocityStats> arcStats = velocityRepository.findByArc(arcId);
            arcStats.ifPresent(stats::add);
        }

        return stats;
    }

    @Override
    public ZoneAnalysisResult getResult() {
        return result;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public int getWorkerId() {
        return workerId;
    }
}
