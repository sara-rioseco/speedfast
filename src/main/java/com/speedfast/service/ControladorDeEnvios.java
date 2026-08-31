package com.speedfast.service;

import com.speedfast.model.EstadoPedido;
import com.speedfast.model.Pedido;
import com.speedfast.model.Rastreable;
import com.speedfast.model.Repartidor;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador central de los envíos de SpeedFast. Concentra la lógica de
 * gestión del sistema: registra pedidos y repartidores, busca automáticamente
 * un repartidor adecuado para cada pedido y lleva el historial de entregas.
 *
 * <p>Implementa {@link Rastreable} para exponer el historial general del
 * sistema, mientras que cada {@link Pedido} mantiene el suyo propio.</p>
 */
public class ControladorDeEnvios implements Rastreable {

    /** Pedidos registrados en el sistema. */
    private final List<Pedido> pedidos = new ArrayList<>();

    /** Repartidores disponibles en la plataforma. */
    private final List<Repartidor> repartidores = new ArrayList<>();

    /** Entregas ya despachadas, en el orden en que se realizaron. */
    private final List<String> historialEntregas = new ArrayList<>();

    /**
     * Registra un pedido en el sistema.
     *
     * @param pedido pedido a registrar
     */
    public void registrarPedido(Pedido pedido) {
        pedidos.add(pedido);
    }

    /**
     * Registra un repartidor en la plataforma.
     *
     * @param repartidor repartidor a registrar
     */
    public void registrarRepartidor(Repartidor repartidor) {
        repartidores.add(repartidor);
    }

    /** @return una copia de la lista de pedidos registrados */
    public List<Pedido> getPedidos() {
        return new ArrayList<>(pedidos);
    }

    /** @return una copia de la lista de repartidores registrados */
    public List<Repartidor> getRepartidores() {
        return new ArrayList<>(repartidores);
    }

    /**
     * Asigna automáticamente el primer repartidor disponible que cumpla los
     * requisitos del pedido. El controlador no conoce las reglas de cada tipo
     * de servicio: se las consulta al propio pedido mediante
     * {@link Pedido#cumpleRequisitos(Repartidor)}.
     *
     * @param pedido pedido que necesita repartidor
     * @return el resultado de la asignación, listo para imprimirse en consola
     */
    public String asignarAutomaticamente(Pedido pedido) {
        for (Repartidor repartidor : repartidores) {
            if (repartidor.isDisponibleInmediato() && pedido.cumpleRequisitos(repartidor)) {
                String resultado = pedido.asignarRepartidor(repartidor);
                repartidor.setDisponibleInmediato(false);
                return resultado;
            }
        }
        return String.format("No hay repartidores disponibles que cumplan los requisitos del pedido %d.",
                pedido.getIdPedido());
    }

    /**
     * Despacha un pedido y, si la operación se concreta, la agrega al historial
     * de entregas del sistema.
     *
     * @param pedido pedido a despachar
     * @return el resultado de la operación, listo para imprimirse en consola
     */
    public String despachar(Pedido pedido) {
        boolean estabaDespachado = pedido.getEstado() == EstadoPedido.DESPACHADO;
        String resultado = pedido.despachar();

        if (!estabaDespachado && pedido.getEstado() == EstadoPedido.DESPACHADO) {
            historialEntregas.add(String.format("%s #%d — entregado por %s",
                    pedido.getTipoPedido(),
                    pedido.getIdPedido(),
                    pedido.getRepartidorAsignado().getNombreCompleto()));
        }
        return resultado;
    }

    /**
     * Cancela un pedido y libera al repartidor que lo tenía asignado, de modo
     * que vuelva a quedar disponible para otros envíos.
     *
     * @param pedido pedido a cancelar
     * @return el resultado de la operación, listo para imprimirse en consola
     */
    public String cancelar(Pedido pedido) {
        Repartidor asignado = pedido.getRepartidorAsignado();
        String resultado = pedido.cancelar();

        if (pedido.getEstado() == EstadoPedido.CANCELADO && asignado != null) {
            asignado.setDisponibleInmediato(true);
        }
        return resultado;
    }

    /**
     * Entrega el historial de entregas realizadas por el sistema.
     *
     * @return una copia de la lista de entregas despachadas
     */
    @Override
    public List<String> verHistorial() {
        return new ArrayList<>(historialEntregas);
    }
}
