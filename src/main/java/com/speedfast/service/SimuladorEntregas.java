package com.speedfast.service;

import com.speedfast.model.Repartidor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Ejecuta una ronda de entregas concurrentes: lanza a los repartidores como
 * hilos independientes mediante {@link ExecutorService}, junto con un
 * {@link MonitorEstado} que informa el avance, y espera a que todos terminen.
 *
 * <p>Concentra el ciclo de vida de los hilos que antes administraba
 * {@code Main}, de modo que la interfaz gráfica solo necesita pedir que la
 * ronda comience. {@link #ejecutarEntregas()} espera hasta el final de la
 * ronda, por lo que debe invocarse desde un hilo de fondo y nunca desde el
 * hilo gráfico, que quedaría congelado mientras los repartidores trabajan.</p>
 */
public class SimuladorEntregas {

    /** Segundos máximos de espera antes de dar por cerrada una ronda. */
    private static final int ESPERA_MAXIMA_SEGUNDOS = 60;

    /** Zona de carga desde la que los repartidores retiran pedidos. */
    private final ZonaDeCarga zonaDeCarga;

    /** Repartidores que participan en cada ronda. */
    private final List<Repartidor> repartidores;

    /**
     * Crea el simulador para una zona de carga y un grupo de repartidores.
     *
     * @param zonaDeCarga  zona de carga que auditará el monitor
     * @param repartidores repartidores que se ejecutarán como hilos
     */
    public SimuladorEntregas(ZonaDeCarga zonaDeCarga, List<Repartidor> repartidores) {
        this.zonaDeCarga = zonaDeCarga;
        this.repartidores = repartidores;
    }

    /**
     * Lanza al monitor y a los repartidores en paralelo, y espera a que todos
     * terminen de retirar y entregar los pedidos compatibles de la zona de carga.
     */
    public void ejecutarEntregas() {
        MonitorEstado monitor = new MonitorEstado(zonaDeCarga);
        Thread hiloMonitor = new Thread(monitor, "Monitor");
        hiloMonitor.setDaemon(true);
        hiloMonitor.start();

        try (ExecutorService executor = Executors.newFixedThreadPool(repartidores.size())) {
            for (Repartidor repartidor : repartidores) {
                executor.execute(repartidor);
            }
            executor.shutdown();

            try {
                if (!executor.awaitTermination(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS)) {
                    System.out.println("[Sistema] La simulación superó el tiempo máximo de espera.");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
                System.out.println("[Sistema] Simulación interrumpida.");
            } finally {
                monitor.detener();
            }
        }
        esperarCierre(hiloMonitor);

        monitor.informarEstado();
    }

    /**
     * Espera explícitamente a que un hilo termine. Se usa con el monitor tras
     * solicitar su detención: aunque es daemon y la bandera {@code volatile}
     * ya detiene su ciclo, esperar su cierre garantiza que ningún hilo creado
     * por la ronda siga activo cuando esta se da por terminada.
     *
     * @param hilo hilo cuyo término se espera
     */
    private static void esperarCierre(Thread hilo) {
        try {
            hilo.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[Sistema] Espera del hilo " + hilo.getName() + " interrumpida.");
        }
    }
}
