package com.speedfast.service;

import com.speedfast.dao.EntregaDAO;
import com.speedfast.dao.PedidoDAO;
import com.speedfast.dao.RepartidorDAO;
import com.speedfast.exception.PersistenciaException;
import com.speedfast.model.Entrega;
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
 * <p>Es el controlador que comparten todas las ventanas de la interfaz
 * gráfica. Desde la Semana 7, cada cambio se guarda en la base de datos por
 * medio de los DAO ({@link PedidoDAO}, {@link RepartidorDAO} y
 * {@link EntregaDAO}): las ventanas siguen trabajando solo con este
 * controlador y no conocen JDBC. En memoria se mantienen los pedidos y
 * repartidores con los que trabajan los hilos de la simulación.</p>
 *
 * <p>Varios repartidores lo utilizan al mismo tiempo desde hilos distintos,
 * por lo que las listas en memoria se protegen con {@code synchronized}. Las
 * operaciones con la base de datos se realizan fuera de esos bloqueos: cada
 * una abre su propia conexión, así que no comparten recursos entre hilos y
 * ningún repartidor queda esperando a otro mientras se completa una consulta.</p>
 */
public class ControladorDeEnvios implements Rastreable {

    /** Pedidos con los que trabaja la simulación. */
    private final List<Pedido> pedidos = new ArrayList<>();

    /** Repartidores disponibles en la plataforma. */
    private final List<Repartidor> repartidores = new ArrayList<>();

    /** Entregas ya realizadas en esta sesión, en el orden en que ocurrieron. */
    private final List<String> historialEntregas = new ArrayList<>();

    /** Zona de carga donde esperan los pedidos registrados hasta que un repartidor los retira. */
    private final ZonaDeCarga zonaDeCarga;

    /** Acceso a la tabla pedido. */
    private final PedidoDAO pedidoDAO = new PedidoDAO();

    /** Acceso a la tabla repartidor. */
    private final RepartidorDAO repartidorDAO = new RepartidorDAO();

    /** Acceso a la tabla entrega. */
    private final EntregaDAO entregaDAO = new EntregaDAO();

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
     * Carga los repartidores y pedidos guardados en la base de datos. Los
     * pedidos pendientes quedan en la zona de carga, listos para la próxima
     * ronda de entregas.
     *
     * <p>Si un pedido figura en reparto, la aplicación se cerró mientras se
     * entregaba: ese intento queda registrado en la tabla entrega y el pedido
     * vuelve a pendiente, para que se entregue en la próxima ronda.</p>
     *
     * @throws PersistenciaException si no fue posible leer la base de datos
     */
    public void cargarDatos() throws PersistenciaException {
        for (Repartidor repartidor : repartidorDAO.listarTodos()) {
            incorporarRepartidor(repartidor);
        }
        for (Pedido pedido : pedidoDAO.listarTodos()) {
            if (pedido.reintentarEntrega()) {
                pedidoDAO.actualizarEstado(pedido);
                System.out.printf("[Sistema] El pedido #%d quedó en reparto al cerrar la aplicación: "
                        + "vuelve a la zona de carga.%n", pedido.getIdPedido());
            }
            incorporarPedido(pedido);
        }
    }

    /**
     * Guarda un pedido nuevo en la base de datos, que le asigna su ID, y lo
     * deja en la zona de carga para que un repartidor lo retire. Si no es
     * posible guardarlo, el pedido tampoco se agrega a la simulación.
     *
     * @param pedido pedido a registrar
     * @throws PersistenciaException si no fue posible guardarlo
     */
    public void registrarPedido(Pedido pedido) throws PersistenciaException {
        pedidoDAO.guardar(pedido);
        incorporarPedido(pedido);
    }

    /**
     * Agrega un pedido ya guardado a la simulación. Solo los pendientes pasan
     * a la zona de carga.
     *
     * @param pedido pedido a incorporar
     */
    private synchronized void incorporarPedido(Pedido pedido) {
        pedidos.add(pedido);
        if (pedido.getEstado() == EstadoPedido.PENDIENTE) {
            zonaDeCarga.agregarPedido(pedido);
        }
    }

    /**
     * Busca un pedido de la simulación por su identificador.
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
     * Cuenta los pedidos de la simulación que se encuentran en un estado dado.
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
     * Guarda un repartidor nuevo en la base de datos, que le asigna su ID, y
     * lo incorpora a la operación: participará desde la próxima ronda.
     *
     * @param repartidor repartidor a registrar
     * @throws PersistenciaException si no fue posible guardarlo
     */
    public void registrarRepartidor(Repartidor repartidor) throws PersistenciaException {
        repartidorDAO.guardar(repartidor);
        incorporarRepartidor(repartidor);
    }

    /**
     * Vincula un repartidor ya guardado a la zona de carga y a este
     * controlador, y lo agrega a la plataforma.
     *
     * @param repartidor repartidor a incorporar
     */
    private synchronized void incorporarRepartidor(Repartidor repartidor) {
        repartidor.vincular(zonaDeCarga, this);
        repartidores.add(repartidor);
    }

    /** @return una copia de la lista de pedidos de la simulación */
    public synchronized List<Pedido> getPedidos() {
        return new ArrayList<>(pedidos);
    }

    /** @return una copia de la lista de repartidores de la plataforma */
    public synchronized List<Repartidor> getRepartidores() {
        return new ArrayList<>(repartidores);
    }

    /**
     * Consulta los pedidos guardados en la base de datos, cada uno con el
     * repartidor de su última entrega. Es la fuente del listado de pedidos.
     *
     * @return los pedidos guardados, ordenados por ID
     * @throws PersistenciaException si no fue posible consultarlos
     */
    public List<Pedido> consultarPedidos() throws PersistenciaException {
        return pedidoDAO.listarTodos();
    }

    /**
     * Consulta los repartidores guardados en la base de datos.
     *
     * @return los repartidores guardados, ordenados por ID
     * @throws PersistenciaException si no fue posible consultarlos
     */
    public List<Repartidor> consultarRepartidores() throws PersistenciaException {
        return repartidorDAO.listarTodos();
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
     * Despacha un pedido para que salga a reparto. Si el despacho se concreta,
     * registra en la base de datos la entrega (qué repartidor lleva el pedido,
     * con fecha y hora) junto al nuevo estado del pedido.
     *
     * @param pedido pedido a despachar
     * @return el resultado de la operación, listo para imprimirse en consola
     * @throws PersistenciaException si no fue posible registrar la entrega
     */
    public String despachar(Pedido pedido) throws PersistenciaException {
        boolean yaEstabaEnReparto = pedido.getEstado() == EstadoPedido.EN_REPARTO;
        String resultado = pedido.despachar();

        if (!yaEstabaEnReparto && pedido.getEstado() == EstadoPedido.EN_REPARTO) {
            entregaDAO.guardar(new Entrega(pedido, pedido.getRepartidorAsignado()));
        }
        return resultado;
    }

    /**
     * Confirma que un pedido llegó a destino, guarda su nuevo estado en la
     * base de datos y lo agrega al historial de entregas del sistema.
     *
     * @param pedido pedido efectivamente entregado
     * @return {@code true} si la entrega se registró correctamente
     * @throws PersistenciaException si no fue posible guardar el nuevo estado
     */
    public boolean registrarEntrega(Pedido pedido) throws PersistenciaException {
        if (!pedido.confirmarEntrega()) {
            return false;
        }
        pedidoDAO.actualizarEstado(pedido);
        anotarEnHistorial(pedido);
        return true;
    }

    /**
     * Agrega una entrega al historial. Es el punto que comparten todos los
     * hilos de repartidor, por lo que se protege con {@code synchronized}.
     *
     * @param pedido pedido entregado
     */
    private synchronized void anotarEnHistorial(Pedido pedido) {
        historialEntregas.add(String.format("%s #%d — entregado por %s",
                pedido.getTipoPedido(),
                pedido.getIdPedido(),
                pedido.getRepartidorAsignado().getNombreCompleto()));
    }

    /**
     * Cancela un pedido, guarda su nuevo estado y libera al repartidor que lo
     * tenía asignado, de modo que vuelva a quedar disponible para otros envíos.
     *
     * @param pedido pedido a cancelar
     * @return el resultado de la operación, listo para imprimirse en consola
     * @throws PersistenciaException si no fue posible guardar el nuevo estado
     */
    public String cancelar(Pedido pedido) throws PersistenciaException {
        Repartidor asignado = pedido.getRepartidorAsignado();
        String resultado = pedido.cancelar();

        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            if (asignado != null) {
                asignado.setDisponibleInmediato(true);
            }
            pedidoDAO.actualizarEstado(pedido);
        }
        return resultado;
    }

    /**
     * Entrega el historial de entregas realizadas en esta sesión.
     *
     * @return una copia de la lista de entregas realizadas
     */
    @Override
    public synchronized List<String> verHistorial() {
        return new ArrayList<>(historialEntregas);
    }
}
