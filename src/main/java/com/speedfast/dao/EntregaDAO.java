package com.speedfast.dao;

import com.speedfast.exception.PersistenciaException;
import com.speedfast.model.Entrega;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Acceso a datos de la tabla {@code entrega}, que relaciona cada pedido con el
 * repartidor que lo lleva, junto a la fecha y hora en que salió a reparto.
 */
public class EntregaDAO {

    private static final String SQL_INSERTAR =
            "INSERT INTO entrega (id_pedido, id_repartidor, fecha, hora) VALUES (?, ?, ?, ?)";

    /** Se reutiliza para actualizar el estado del pedido dentro de la misma transacción. */
    private final PedidoDAO pedidoDAO = new PedidoDAO();

    /**
     * Registra una entrega y guarda el estado con que queda el pedido (en
     * reparto), dentro de una misma transacción: si alguna de las dos
     * operaciones falla, se deshacen ambas. Así la base de datos nunca muestra
     * una entrega cuyo pedido no figura en reparto, ni al revés.
     *
     * <p>La conexión se cierra en el bloque {@code finally}, tanto si la
     * transacción se confirmó como si se deshizo.</p>
     *
     * @param entrega entrega a guardar; al terminar queda con su ID definitivo
     * @throws PersistenciaException si no fue posible registrarla
     */
    public void guardar(Entrega entrega) throws PersistenciaException {
        Connection conexion = null;
        try {
            conexion = ConexionBD.conectar();
            conexion.setAutoCommit(false);

            insertar(conexion, entrega);
            pedidoDAO.actualizarEstado(conexion, entrega.getPedido());

            conexion.commit();
        } catch (SQLException e) {
            ConexionBD.deshacer(conexion);
            throw new PersistenciaException("No fue posible registrar la entrega del pedido #"
                    + entrega.getPedido().getIdPedido() + ".", e);
        } finally {
            ConexionBD.cerrar(conexion);
        }
    }

    /**
     * Inserta la fila de la entrega usando la conexión de la transacción.
     *
     * @param conexion conexión con la transacción en curso
     * @param entrega  entrega a insertar
     * @throws SQLException si la sentencia falla, por ejemplo si el pedido o
     *                      el repartidor no existen (claves foráneas)
     */
    private static void insertar(Connection conexion, Entrega entrega) throws SQLException {
        try (PreparedStatement sentencia =
                     conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setInt(1, entrega.getPedido().getIdPedido());
            sentencia.setInt(2, entrega.getRepartidor().getIdRepartidor());
            // JDBC 4.2 convierte LocalDate y LocalTime a los tipos DATE y TIME de SQL.
            sentencia.setObject(3, entrega.getFecha());
            sentencia.setObject(4, entrega.getHora());
            sentencia.executeUpdate();
            entrega.setIdEntrega(ConexionBD.leerIdGenerado(sentencia));
        }
    }
}
