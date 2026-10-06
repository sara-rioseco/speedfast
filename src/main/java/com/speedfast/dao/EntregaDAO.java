package com.speedfast.dao;

import com.speedfast.exception.PersistenciaException;
import com.speedfast.model.Entrega;
import com.speedfast.model.Repartidor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la tabla {@code entregas}, que relaciona cada pedido con el
 * repartidor que lo lleva, junto a la fecha y hora en que salió a reparto.
 * Ofrece las operaciones CRUD ({@link #create}, {@link #readAll},
 * {@link #update} y {@link #delete}) y la búsqueda por ID.
 *
 * <p>Registrar o eliminar una entrega puede cambiar el estado de su pedido,
 * por lo que esas dos operaciones guardan ambos cambios en una misma
 * transacción.</p>
 */
public class EntregaDAO {

    private static final String SQL_INSERTAR =
            "INSERT INTO entregas (id_pedido, id_repartidor, fecha, hora) VALUES (?, ?, ?, ?)";

    private static final String SQL_ACTUALIZAR =
            "UPDATE entregas SET id_pedido = ?, id_repartidor = ?, fecha = ?, hora = ? WHERE id = ?";

    private static final String SQL_ELIMINAR = "DELETE FROM entregas WHERE id = ?";

    private static final String SQL_ELIMINAR_POR_PEDIDO = "DELETE FROM entregas WHERE id_pedido = ?";

    /**
     * Consulta base: cada entrega con los datos de su pedido y de su
     * repartidor. Las columnas del pedido usan los mismos nombres que en
     * {@link PedidoDAO}, para reutilizar su lectura.
     */
    private static final String SQL_SELECCIONAR = """
            SELECT e.id, e.fecha, e.hora,
                   p.id AS id_pedido, p.direccion, p.tipo, p.estado,
                   r.id AS id_repartidor, r.nombre AS nombre_repartidor
            FROM entregas e
            JOIN pedidos p ON p.id = e.id_pedido
            JOIN repartidores r ON r.id = e.id_repartidor
            """;

    /**
     * Listado con filtros opcionales por pedido y por repartidor. Cada filtro
     * se envía dos veces: si su valor es {@code NULL}, la condición se cumple
     * siempre y el filtro no se aplica.
     */
    private static final String SQL_LISTAR = SQL_SELECCIONAR + """
            WHERE (? IS NULL OR e.id_pedido = ?)
              AND (? IS NULL OR e.id_repartidor = ?)
            ORDER BY e.id
            """;

    private static final String SQL_BUSCAR_POR_ID = SQL_SELECCIONAR + "WHERE e.id = ?";

    /**
     * Registra una entrega y guarda el estado con que queda su pedido, dentro
     * de una misma transacción: si alguna de las dos operaciones falla, se
     * deshacen ambas. Así la base de datos nunca muestra una entrega cuyo
     * pedido no figura en reparto, ni al revés.
     *
     * <p>La conexión se cierra en el bloque {@code finally}, tanto si la
     * transacción se confirmó como si se deshizo.</p>
     *
     * @param entrega entrega a guardar; al terminar queda con su ID definitivo
     * @throws PersistenciaException si no fue posible registrarla
     */
    public void create(Entrega entrega) throws PersistenciaException {
        Connection conexion = null;
        try {
            conexion = ConexionDB.conectar();
            conexion.setAutoCommit(false);

            try (PreparedStatement sentencia =
                         conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
                asignarDatos(sentencia, entrega);
                sentencia.executeUpdate();
                entrega.setIdEntrega(ConexionDB.leerIdGenerado(sentencia));
            }
            PedidoDAO.actualizarEstado(conexion, entrega.getPedido());

            conexion.commit();
        } catch (SQLException e) {
            ConexionDB.deshacer(conexion);
            throw new PersistenciaException("No fue posible registrar la entrega del pedido #"
                    + entrega.getPedido().getIdPedido() + ".", e);
        } finally {
            ConexionDB.cerrar(conexion);
        }
    }

    /**
     * Lista todas las entregas guardadas.
     *
     * @return las entregas, ordenadas por ID
     * @throws PersistenciaException si no fue posible consultarlas
     */
    public List<Entrega> readAll() throws PersistenciaException {
        return readAll(null, null);
    }

    /**
     * Lista las entregas de un pedido, de un repartidor, o de ambos.
     *
     * @param idPedido     ID del pedido, o {@code null} para no filtrar por pedido
     * @param idRepartidor ID del repartidor, o {@code null} para no filtrar por repartidor
     * @return las entregas encontradas, ordenadas por ID
     * @throws PersistenciaException si no fue posible consultarlas
     */
    public List<Entrega> readAll(Integer idPedido, Integer idRepartidor) throws PersistenciaException {
        List<Entrega> entregas = new ArrayList<>();
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR)) {
            // setObject con el tipo indicado envía NULL cuando el filtro no se usa.
            sentencia.setObject(1, idPedido, Types.INTEGER);
            sentencia.setObject(2, idPedido, Types.INTEGER);
            sentencia.setObject(3, idRepartidor, Types.INTEGER);
            sentencia.setObject(4, idRepartidor, Types.INTEGER);
            try (ResultSet filas = sentencia.executeQuery()) {
                while (filas.next()) {
                    entregas.add(leerEntrega(filas));
                }
            }
        } catch (SQLException | IllegalArgumentException e) {
            throw new PersistenciaException("No fue posible consultar las entregas en la base de datos.", e);
        }
        return entregas;
    }

    /**
     * Busca una entrega por su ID.
     *
     * @param idEntrega ID de la entrega buscada
     * @return la entrega, o {@code null} si no existe
     * @throws PersistenciaException si no fue posible consultarla
     */
    public Entrega readById(int idEntrega) throws PersistenciaException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR_POR_ID)) {
            sentencia.setInt(1, idEntrega);
            try (ResultSet fila = sentencia.executeQuery()) {
                return fila.next() ? leerEntrega(fila) : null;
            }
        } catch (SQLException | IllegalArgumentException e) {
            throw new PersistenciaException("No fue posible consultar la entrega #" + idEntrega + ".", e);
        }
    }

    /**
     * Actualiza el pedido, el repartidor, la fecha y la hora de una entrega existente.
     *
     * @param entrega entrega con los datos nuevos y el ID del registro a modificar
     * @throws PersistenciaException si no fue posible actualizarla o la entrega ya no existe
     */
    public void update(Entrega entrega) throws PersistenciaException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_ACTUALIZAR)) {
            asignarDatos(sentencia, entrega);
            sentencia.setInt(5, entrega.getIdEntrega());
            ConexionDB.exigirFilaAfectada(sentencia.executeUpdate(), "la entrega #" + entrega.getIdEntrega());
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "No fue posible actualizar la entrega #" + entrega.getIdEntrega() + ".", e);
        }
    }

    /**
     * Elimina una entrega y guarda el estado con que queda su pedido, dentro
     * de una misma transacción: si alguna de las dos operaciones falla, se
     * deshacen ambas.
     *
     * @param entrega entrega a eliminar, con su pedido en el estado que debe guardarse
     * @throws PersistenciaException si no fue posible eliminarla o la entrega ya no existe
     */
    public void delete(Entrega entrega) throws PersistenciaException {
        Connection conexion = null;
        try {
            conexion = ConexionDB.conectar();
            conexion.setAutoCommit(false);

            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_ELIMINAR)) {
                sentencia.setInt(1, entrega.getIdEntrega());
                ConexionDB.exigirFilaAfectada(sentencia.executeUpdate(), "la entrega #" + entrega.getIdEntrega());
            }
            PedidoDAO.actualizarEstado(conexion, entrega.getPedido());

            conexion.commit();
        } catch (SQLException e) {
            ConexionDB.deshacer(conexion);
            throw new PersistenciaException(
                    "No fue posible eliminar la entrega #" + entrega.getIdEntrega() + ".", e);
        } finally {
            ConexionDB.cerrar(conexion);
        }
    }

    /**
     * Elimina todas las entregas de un pedido usando una conexión ya abierta.
     * La usa {@link PedidoDAO#delete(int)}, dentro de la transacción que
     * elimina el pedido. No cierra la conexión: eso le corresponde a quien la abrió.
     *
     * @param conexion conexión con la transacción en curso
     * @param idPedido ID del pedido cuyas entregas se eliminan
     * @return cantidad de entregas eliminadas
     * @throws SQLException si la sentencia falla
     */
    static int deleteByPedido(Connection conexion, int idPedido) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_ELIMINAR_POR_PEDIDO)) {
            sentencia.setInt(1, idPedido);
            return sentencia.executeUpdate();
        }
    }

    /**
     * Asigna el pedido, el repartidor, la fecha y la hora de la entrega a los
     * cuatro primeros parámetros de una sentencia. Lo comparten el
     * {@code INSERT} y el {@code UPDATE}.
     *
     * @param sentencia sentencia preparada
     * @param entrega   entrega de la que se toman los datos
     * @throws SQLException si no es posible asignar algún parámetro
     */
    private static void asignarDatos(PreparedStatement sentencia, Entrega entrega) throws SQLException {
        sentencia.setInt(1, entrega.getPedido().getIdPedido());
        sentencia.setInt(2, entrega.getRepartidor().getIdRepartidor());
        // JDBC 4.2 convierte LocalDate y LocalTime a los tipos DATE y TIME de SQL.
        sentencia.setObject(3, entrega.getFecha());
        sentencia.setObject(4, entrega.getHora());
    }

    /**
     * Construye una entrega a partir de la fila actual del {@link ResultSet}.
     *
     * @param fila resultado posicionado en la fila a leer
     * @return la entrega, con su pedido y su repartidor
     * @throws SQLException si no es posible leer alguna columna
     */
    private static Entrega leerEntrega(ResultSet fila) throws SQLException {
        return new Entrega(
                fila.getInt("id"),
                PedidoDAO.leerPedido(fila),
                new Repartidor(fila.getInt("id_repartidor"), fila.getString("nombre_repartidor")),
                fila.getObject("fecha", LocalDate.class),
                fila.getObject("hora", LocalTime.class));
    }
}
