package co.edu.icesi.mio.concurrency.route;

import co.edu.icesi.mio.model.routing.CalculatedRoute;
import co.edu.icesi.mio.model.routing.RoutePair;

/**
 * Resultado del cálculo de una ruta por un worker.
 * Contiene el par origen-destino y la ruta calculada (si existe).
 */
public class RouteCalculationResult {

    private final int workerId;
    private final RoutePair routePair;
    private final CalculatedRoute route;
    private final boolean success;
    private final String errorMessage;
    private final long calculationTimeMs;

    /**
     * Constructor para resultado exitoso
     */
    public RouteCalculationResult(int workerId, RoutePair routePair,
                                  CalculatedRoute route, long calculationTimeMs) {
        this.workerId = workerId;
        this.routePair = routePair;
        this.route = route;
        this.success = true;
        this.errorMessage = null;
        this.calculationTimeMs = calculationTimeMs;
    }

    /**
     * Constructor para resultado con error
     */
    public RouteCalculationResult(int workerId, RoutePair routePair,
                                  String errorMessage, long calculationTimeMs) {
        this.workerId = workerId;
        this.routePair = routePair;
        this.route = null;
        this.success = false;
        this.errorMessage = errorMessage;
        this.calculationTimeMs = calculationTimeMs;
    }

    public int getWorkerId() {
        return workerId;
    }

    public RoutePair getRoutePair() {
        return routePair;
    }

    public CalculatedRoute getRoute() {
        return route;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getCalculationTimeMs() {
        return calculationTimeMs;
    }

    @Override
    public String toString() {
        if (success) {
            return String.format("Worker[%d]: %s -> Ruta calculada (%.1f min, %.0f m) en %d ms",
                    workerId, routePair.getPairId(),
                    route.getTotalTime(), route.getTotalDistance(), calculationTimeMs);
        } else {
            return String.format("Worker[%d]: %s -> Error: %s (en %d ms)",
                    workerId, routePair.getPairId(), errorMessage, calculationTimeMs);
        }
    }
}
