package com.speedfast.service;

import com.speedfast.model.EstadoPedido;
import com.speedfast.model.Pedido;
import com.speedfast.model.Repartidor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Zona de carga compartida por todos los repartidores. Los pedidos llegan aquí
 * y cada repartidor los retira de a uno para salir a repartir.
 *
 * <p>Es el recurso compartido del sistema: varios hilos intentan retirar
 * pedidos al mismo tiempo, por lo que {@link #agregarPedido(Pedido)} y
 * {@link #retirarPedido()} son {@code synchronized}. Gracias a eso, la
 * comprobación de la cola y la extracción del pedido ocurren de forma
 * indivisible y un mismo pedido nunca puede ser retirado por dos repartidores.</p>
 *
 * <p>Los contadores usan {@link AtomicInteger} para que el monitor pueda
 * consultarlos sin tomar el bloqueo, es decir, sin interferir con el trabajo de
 * los repartidores.</p>
 */
public class ZonaDeCarga {

    /** Pedidos en espera de ser retirados, en orden de llegada. */
    private final Queue<Pedido> pedidosPendientes = new LinkedList<>();

    /** Cantidad de pedidos retirados que aún están en camino. */
    private final AtomicInteger pedidosEnReparto = new AtomicInteger(0);

    /** Cantidad de pedidos ya entregados. */
    private final AtomicInteger pedidosEntregados = new AtomicInteger(0);

    /** Total de pedidos que ingresaron a la zona de carga. */
    private final AtomicInteger pedidosRecibidos = new AtomicInteger(0);

    /**
     * Agrega un pedido a la zona de carga.
     *
     * @param pedido pedido que llega a la zona de carga
     */
    public synchronized void agregarPedido(Pedido pedido) {
        if (pedido == null) {
            return;
        }
        pedidosPendientes.add(pedido);
        pedidosRecibidos.incrementAndGet();
        System.out.printf("Pedido #%d agregado. Destino: %s%n",
                pedido.getIdPedido(), pedido.getDireccionEntrega());
    }

    /**
     * Retira el siguiente pedido pendiente de la zona de carga.
     *
     * <p>Al ser {@code synchronized}, dos repartidores no pueden obtener el
     * mismo pedido: el segundo espera a que el primero termine de extraerlo.</p>
     *
     * @return el pedido retirado, o {@code null} si ya no quedan pendientes
     */
    public synchronized Pedido retirarPedido() {
        Pedido pedido = pedidosPendientes.poll();
        if (pedido != null) {
            pedidosEnReparto.incrementAndGet();
        }
        return pedido;
    }

    /**
     * Sobrecarga que retira el primer pedido que el repartidor esté en
     * condiciones de atender, según los requisitos propios de cada tipo de
     * pedido. Si ninguno le corresponde, no retira nada.
     *
     * @param repartidor repartidor que intenta retirar un pedido
     * @return el pedido retirado y ya asignado, o {@code null} si no hay uno compatible
     */
    public synchronized Pedido retirarPedido(Repartidor repartidor) {
        for (Pedido pedido : pedidosPendientes) {
            if (pedido.getEstado() == EstadoPedido.PENDIENTE
                    && pedido.cumpleRequisitos(repartidor)) {
                pedidosPendientes.remove(pedido);
                pedido.asignarRepartidor(repartidor);
                pedidosEnReparto.incrementAndGet();
                return pedido;
            }
        }
        return null;
    }

    /**
     * Registra que un pedido retirado ya llegó a destino.
     */
    public void confirmarEntrega() {
        pedidosEnReparto.decrementAndGet();
        pedidosEntregados.incrementAndGet();
    }

    /** @return cantidad de pedidos que aún esperan en la zona de carga */
    public synchronized int getCantidadPendientes() {
        return pedidosPendientes.size();
    }

    /** @return cantidad de pedidos retirados que siguen en camino */
    public int getCantidadEnReparto() {
        return pedidosEnReparto.get();
    }

    /** @return cantidad de pedidos ya entregados */
    public int getCantidadEntregados() {
        return pedidosEntregados.get();
    }

    /** @return total de pedidos que ingresaron a la zona de carga */
    public int getCantidadRecibidos() {
        return pedidosRecibidos.get();
    }

    /** @return {@code true} si todos los pedidos recibidos fueron entregados */
    public boolean todoEntregado() {
        return pedidosEntregados.get() == pedidosRecibidos.get();
    }

    /** @return los pedidos que siguen esperando en la zona de carga */
    public synchronized List<Pedido> getPedidosPendientes() {
        return new ArrayList<>(pedidosPendientes);
    }
}
