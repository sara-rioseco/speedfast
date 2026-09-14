package com.speedfast.service;

/**
 * Monitor que informa en tiempo real el estado de la zona de carga mientras los
 * repartidores trabajan.
 *
 * <p>Consulta únicamente los contadores atómicos de {@link ZonaDeCarga}, por lo
 * que audita el sistema <strong>sin tomar el bloqueo</strong> y sin interferir
 * con los retiros de pedidos.</p>
 *
 * <p>La bandera {@code activo} es {@code volatile} para que el hilo del monitor
 * vea de inmediato la orden de detenerse emitida desde otro hilo.</p>
 */
public class MonitorEstado implements Runnable {

    /** Milisegundos entre dos informes consecutivos. */
    private static final int INTERVALO_MS = 700;

    /** Zona de carga que se está auditando. */
    private final ZonaDeCarga zonaDeCarga;

    /** Indica si el monitor debe seguir informando. */
    private volatile boolean activo = true;

    /**
     * Crea el monitor asociado a una zona de carga.
     *
     * @param zonaDeCarga zona de carga a auditar
     */
    public MonitorEstado(ZonaDeCarga zonaDeCarga) {
        this.zonaDeCarga = zonaDeCarga;
    }

    /** Solicita al monitor que deje de informar. */
    public void detener() {
        this.activo = false;
    }

    /**
     * Informa periódicamente cuántos pedidos hay pendientes, en reparto y
     * entregados, hasta que se le solicite detenerse.
     */
    @Override
    public void run() {
        while (activo) {
            try {
                Thread.sleep(INTERVALO_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (activo) {
                informarEstado();
            }
        }
    }

    /** Imprime una línea con el estado actual del sistema. */
    public void informarEstado() {
        System.out.printf("   [Monitor] Pendientes: %d | En reparto: %d | Entregados: %d de %d%n",
                zonaDeCarga.getCantidadPendientes(),
                zonaDeCarga.getCantidadEnReparto(),
                zonaDeCarga.getCantidadEntregados(),
                zonaDeCarga.getCantidadRecibidos());
    }
}
