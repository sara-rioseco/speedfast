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
 *
 * <p>Sus métodos son {@code synchronized} porque varios repartidores lo
 * utilizan al mismo tiempo desde hilos distintos: sin esa protección, dos
 * entregas simultáneas podrían perderse al escribir en el historial. Ninguno
 * de estos métodos realiza pausas, de modo que los hilos nunca quedan
 * bloqueados esperando a otro.</p>
 *
 * <p>Es también el controlador que comparten todas las ventanas de la
 * interfaz gráfica: como trabajan sobre la misma instancia, un pedido
 * registrado en el formulario aparece en el listado de pedidos.</p>
 */
public class ControladorDeEnvios implements Rastreable {

    /** Pedidos registrados en el sistema. */
    private final List<Pedido> pedidos = new ArrayList<>();

    /** Repartidores disponibles en la plataforma. */
    private final List<Repartidor> repartidores = new ArrayList<>();

    /** Entregas ya despachadas, en el orden en que se realizaron. */
    private final List<String> historialEntregas = new ArrayList<>();

    /** Zona de carga donde esperan los pedidos registrados hasta que un repartidor los retira. */
    private final ZonaDeCarga zonaDeCarga;

    /**
     * Crea el controlador asociado a la zona de carga donde quedarán los
     * pedidos que se registren.
     *
     * @param zonaDeCarga zona de carga compartida con los repartidores
     */
    public ControladorDeEnvios(ZonaDeCarga zonaDeCarga) {
        this.zonaDeCarga = zonaDeCarga;
    }

    /**
     * Registra un pedido en el sistema y lo deja en la zona de carga para que
     * un repartidor lo retire.
     *
     * @param pedido pedido a registrar
     * @throws IllegalArgumentException si ya existe un pedido con el mismo identificador
     */
    public synchronized void registrarPedido(Pedido pedido) {
        if (buscarPedidoPorId(pedido.getIdPedido()) != null) {
            throw new IllegalArgumentException(
                    "Ya existe un pedido registrado con el ID " + pedido.getIdPedido() + ".");
        }
        pedidos.add(pedido);
        zonaDeCarga.agregarPedido(pedido);
    }

    /**
     * Busca un pedido registrado por su identificador.
     *
     * @param idPedido identificador del pedido buscado
     * @return el pedido encontrado, o {@code null} si no existe
     */
    public synchronized Pedido buscarPedidoPorId(int idPedido) {
        for (Pedido pedido : pedidos) {
            if (pedido.getIdPedido() == idPedido) {
                return pedido;
            }
        }
        return null;
    }

    /**
     * Cuenta los pedidos registrados que se encuentran en un estado dado.
     *
     * @param estado estado a contar
     * @return cantidad de pedidos en ese estado
     */
    public synchronized int contarPedidos(EstadoPedido estado) {
        int cantidad = 0;
        for (Pedido pedido : pedidos) {
            if (pedido.getEstado() == estado) {
                cantidad++;
            }
        }
        return cantidad;
    }

    /**
     * Registra un repartidor en la plataforma.
     *
     * @param repartidor repartidor a registrar
     */
    public synchronized void registrarRepartidor(Repartidor repartidor) {
        repartidores.add(repartidor);
    }

    /** @return una copia de la lista de pedidos registrados */
    public synchronized List<Pedido> getPedidos() {
        return new ArrayList<>(pedidos);
    }

    /** @return una copia de la lista de repartidores registrados */
    public synchronized List<Repartidor> getRepartidores() {
        return new ArrayList<>(repartidores);
    }

    /**
     * Busca un repartidor registrado por su nombre.
     *
     * @param nombre nombre del repartidor buscado
     * @return el repartidor encontrado, o {@code null} si no existe
     */
    public synchronized Repartidor buscarRepartidorPorNombre(String nombre) {
        for (Repartidor repartidor : repartidores) {
            if (repartidor.getNombre().equalsIgnoreCase(nombre)) {
                return repartidor;
            }
        }
        return null;
    }

    /**
     * Asigna un pedido a un repartidor identificado por su nombre. El
     * controlador resuelve el nombre y delega en la asignación por objeto, que
     * es la única que valida los requisitos y actualiza el estado del pedido.
     *
     * @param pedido pedido a asignar
     * @param nombre nombre del repartidor
     * @return el resultado de la asignación, listo para imprimirse en consola
     */
    public synchronized String asignarRepartidor(Pedido pedido, String nombre) {
        Repartidor repartidor = buscarRepartidorPorNombre(nombre);
        if (repartidor == null) {
            return String.format("No existe un repartidor registrado con el nombre %s.", nombre);
        }
        return asignarPedidoA(pedido, repartidor);
    }

    /**
     * Asigna un pedido a un repartidor y, solo si la validación fue exitosa, lo
     * agrega al recorrido de ese repartidor. Al concentrar ambos pasos en un
     * único método se evita que un repartidor tenga en su lista un pedido que
     * en realidad nunca le fue asignado.
     *
     * @param pedido     pedido a asignar
     * @param repartidor repartidor candidato
     * @return el resultado de la asignación, listo para imprimirse en consola
     */
    public synchronized String asignarPedidoA(Pedido pedido, Repartidor repartidor) {
        String resultado = pedido.asignarRepartidor(repartidor);

        if (pedido.getRepartidorAsignado() == repartidor) {
            repartidor.agregarPedido(pedido);
        }
        return resultado;
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
    public synchronized String asignarAutomaticamente(Pedido pedido) {
        for (Repartidor repartidor : repartidores) {
            if (repartidor.isDisponibleInmediato() && pedido.cumpleRequisitos(repartidor)) {
                String resultado = asignarPedidoA(pedido, repartidor);
                repartidor.setDisponibleInmediato(false);
                return resultado;
            }
        }
        return String.format("No hay repartidores disponibles que cumplan los requisitos del pedido %d.",
                pedido.getIdPedido());
    }

    /**
     * Despacha un pedido para que salga a reparto.
     *
     * @param pedido pedido a despachar
     * @return el resultado de la operación, listo para imprimirse en consola
     */
    public synchronized String despachar(Pedido pedido) {
        return pedido.despachar();
    }

    /**
     * Confirma que un pedido llegó a destino y lo agrega al historial de
     * entregas del sistema. Es el punto que comparten todos los hilos de
     * repartidor, por lo que se protege con {@code synchronized}.
     *
     * @param pedido pedido efectivamente entregado
     * @return {@code true} si la entrega se registró correctamente
     */
    public synchronized boolean registrarEntrega(Pedido pedido) {
        if (!pedido.confirmarEntrega()) {
            return false;
        }
        historialEntregas.add(String.format("%s #%d — entregado por %s",
                pedido.getTipoPedido(),
                pedido.getIdPedido(),
                pedido.getRepartidorAsignado().getNombreCompleto()));
        return true;
    }

    /**
     * Cancela un pedido y libera al repartidor que lo tenía asignado, de modo
     * que vuelva a quedar disponible para otros envíos.
     *
     * @param pedido pedido a cancelar
     * @return el resultado de la operación, listo para imprimirse en consola
     */
    public synchronized String cancelar(Pedido pedido) {
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
    public synchronized List<String> verHistorial() {
        return new ArrayList<>(historialEntregas);
    }
}
