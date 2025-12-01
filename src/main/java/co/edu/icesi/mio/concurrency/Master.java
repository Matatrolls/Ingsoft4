package co.edu.icesi.mio.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Clase Master genérica para el patrón Master-Worker.
 * Coordina workers, distribuye trabajo y agrega resultados.
 *
 * @param <T> Tipo del trabajo a distribuir
 * @param <R> Tipo del resultado parcial de cada worker
 * @param <A> Tipo del resultado agregado final
 */
public class Master<T, R, A> {

    private final int numWorkers;
    private final Function<Integer, Worker<T, R>> workerFactory;
    private final Function<List<R>, A> resultAggregator;
    private final ExecutorService executorService;

    public Master(int numWorkers,
                  Function<Integer, Worker<T, R>> workerFactory,
                  Function<List<R>, A> resultAggregator) {
        this.numWorkers = numWorkers;
        this.workerFactory = workerFactory;
        this.resultAggregator = resultAggregator;
        this.executorService = Executors.newFixedThreadPool(numWorkers);
    }

    /**
     * Procesa una lista de trabajos distribuyéndolos entre los workers
     *
     * @param workItems Lista de trabajos a procesar
     * @return Resultado agregado
     */
    public A process(List<T> workItems) throws InterruptedException {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  MASTER-WORKER: Iniciando procesamiento paralelo");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.printf("Workers: %d\n", numWorkers);
        System.out.printf("Trabajos: %d\n", workItems.size());
        System.out.println();

        long startTime = System.currentTimeMillis();

        // Crear workers
        List<Worker<T, R>> workers = new ArrayList<>();
        for (int i = 0; i < numWorkers; i++) {
            Worker<T, R> worker = workerFactory.apply(i);
            workers.add(worker);
        }

        // Distribuir trabajo de forma balanceada
        int workIndex = 0;
        for (T workItem : workItems) {
            Worker<T, R> worker = workers.get(workIndex % numWorkers);
            worker.assignWork(workItem);
            workIndex++;
        }

        System.out.println("✓ Trabajo distribuido a workers");
        System.out.println("✓ Iniciando procesamiento paralelo...\n");

        // Ejecutar workers
        CountDownLatch latch = new CountDownLatch(numWorkers);
        for (Worker<T, R> worker : workers) {
            executorService.submit(() -> {
                try {
                    worker.run();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Esperar a que todos terminen
        latch.await();

        // Recolectar resultados
        List<R> results = new ArrayList<>();
        for (Worker<T, R> worker : workers) {
            R result = worker.getResult();
            if (result != null) {
                results.add(result);
            }
        }

        // Agregar resultados
        A aggregatedResult = resultAggregator.apply(results);

        long endTime = System.currentTimeMillis();
        double durationSeconds = (endTime - startTime) / 1000.0;

        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("  MASTER-WORKER: Procesamiento completado");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.printf("Duración: %.2f segundos\n", durationSeconds);
        System.out.printf("Workers completados: %d/%d\n", results.size(), numWorkers);
        System.out.println();

        return aggregatedResult;
    }

    /**
     * Cierra el executor service
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
