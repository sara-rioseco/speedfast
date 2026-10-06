package com.speedfast.service;

import com.speedfast.dao.EntregaDAO;
import com.speedfast.dao.PedidoDAO;
import com.speedfast.dao.RepartidorDAO;
import com.speedfast.exception.OperacionNoPermitidaException;
import com.speedfast.exception.PersistenciaException;
import com.speedfast.model.Entrega;
import com.speedfast.model.EstadoPedido;
import com.speedfast.model.Pedido;
import com.speedfast.model.Rastreable;
import com.speedfast.model.Repartidor;
import com.speedfast.model.TipoPedido;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Controlador central de los envíos de SpeedFast. Concentra la lógica de
 * gestión del sistema: las operaciones CRUD de pedidos, repartidores y
 * entregas, la preparación de cada ronda de entregas y el historial.
 *
 * <p>Implementa {@link Rastreable} para exponer el historial general del
 * sistema, mientras que cada {@link Pedido} mantiene el suyo propio.</p>
 *
 * <p>Es el controlador que comparten todas las ventanas de la interfaz
 * gráfica: aplica las reglas del negocio y delega el acceso a datos en los
 * DAO ({@link PedidoDAO}, {@link RepartidorDAO} y {@link EntregaDAO}), de modo
 * que las ventanas no conocen JDBC. Cada vez que los datos cambian, avisa a los
 * {@link ObservadorDeCambios} registrados para que actualicen sus tablas.</p>
 *
 * <p>La base de datos es la fuente de los datos. En memoria solo se mantienen
 * los pedidos y repartidores de la ronda de entregas en curso, que se cargan
 * desde la base de datos al comenzar cada ronda. Mientras la ronda dura, se
 * rechazan las operaciones que podrían interferir con los repartidores
 * (modificar o eliminar registros y registrar entregas manualmente).</p>
 *
 * <p>Varios repartidores lo utilizan al mismo tiempo desde hilos distintos,
 * por lo que las listas en memoria y la marca de ronda en curso se protegen
 * con {@code synchronized}. Los repartidores registran sus entregas fuera de
 * esos bloqueos: cada operación abre su propia conexión, así que no comparten
 * recursos entre hilos.</p>
 */
public class ControladorDeEnvios implements Rastreable {

    /** Pedidos de la ronda de entregas en curso. */
    private final List<Pedido> pedidos = new ArrayList<>();

    /** Repartidores de la ronda de entregas en curso. */
    private final List<Repartidor> repartidores = new ArrayList<>();

    /** Entregas ya realizadas en esta sesión, en el orden en que ocurrieron. */
    private final List<String> historialEntregas = new ArrayList<>();

    /**
     * Ventanas que deben enterarse de los cambios. Se agregan y quitan desde
     * el hilo gráfico y se recorren también desde los hilos de los
     * repartidores: {@link CopyOnWriteArrayList} permite recorrerla sin
     * bloqueos mientras otra ventana se agrega o se quita.
     */
    private final List<ObservadorDeCambios> observadores = new CopyOnWriteArrayList<>();

    /** Zona de carga donde esperan los pedidos de la ronda hasta que un repartidor los retira. */
    private final ZonaDeCarga zonaDeCarga;

    /** Acceso a la tabla pedidos. */
    private final PedidoDAO pedidoDAO = new PedidoDAO();

    /** Acceso a la tabla repartidores. */
    private final RepartidorDAO repartidorDAO = new RepartidorDAO();

    /** Acceso a la tabla entregas. */
    private final EntregaDAO entregaDAO = new EntregaDAO();

    /** Indica si hay una ronda de entregas en curso. Solo se usa dentro de métodos {@code synchronized}. */
    private boolean rondaEnCurso;

    /**
     * Crea el controlador asociado a la zona de carga donde quedarán los
     * pedidos de cada ronda.
     *
     * @param zonaDeCarga zona de carga compartida con los repartidores
     */
    public ControladorDeEnvios(ZonaDeCarga zonaDeCarga) {
        this.zonaDeCarga = zonaDeCarga;
    }

    /**
     * Comprueba al iniciar la aplicación que la base de datos esté disponible
     * y tenga las tres tablas del modelo, e informa por consola cuántos
     * registros contiene.
     *
     * @throws PersistenciaException si no fue posible consultar alguna de las tablas
     */
    public void verificarBaseDeDatos() throws PersistenciaException {
        System.out.printf("[Sistema] Base de datos disponible: %d repartidores, %d pedidos y %d entregas.%n",
                repartidorDAO.readAll().size(), pedidoDAO.readAll().size(), entregaDAO.readAll().size());
    }

    /**
     * Agrega una ventana a las que se avisa cuando los datos cambian.
     *
     * @param observador ventana interesada en los cambios
     */
    public void agregarObservador(ObservadorDeCambios observador) {
        observadores.add(observador);
    }

    /**
     * Deja de avisar a una ventana, por ejemplo porque se cerró.
     *
     * @param observador ventana que ya no necesita los avisos
     */
    public void quitarObservador(ObservadorDeCambios observador) {
        observadores.remove(observador);
    }

    /** Avisa a todas las ventanas registradas que los datos cambiaron. */
    private void notificarCambios() {
        for (ObservadorDeCambios observador : observadores) {
            observador.datosActualizados();
        }
    }

    // ------------------------------------------------------------------
    // Pedidos
    // ------------------------------------------------------------------

    /**
     * Guarda un pedido nuevo en la base de datos, que le asigna su ID. Se
     * permite durante una ronda: si queda pendiente, participa en la siguiente.
     *
     * @param pedido pedido a registrar
     * @throws PersistenciaException si no fue posible guardarlo
     */
    public void registrarPedido(Pedido pedido) throws PersistenciaException {
        pedidoDAO.create(pedido);
        notificarCambios();
    }

    /**
     * Guarda los cambios de dirección, tipo o estado de un pedido existente.
     *
     * @param pedido pedido con los datos nuevos y el ID del registro a modificar
     * @throws PersistenciaException         si no fue posible guardarlo
     * @throws OperacionNoPermitidaException si hay una ronda de entregas en curso
     */
    public synchronized void actualizarPedido(Pedido pedido)
            throws PersistenciaException, OperacionNoPermitidaException {
        exigirSinRondaEnCurso();
        pedidoDAO.update(pedido);
        notificarCambios();
    }

    /**
     * Elimina un pedido junto a sus entregas registradas, que son el historial
     * de sus intentos de entrega y no tienen sentido sin él.
     *
     * @param idPedido ID del pedido a eliminar
     * @return cantidad de entregas que se eliminaron junto al pedido
     * @throws PersistenciaException         si no fue posible eliminarlo
     * @throws OperacionNoPermitidaException si hay una ronda de entregas en curso
     */
    public synchronized int eliminarPedido(int idPedido)
            throws PersistenciaException, OperacionNoPermitidaException {
        exigirSinRondaEnCurso();
        int entregasEliminadas = pedidoDAO.delete(idPedido);
        notificarCambios();
        return entregasEliminadas;
    }

    /**
     * Consulta todos los pedidos guardados, cada uno con el repartidor de su
     * última entrega.
     *
     * @return los pedidos, ordenados por ID
     * @throws PersistenciaException si no fue posible consultarlos
     */
    public List<Pedido> consultarPedidos() throws PersistenciaException {
        return pedidoDAO.readAll();
    }

    /**
     * Consulta los pedidos guardados, filtrados de forma opcional por estado y por tipo.
     *
     * @param estado estado buscado, o {@code null} para no filtrar por estado
     * @param tipo   tipo buscado, o {@code null} para no filtrar por tipo
     * @return los pedidos que cumplen los filtros, ordenados por ID
     * @throws PersistenciaException si no fue posible consultarlos
     */
    public List<Pedido> consultarPedidos(EstadoPedido estado, TipoPedido tipo) throws PersistenciaException {
        return pedidoDAO.readAll(estado, tipo);
    }

    // ------------------------------------------------------------------
    // Repartidores
    // ------------------------------------------------------------------

    /**
     * Guarda un repartidor nuevo en la base de datos, que le asigna su ID. Se
     * permite durante una ronda: el repartidor participa desde la siguiente.
     *
     * @param repartidor repartidor a registrar
     * @throws PersistenciaException si no fue posible guardarlo
     */
    public void registrarRepartidor(Repartidor repartidor) throws PersistenciaException {
        repartidorDAO.create(repartidor);
        notificarCambios();
    }

    /**
     * Guarda el nuevo nombre de un repartidor existente.
     *
     * @param repartidor repartidor con el nombre nuevo y el ID del registro a modificar
     * @throws PersistenciaException         si no fue posible guardarlo
     * @throws OperacionNoPermitidaException si hay una ronda de entregas en curso
     */
    public synchronized void actualizarRepartidor(Repartidor repartidor)
            throws PersistenciaException, OperacionNoPermitidaException {
        exigirSinRondaEnCurso();
        repartidorDAO.update(repartidor);
        notificarCambios();
    }

    /**
     * Elimina un repartidor que no tiene entregas registradas. Si tiene, no se
     * elimina: sus entregas indican quién llevó cada pedido, y borrarlas dejaría
     * pedidos entregados sin ese registro.
     *
     * @param idRepartidor ID del repartidor a eliminar
     * @throws PersistenciaException         si no fue posible eliminarlo
     * @throws OperacionNoPermitidaException si tiene entregas registradas o hay una ronda en curso
     */
    public synchronized void eliminarRepartidor(int idRepartidor)
            throws PersistenciaException, OperacionNoPermitidaException {
        exigirSinRondaEnCurso();
        int entregas = entregaDAO.readAll(null, idRepartidor).size();
        if (entregas > 0) {
            throw new OperacionNoPermitidaException(String.format(
                    "El repartidor #%d tiene %d entrega(s) registrada(s), por lo que no se puede eliminar.%n"
                            + "Elimina o asigna a otro repartidor esas entregas en Gestión de entregas.",
                    idRepartidor, entregas));
        }
        repartidorDAO.delete(idRepartidor);
        notificarCambios();
    }

    /**
     * Consulta los repartidores guardados en la base de datos.
     *
     * @return los repartidores, ordenados por ID
     * @throws PersistenciaException si no fue posible consultarlos
     */
    public List<Repartidor> consultarRepartidores() throws PersistenciaException {
        return repartidorDAO.readAll();
    }

    // ------------------------------------------------------------------
    // Entregas
    // ------------------------------------------------------------------

    /**
     * Registra manualmente la entrega de un pedido pendiente: el pedido queda
     * asignado al repartidor y pasa a estar en reparto, igual que cuando un
     * repartidor de la simulación lo retira. La entrega y el nuevo estado del
     * pedido se guardan en una misma transacción.
     *
     * @param idPedido     ID del pedido que sale a reparto
     * @param idRepartidor ID del repartidor que lo lleva
     * @param fecha        fecha de la entrega
     * @param hora         hora de la entrega
     * @return la entrega registrada, con su ID
     * @throws PersistenciaException         si no fue posible guardarla
     * @throws OperacionNoPermitidaException si el pedido no está pendiente, el
     *                                       pedido o el repartidor ya no existen,
     *                                       o hay una ronda en curso
     */
    public synchronized Entrega registrarEntrega(int idPedido, int idRepartidor, LocalDate fecha, LocalTime hora)
            throws PersistenciaException, OperacionNoPermitidaException {
        exigirSinRondaEnCurso();
        Pedido pedido = buscarPedido(idPedido);
        Repartidor repartidor = buscarRepartidor(idRepartidor);

        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new OperacionNoPermitidaException(String.format(
                    "El pedido #%d se encuentra %s.%nSolo se pueden registrar entregas de pedidos pendientes.",
                    idPedido, pedido.getEstado()));
        }
        // Se reutilizan las reglas del modelo: la asignación valida los requisitos
        // del tipo de pedido, y el despacho lo deja en reparto.
        pedido.asignarRepartidor(repartidor);
        pedido.despachar();
        if (pedido.getEstado() != EstadoPedido.EN_REPARTO) {
            throw new OperacionNoPermitidaException(String.format(
                    "%s no cumple los requisitos del pedido #%d.", repartidor.getNombreCompleto(), idPedido));
        }

        Entrega entrega = new Entrega(0, pedido, repartidor, fecha, hora);
        entregaDAO.create(entrega);
        notificarCambios();
        return entrega;
    }

    /**
     * Corrige el repartidor, la fecha o la hora de una entrega. El pedido no
     * cambia: una entrega es parte del historial de su pedido.
     *
     * @param idEntrega    ID de la entrega a modificar
     * @param idRepartidor ID del repartidor que la realizó
     * @param fecha        fecha de la entrega
     * @param hora         hora de la entrega
     * @throws PersistenciaException         si no fue posible guardarla
     * @throws OperacionNoPermitidaException si la entrega o el repartidor ya no
     *                                       existen, o hay una ronda en curso
     */
    public synchronized void actualizarEntrega(int idEntrega, int idRepartidor, LocalDate fecha, LocalTime hora)
            throws PersistenciaException, OperacionNoPermitidaException {
        exigirSinRondaEnCurso();
        Entrega actual = buscarEntrega(idEntrega);
        Repartidor repartidor = buscarRepartidor(idRepartidor);
        entregaDAO.update(new Entrega(idEntrega, actual.getPedido(), repartidor, fecha, hora));
        notificarCambios();
    }

    /**
     * Elimina una entrega. Si era la más reciente de un pedido que está en
     * reparto, el pedido vuelve a pendiente: ya no hay un repartidor que lo
     * lleve. El cambio de estado y la eliminación se guardan en una misma
     * transacción.
     *
     * @param idEntrega ID de la entrega a eliminar
     * @return {@code true} si su pedido volvió a pendiente
     * @throws PersistenciaException         si no fue posible eliminarla
     * @throws OperacionNoPermitidaException si la entrega ya no existe o hay una ronda en curso
     */
    public synchronized boolean eliminarEntrega(int idEntrega)
            throws PersistenciaException, OperacionNoPermitidaException {
        exigirSinRondaEnCurso();
        Entrega entrega = buscarEntrega(idEntrega);
        Pedido pedido = entrega.getPedido();

        List<Entrega> entregasDelPedido = entregaDAO.readAll(pedido.getIdPedido(), null);
        boolean esLaMasReciente = entregasDelPedido.getLast().getIdEntrega() == idEntrega;
        boolean vuelveAPendiente = esLaMasReciente && pedido.reintentarEntrega();

        entregaDAO.delete(entrega);
        notificarCambios();
        return vuelveAPendiente;
    }

    /**
     * Consulta las entregas guardadas, filtradas de forma opcional por pedido y por repartidor.
     *
     * @param idPedido     ID del pedido, o {@code null} para no filtrar por pedido
     * @param idRepartidor ID del repartidor, o {@code null} para no filtrar por repartidor
     * @return las entregas que cumplen los filtros, ordenadas por ID
     * @throws PersistenciaException si no fue posible consultarlas
     */
    public List<Entrega> consultarEntregas(Integer idPedido, Integer idRepartidor) throws PersistenciaException {
        return entregaDAO.readAll(idPedido, idRepartidor);
    }

    /**
     * Busca un pedido en la base de datos.
     *
     * @param idPedido ID del pedido buscado
     * @return el pedido encontrado
     * @throws PersistenciaException         si no fue posible consultarlo
     * @throws OperacionNoPermitidaException si el pedido ya no existe
     */
    private Pedido buscarPedido(int idPedido) throws PersistenciaException, OperacionNoPermitidaException {
        Pedido pedido = pedidoDAO.readById(idPedido);
        if (pedido == null) {
            throw new OperacionNoPermitidaException("El pedido #" + idPedido + " ya no existe.");
        }
        return pedido;
    }

    /**
     * Busca un repartidor en la base de datos.
     *
     * @param idRepartidor ID del repartidor buscado
     * @return el repartidor encontrado
     * @throws PersistenciaException         si no fue posible consultarlo
     * @throws OperacionNoPermitidaException si el repartidor ya no existe
     */
    private Repartidor buscarRepartidor(int idRepartidor)
            throws PersistenciaException, OperacionNoPermitidaException {
        Repartidor repartidor = repartidorDAO.readById(idRepartidor);
        if (repartidor == null) {
            throw new OperacionNoPermitidaException("El repartidor #" + idRepartidor + " ya no existe.");
        }
        return repartidor;
    }

    /**
     * Busca una entrega en la base de datos.
     *
     * @param idEntrega ID de la entrega buscada
     * @return la entrega encontrada
     * @throws PersistenciaException         si no fue posible consultarla
     * @throws OperacionNoPermitidaException si la entrega ya no existe
     */
    private Entrega buscarEntrega(int idEntrega) throws PersistenciaException, OperacionNoPermitidaException {
        Entrega entrega = entregaDAO.readById(idEntrega);
        if (entrega == null) {
            throw new OperacionNoPermitidaException("La entrega #" + idEntrega + " ya no existe.");
        }
        return entrega;
    }

    // ------------------------------------------------------------------
    // Rondas de entregas (simulación concurrente)
    // ------------------------------------------------------------------

    /**
     * Prepara una ronda de entregas: carga desde la base de datos a los
     * repartidores y los pedidos pendientes, y deja los pedidos en la zona de
     * carga. Desde este momento y hasta {@link #finalizarRonda()}, se rechazan
     * las operaciones que podrían interferir con los repartidores.
     *
     * <p>Como los datos se cargan al comenzar cada ronda, la simulación
     * siempre trabaja con lo que está guardado, incluidos los registros que se
     * crearon, modificaron o eliminaron desde las ventanas de gestión.</p>
     *
     * @return los repartidores que participarán en la ronda
     * @throws PersistenciaException         si no fue posible leer la base de datos
     * @throws OperacionNoPermitidaException si no hay pedidos pendientes o repartidores registrados
     */
    public synchronized List<Repartidor> prepararRonda()
            throws PersistenciaException, OperacionNoPermitidaException {
        List<Pedido> pendientes = pedidoDAO.readAll(EstadoPedido.PENDIENTE, null);
        if (pendientes.isEmpty()) {
            throw new OperacionNoPermitidaException(
                    "No hay pedidos pendientes de entrega.\nRegistra un pedido para iniciar una nueva ronda.");
        }
        List<Repartidor> registrados = repartidorDAO.readAll();
        if (registrados.isEmpty()) {
            throw new OperacionNoPermitidaException(
                    "No hay repartidores registrados.\nRegistra al menos uno para iniciar las entregas.");
        }

        pedidos.clear();
        repartidores.clear();
        zonaDeCarga.vaciar();
        for (Repartidor repartidor : registrados) {
            repartidor.vincular(zonaDeCarga, this);
            repartidores.add(repartidor);
        }
        for (Pedido pedido : pendientes) {
            pedidos.add(pedido);
            zonaDeCarga.agregarPedido(pedido);
        }
        rondaEnCurso = true;
        return new ArrayList<>(repartidores);
    }

    /** Marca el término de la ronda: vuelven a permitirse todas las operaciones. */
    public synchronized void finalizarRonda() {
        rondaEnCurso = false;
    }

    /**
     * Rechaza una operación si hay una ronda en curso. Se invoca dentro de
     * métodos {@code synchronized}, de modo que una ronda no puede comenzar
     * entre esta comprobación y la operación que protege.
     *
     * @throws OperacionNoPermitidaException si hay una ronda de entregas en curso
     */
    private void exigirSinRondaEnCurso() throws OperacionNoPermitidaException {
        if (rondaEnCurso) {
            throw new OperacionNoPermitidaException("Hay una ronda de entregas en curso.\n"
                    + "Espera a que termine para modificar o eliminar registros, o para registrar entregas.");
        }
    }

    /**
     * Busca un pedido de la ronda en curso por su identificador.
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
     * Cuenta los pedidos de la ronda en curso que se encuentran en un estado dado.
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

    /** @return una copia de la lista de pedidos de la ronda en curso */
    public synchronized List<Pedido> getPedidos() {
        return new ArrayList<>(pedidos);
    }

    /** @return una copia de la lista de repartidores de la ronda en curso */
    public synchronized List<Repartidor> getRepartidores() {
        return new ArrayList<>(repartidores);
    }

    /**
     * Busca un repartidor de la ronda en curso por su nombre.
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
            entregaDAO.create(new Entrega(pedido, pedido.getRepartidorAsignado()));
            notificarCambios();
        }
        return resultado;
    }

    /**
     * Confirma que un pedido llegó a destino, guarda su nuevo estado en la
     * base de datos y lo agrega al historial de entregas del sistema.
     *
     * @param pedido pedido efectivamente entregado
     * @return {@code true} si la entrega se confirmó correctamente
     * @throws PersistenciaException si no fue posible guardar el nuevo estado
     */
    public boolean confirmarEntrega(Pedido pedido) throws PersistenciaException {
        if (!pedido.confirmarEntrega()) {
            return false;
        }
        pedidoDAO.actualizarEstado(pedido);
        anotarEnHistorial(pedido);
        notificarCambios();
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
     * Entrega el historial de entregas realizadas en esta sesión.
     *
     * @return una copia de la lista de entregas realizadas
     */
    @Override
    public synchronized List<String> verHistorial() {
        return new ArrayList<>(historialEntregas);
    }
}
