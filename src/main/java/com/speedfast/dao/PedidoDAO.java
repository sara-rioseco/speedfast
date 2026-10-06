package com.speedfast.dao;

import com.speedfast.exception.PersistenciaException;
import com.speedfast.model.EstadoPedido;
import com.speedfast.model.Pedido;
import com.speedfast.model.Repartidor;
import com.speedfast.model.TipoPedido;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la tabla {@code pedidos}. Ofrece las operaciones CRUD
 * ({@link #create}, {@link #readAll}, {@link #update} y {@link #delete}), la
 * búsqueda por ID y la actualización del estado a medida que avanzan las
 * entregas.
 *
 * <p>Todas las sentencias usan {@link PreparedStatement}: los valores viajan
 * como parámetros y no concatenados en el SQL, lo que evita la inyección SQL y
 * errores con caracteres especiales en las direcciones.</p>
 */
public class PedidoDAO {

    private static final String SQL_INSERTAR =
            "INSERT INTO pedidos (direccion, tipo, estado) VALUES (?, ?, ?)";

    private static final String SQL_ACTUALIZAR =
            "UPDATE pedidos SET direccion = ?, tipo = ?, estado = ? WHERE id = ?";

    private static final String SQL_ACTUALIZAR_ESTADO =
            "UPDATE pedidos SET estado = ? WHERE id = ?";

    private static final String SQL_ELIMINAR = "DELETE FROM pedidos WHERE id = ?";

    /**
     * Consulta base: cada pedido junto al repartidor de su última entrega, si
     * la tiene. Un pedido puede tener varias entregas (una por intento), por lo
     * que se une solo con la más reciente para obtener una fila por pedido.
     */
    private static final String SQL_SELECCIONAR = """
            SELECT p.id AS id_pedido, p.direccion, p.tipo, p.estado,
                   r.id AS id_repartidor, r.nombre AS nombre_repartidor
            FROM pedidos p
            LEFT JOIN entregas e
                   ON e.id_pedido = p.id
                  AND e.id = (SELECT MAX(u.id) FROM entregas u WHERE u.id_pedido = p.id)
            LEFT JOIN repartidores r ON r.id = e.id_repartidor
            """;

    /**
     * Listado con filtros opcionales por estado y por tipo. Cada filtro se
     * envía dos veces: si su valor es {@code NULL}, la condición se cumple
     * siempre y el filtro no se aplica.
     */
    private static final String SQL_LISTAR = SQL_SELECCIONAR + """
            WHERE (? IS NULL OR p.estado = ?)
              AND (? IS NULL OR p.tipo = ?)
            ORDER BY p.id
            """;

    private static final String SQL_BUSCAR_POR_ID = SQL_SELECCIONAR + "WHERE p.id = ?";

    /**
     * Inserta un pedido nuevo y le asigna el ID generado por la base de datos.
     *
     * @param pedido pedido a guardar; al terminar queda con su ID definitivo
     * @throws PersistenciaException si no fue posible guardarlo
     */
    public void create(Pedido pedido) throws PersistenciaException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement sentencia =
                     conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            asignarDatos(sentencia, pedido);
            sentencia.executeUpdate();
            pedido.setIdPedido(ConexionDB.leerIdGenerado(sentencia));
        } catch (SQLException e) {
            throw new PersistenciaException("No fue posible registrar el pedido en la base de datos.", e);
        }
    }

    /**
     * Lista todos los pedidos guardados.
     *
     * @return los pedidos, ordenados por ID
     * @throws PersistenciaException si no fue posible consultarlos
     */
    public List<Pedido> readAll() throws PersistenciaException {
        return readAll(null, null);
    }

    /**
     * Lista los pedidos que cumplen los filtros indicados. Cada fila se
     * convierte en la subclase que corresponde a su tipo, con su estado y el
     * repartidor de su última entrega.
     *
     * @param estado estado de los pedidos buscados, o {@code null} para no filtrar por estado
     * @param tipo   tipo de los pedidos buscados, o {@code null} para no filtrar por tipo
     * @return los pedidos encontrados, ordenados por ID
     * @throws PersistenciaException si no fue posible consultarlos, o si una
     *                               fila tiene datos no válidos
     */
    public List<Pedido> readAll(EstadoPedido estado, TipoPedido tipo) throws PersistenciaException {
        String valorEstado = estado == null ? null : estado.name();
        String valorTipo = tipo == null ? null : tipo.name();
        List<Pedido> pedidos = new ArrayList<>();

        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR)) {
            sentencia.setString(1, valorEstado);
            sentencia.setString(2, valorEstado);
            sentencia.setString(3, valorTipo);
            sentencia.setString(4, valorTipo);
            try (ResultSet filas = sentencia.executeQuery()) {
                while (filas.next()) {
                    pedidos.add(leerPedidoConRepartidor(filas));
                }
            }
        } catch (SQLException | IllegalArgumentException e) {
            throw new PersistenciaException("No fue posible consultar los pedidos en la base de datos.", e);
        }
        return pedidos;
    }

    /**
     * Busca un pedido por su ID.
     *
     * @param idPedido ID del pedido buscado
     * @return el pedido, o {@code null} si no existe
     * @throws PersistenciaException si no fue posible consultarlo
     */
    public Pedido readById(int idPedido) throws PersistenciaException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR_POR_ID)) {
            sentencia.setInt(1, idPedido);
            try (ResultSet fila = sentencia.executeQuery()) {
                return fila.next() ? leerPedidoConRepartidor(fila) : null;
            }
        } catch (SQLException | IllegalArgumentException e) {
            throw new PersistenciaException("No fue posible consultar el pedido #" + idPedido + ".", e);
        }
    }

    /**
     * Actualiza la dirección, el tipo y el estado de un pedido existente.
     *
     * @param pedido pedido con los datos nuevos y el ID del registro a modificar
     * @throws PersistenciaException si no fue posible actualizarlo o el pedido ya no existe
     */
    public void update(Pedido pedido) throws PersistenciaException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_ACTUALIZAR)) {
            asignarDatos(sentencia, pedido);
            sentencia.setInt(4, pedido.getIdPedido());
            ConexionDB.exigirFilaAfectada(sentencia.executeUpdate(), "el pedido #" + pedido.getIdPedido());
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "No fue posible actualizar el pedido #" + pedido.getIdPedido() + ".", e);
        }
    }

    /**
     * Elimina un pedido junto a sus entregas, dentro de una misma transacción.
     * Las entregas deben eliminarse primero, porque su clave foránea apunta al
     * pedido. Si alguna de las dos operaciones falla se deshacen ambas, de
     * modo que nunca queda un pedido a medio eliminar.
     *
     * @param idPedido ID del pedido a eliminar
     * @return cantidad de entregas que se eliminaron junto al pedido
     * @throws PersistenciaException si no fue posible eliminarlo o el pedido ya no existe
     */
    public int delete(int idPedido) throws PersistenciaException {
        Connection conexion = null;
        try {
            conexion = ConexionDB.conectar();
            conexion.setAutoCommit(false);

            int entregasEliminadas = EntregaDAO.deleteByPedido(conexion, idPedido);
            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_ELIMINAR)) {
                sentencia.setInt(1, idPedido);
                ConexionDB.exigirFilaAfectada(sentencia.executeUpdate(), "el pedido #" + idPedido);
            }

            conexion.commit();
            return entregasEliminadas;
        } catch (SQLException e) {
            ConexionDB.deshacer(conexion);
            throw new PersistenciaException("No fue posible eliminar el pedido #" + idPedido + ".", e);
        } finally {
            ConexionDB.cerrar(conexion);
        }
    }

    /**
     * Guarda el estado actual de un pedido, abriendo su propia conexión. Lo
     * usan los repartidores al confirmar una entrega.
     *
     * @param pedido pedido cuyo estado cambió
     * @throws PersistenciaException si no fue posible actualizarlo
     */
    public void actualizarEstado(Pedido pedido) throws PersistenciaException {
        try (Connection conexion = ConexionDB.conectar()) {
            actualizarEstado(conexion, pedido);
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "No fue posible actualizar el estado del pedido #" + pedido.getIdPedido() + ".", e);
        }
    }

    /**
     * Sobrecarga que guarda el estado usando una conexión ya abierta, para
     * participar en una transacción mayor (la usa {@link EntregaDAO}). No
     * cierra la conexión: eso le corresponde a quien la abrió.
     *
     * @param conexion conexión abierta, posiblemente con una transacción en curso
     * @param pedido   pedido cuyo estado cambió
     * @throws SQLException si la sentencia falla o el pedido no existe
     */
    static void actualizarEstado(Connection conexion, Pedido pedido) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_ACTUALIZAR_ESTADO)) {
            sentencia.setString(1, pedido.getEstado().name());
            sentencia.setInt(2, pedido.getIdPedido());
            ConexionDB.exigirFilaAfectada(sentencia.executeUpdate(), "el pedido #" + pedido.getIdPedido());
        }
    }

    /**
     * Construye un pedido, sin repartidor, a partir de la fila actual del
     * {@link ResultSet}. La reutiliza {@link EntregaDAO}, cuyas consultas
     * devuelven las columnas del pedido con los mismos nombres.
     *
     * @param fila resultado posicionado en la fila a leer
     * @return el pedido reconstruido, de la subclase que corresponde a su tipo
     * @throws SQLException si no es posible leer alguna columna, o si el pedido
     *                      no tiene tipo o estado registrado
     */
    static Pedido leerPedido(ResultSet fila) throws SQLException {
        int idPedido = fila.getInt("id_pedido");
        String tipo = fila.getString("tipo");
        String estado = fila.getString("estado");
        if (tipo == null || estado == null) {
            throw new SQLException("El pedido #" + idPedido + " no tiene tipo o estado registrado.");
        }
        Pedido pedido = TipoPedido.valueOf(tipo).crearPedido(idPedido, fila.getString("direccion"));
        pedido.restablecer(EstadoPedido.valueOf(estado), null);
        return pedido;
    }

    /**
     * Construye un pedido junto al repartidor de su última entrega. Un pedido
     * pendiente no tiene repartidor, aunque tenga registrado un intento de
     * entrega anterior.
     *
     * @param fila resultado posicionado en la fila a leer
     * @return el pedido reconstruido
     * @throws SQLException si no es posible leer alguna columna
     */
    private static Pedido leerPedidoConRepartidor(ResultSet fila) throws SQLException {
        Pedido pedido = leerPedido(fila);
        int idRepartidor = fila.getInt("id_repartidor");
        if (!fila.wasNull() && pedido.getEstado() != EstadoPedido.PENDIENTE) {
            pedido.restablecer(pedido.getEstado(),
                    new Repartidor(idRepartidor, fila.getString("nombre_repartidor")));
        }
        return pedido;
    }

    /**
     * Asigna la dirección, el tipo y el estado del pedido a los tres primeros
     * parámetros de una sentencia. Lo comparten el {@code INSERT} y el {@code UPDATE}.
     *
     * @param sentencia sentencia preparada
     * @param pedido    pedido del que se toman los datos
     * @throws SQLException si no es posible asignar algún parámetro
     */
    private static void asignarDatos(PreparedStatement sentencia, Pedido pedido) throws SQLException {
        sentencia.setString(1, pedido.getDireccionEntrega());
        sentencia.setString(2, pedido.getTipo().name());
        sentencia.setString(3, pedido.getEstado().name());
    }
}
