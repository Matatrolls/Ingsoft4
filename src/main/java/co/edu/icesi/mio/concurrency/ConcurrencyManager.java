package co.edu.icesi.mio.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConcurrencyManager {

    private final ExecutorService executor;

    public ConcurrencyManager(int poolSize, int queueHint) {
        this.executor = Executors.newFixedThreadPool(poolSize);
    }

    public void run(Runnable task) {
        executor.submit(task);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
