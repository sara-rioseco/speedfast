package com.speedfast.service;

import com.speedfast.exception.OperacionNoPermitidaException;
import com.speedfast.exception.PersistenciaException;
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
 * <p>Concentra el ciclo de vida de los hilos, de modo que la interfaz gráfica
 * solo necesita pedir que la ronda comience. {@link #ejecutarEntregas()}
 * espera hasta el final de la ronda, por lo que debe invocarse desde un hilo
 * de fondo y nunca desde el hilo gráfico, que quedaría congelado mientras los
 * repartidores trabajan.</p>
 *
 * <p>Al comenzar cada ronda, el controlador carga desde la base de datos a los
 * repartidores y los pedidos pendientes: quienes se registren desde la
 * interfaz participan en la siguiente.</p>
 */
public class SimuladorEntregas {

    /** Segundos máximos de espera antes de dar por cerrada una ronda. */
    private static final int ESPERA_MAXIMA_SEGUNDOS = 60;

    /** Zona de carga desde la que los repartidores retiran pedidos. */
    private final ZonaDeCarga zonaDeCarga;

    /** Controlador que prepara cada ronda y registra las entregas. */
    private final ControladorDeEnvios controlador;

    /**
     * Crea el simulador para una zona de carga y los repartidores de un controlador.
     *
     * @param zonaDeCarga zona de carga que auditará el monitor
     * @param controlador controlador que prepara cada ronda
     */
    public SimuladorEntregas(ZonaDeCarga zonaDeCarga, ControladorDeEnvios controlador) {
        this.zonaDeCarga = zonaDeCarga;
        this.controlador = controlador;
    }

    /**
     * Prepara la ronda, lanza al monitor y a los repartidores en paralelo, y
     * espera a que todos terminen de retirar y entregar los pedidos
     * compatibles de la zona de carga. Al terminar, con éxito o no, la ronda
     * se da por finalizada en el controlador.
     *
     * @return cantidad de pedidos entregados en la ronda
     * @throws PersistenciaException         si no fue posible cargar los datos de la ronda
     * @throws OperacionNoPermitidaException si no hay pedidos pendientes o repartidores registrados
     */
    public int ejecutarEntregas() throws PersistenciaException, OperacionNoPermitidaException {
        List<Repartidor> repartidores = controlador.prepararRonda();
        try {
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
            return zonaDeCarga.getCantidadEntregados();
        } finally {
            controlador.finalizarRonda();
        }
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
