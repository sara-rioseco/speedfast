package com.speedfast.service;

/**
 * Observador de los cambios en los datos de SpeedFast (patrón Observer).
 *
 * <p>Lo implementan las ventanas de gestión: el {@link ControladorDeEnvios} les
 * avisa cada vez que se registra, modifica o elimina un pedido, un repartidor
 * o una entrega, y cada ventana vuelve a consultar la base de datos para
 * actualizar sus tablas y combos. Así, un cambio hecho en una ventana (o por
 * los repartidores durante una ronda de entregas) se ve en todas las demás.</p>
 *
 * <p>El aviso puede llegar desde el hilo de un repartidor, por lo que una
 * ventana debe trasladar la actualización al hilo gráfico de Swing.</p>
 */
@FunctionalInterface
public interface ObservadorDeCambios {

    /** Indica que los datos guardados cambiaron y deben volver a consultarse. */
    void datosActualizados();
}
