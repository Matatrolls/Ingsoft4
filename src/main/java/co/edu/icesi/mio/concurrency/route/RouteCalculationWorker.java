package co.edu.icesi.mio.concurrency.route;

import co.edu.icesi.mio.concurrency.Worker;
import co.edu.icesi.mio.model.routing.CalculatedRoute;
import co.edu.icesi.mio.model.routing.RoutePair;
import co.edu.icesi.mio.service.routing.RouteCalculatorService;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Worker que calcula rutas para múltiples pares origen-destino.
 * Usa RouteCalculatorService para calcular cada ruta de forma paralela.
 */
public class RouteCalculationWorker implements Worker<RoutePair, List<RouteCalculationResult>> {

    private final int workerId;
    private final RouteCalculatorService routeService;
    private final Queue<RoutePair> workQueue;
    private List<RouteCalculationResult> results;
    private volatile boolean done;

    public RouteCalculationWorker(int workerId, RouteCalculatorService routeService) {
        this.workerId = workerId;
        this.routeService = routeService;
        this.workQueue = new LinkedList<>();
        this.results = new ArrayList<>();
        this.done = false;
    }

    @Override
    public void assignWork(RoutePair work) {
        workQueue.offer(work);
    }

    @Override
    public void run() {
        results = new ArrayList<>();

        // Procesar todos los pares asignados
        while (!workQueue.isEmpty()) {
            RoutePair pair = workQueue.poll();
            RouteCalculationResult result = calculateRoute(pair);
            results.add(result);
        }

        done = true;
    }

    /**
     * Calcula la ruta más rápida para un par origen-destino
     */
    private RouteCalculationResult calculateRoute(RoutePair pair) {
        long startTime = System.currentTimeMillis();

        try {
            CalculatedRoute route = routeService.calculateFastestRoute(
                    pair.getOriginStopId(),
                    pair.getDestinationStopId()
            );

            long endTime = System.currentTimeMillis();
            long calculationTime = endTime - startTime;

            if (route != null && route.isFound()) {
                return new RouteCalculationResult(workerId, pair, route, calculationTime);
            } else {
                return new RouteCalculationResult(workerId, pair,
                        "No se encontró ruta", calculationTime);
            }

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long calculationTime = endTime - startTime;
            return new RouteCalculationResult(workerId, pair,
                    "Error: " + e.getMessage(), calculationTime);
        }
    }

    @Override
    public List<RouteCalculationResult> getResult() {
        return results;
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
