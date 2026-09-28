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
 * Acceso a datos de la tabla {@code pedido}. Guarda pedidos nuevos, actualiza
 * su estado a medida que avanzan las entregas y los lista para la interfaz.
 *
 * <p>Todas las sentencias usan {@link PreparedStatement}: los valores viajan
 * como parámetros y no concatenados en el SQL, lo que evita la inyección SQL y
 * errores con caracteres especiales en las direcciones.</p>
 */
public class PedidoDAO {

    private static final String SQL_INSERTAR =
            "INSERT INTO pedido (direccion, tipo, estado) VALUES (?, ?, ?)";

    private static final String SQL_ACTUALIZAR_ESTADO =
            "UPDATE pedido SET estado = ? WHERE id = ?";

    /**
     * Lista los pedidos junto al repartidor de su última entrega, si la tiene.
     * Un pedido puede tener varias entregas (una por intento), por lo que se
     * une solo con la más reciente para obtener una fila por pedido.
     */
    private static final String SQL_LISTAR = """
            SELECT p.id, p.direccion, p.tipo, p.estado,
                   r.id AS id_repartidor, r.nombre AS nombre_repartidor
            FROM pedido p
            LEFT JOIN entrega e
                   ON e.id_pedido = p.id
                  AND e.id = (SELECT MAX(u.id) FROM entrega u WHERE u.id_pedido = p.id)
            LEFT JOIN repartidor r ON r.id = e.id_repartidor
            ORDER BY p.id
            """;

    /**
     * Inserta un pedido nuevo y le asigna el ID generado por la base de datos.
     *
     * @param pedido pedido a guardar; al terminar queda con su ID definitivo
     * @throws PersistenciaException si no fue posible guardarlo
     */
    public void guardar(Pedido pedido) throws PersistenciaException {
        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement sentencia =
                     conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, pedido.getDireccionEntrega());
            sentencia.setString(2, pedido.getTipo().name());
            sentencia.setString(3, pedido.getEstado().name());
            sentencia.executeUpdate();
            pedido.setIdPedido(ConexionBD.leerIdGenerado(sentencia));
        } catch (SQLException e) {
            throw new PersistenciaException("No fue posible guardar el pedido en la base de datos.", e);
        }
    }

    /**
     * Lista todos los pedidos guardados. Cada fila se convierte en la subclase
     * que corresponde a su tipo, con el estado registrado y el repartidor de
     * su última entrega.
     *
     * @return los pedidos, ordenados por ID
     * @throws PersistenciaException si no fue posible consultarlos, o si una
     *                               fila tiene un tipo o estado no reconocido
     */
    public List<Pedido> listarTodos() throws PersistenciaException {
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR);
             ResultSet filas = sentencia.executeQuery()) {
            while (filas.next()) {
                pedidos.add(leerPedido(filas));
            }
        } catch (SQLException | IllegalArgumentException e) {
            throw new PersistenciaException("No fue posible consultar los pedidos en la base de datos.", e);
        }
        return pedidos;
    }

    /**
     * Guarda el estado actual de un pedido, abriendo su propia conexión.
     *
     * @param pedido pedido cuyo estado cambió
     * @throws PersistenciaException si no fue posible actualizarlo
     */
    public void actualizarEstado(Pedido pedido) throws PersistenciaException {
        try (Connection conexion = ConexionBD.conectar()) {
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
    void actualizarEstado(Connection conexion, Pedido pedido) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_ACTUALIZAR_ESTADO)) {
            sentencia.setString(1, pedido.getEstado().name());
            sentencia.setInt(2, pedido.getIdPedido());
            if (sentencia.executeUpdate() == 0) {
                throw new SQLException("No existe el pedido #" + pedido.getIdPedido() + " en la base de datos.");
            }
        }
    }

    /**
     * Construye un pedido a partir de la fila actual del {@link ResultSet}.
     *
     * @param fila resultado posicionado en la fila a leer
     * @return el pedido reconstruido
     * @throws SQLException si no es posible leer alguna columna
     */
    private static Pedido leerPedido(ResultSet fila) throws SQLException {
        TipoPedido tipo = TipoPedido.valueOf(fila.getString("tipo").trim().toUpperCase());
        EstadoPedido estado = EstadoPedido.valueOf(fila.getString("estado").trim().toUpperCase());

        Pedido pedido = tipo.crearPedido(fila.getInt("id"), fila.getString("direccion"));
        pedido.restablecer(estado, leerRepartidor(fila, estado));
        return pedido;
    }

    /**
     * Obtiene el repartidor de la última entrega del pedido. Un pedido
     * pendiente no tiene repartidor, aunque tenga registrado un intento de
     * entrega anterior que quedó interrumpido.
     *
     * @param fila   resultado posicionado en la fila a leer
     * @param estado estado del pedido de esa fila
     * @return el repartidor, o {@code null} si el pedido no tiene uno asignado
     * @throws SQLException si no es posible leer alguna columna
     */
    private static Repartidor leerRepartidor(ResultSet fila, EstadoPedido estado) throws SQLException {
        int idRepartidor = fila.getInt("id_repartidor");
        if (fila.wasNull() || estado == EstadoPedido.PENDIENTE) {
            return null;
        }
        return new Repartidor(idRepartidor, fila.getString("nombre_repartidor"));
    }
}
