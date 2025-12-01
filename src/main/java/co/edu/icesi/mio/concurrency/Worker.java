package co.edu.icesi.mio.concurrency;

/**
 * Interfaz genérica para Workers en el patrón Master-Worker.
 * Un Worker procesa una unidad de trabajo y retorna un resultado.
 *
 * @param <T> Tipo del trabajo a procesar
 * @param <R> Tipo del resultado
 */
public interface Worker<T, R> extends Runnable {

    /**
     * Asigna trabajo al worker
     */
    void assignWork(T work);

    /**
     * Obtiene el resultado del procesamiento
     */
    R getResult();

    /**
     * Indica si el worker ha terminado su trabajo
     */
    boolean isDone();

    /**
     * Obtiene el ID del worker
     */
    int getWorkerId();
}
