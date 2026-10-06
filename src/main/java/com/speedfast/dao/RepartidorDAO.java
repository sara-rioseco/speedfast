package com.speedfast.dao;

import com.speedfast.exception.PersistenciaException;
import com.speedfast.model.Repartidor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la tabla {@code repartidores}. Ofrece las operaciones CRUD
 * ({@link #create}, {@link #readAll}, {@link #update} y {@link #delete}) y la
 * búsqueda por ID.
 *
 * <p>La tabla solo guarda el ID y el nombre, por lo que cada repartidor leído
 * recibe el perfil estándar definido en {@link Repartidor}.</p>
 */
public class RepartidorDAO {

    private static final String SQL_INSERTAR = "INSERT INTO repartidores (nombre) VALUES (?)";

    private static final String SQL_LISTAR = "SELECT id, nombre FROM repartidores ORDER BY id";

    private static final String SQL_BUSCAR_POR_ID = "SELECT id, nombre FROM repartidores WHERE id = ?";

    private static final String SQL_ACTUALIZAR = "UPDATE repartidores SET nombre = ? WHERE id = ?";

    private static final String SQL_ELIMINAR = "DELETE FROM repartidores WHERE id = ?";

    /**
     * Inserta un repartidor nuevo y le asigna el ID generado por la base de
     * datos. La columna {@code nombre} guarda el nombre completo.
     *
     * @param repartidor repartidor a guardar; al terminar queda con su ID definitivo
     * @throws PersistenciaException si no fue posible guardarlo
     */
    public void create(Repartidor repartidor) throws PersistenciaException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement sentencia =
                     conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, repartidor.getNombreCompleto());
            sentencia.executeUpdate();
            repartidor.setIdRepartidor(ConexionDB.leerIdGenerado(sentencia));
        } catch (SQLException e) {
            throw new PersistenciaException("No fue posible registrar el repartidor en la base de datos.", e);
        }
    }

    /**
     * Lista todos los repartidores guardados.
     *
     * @return los repartidores, ordenados por ID
     * @throws PersistenciaException si no fue posible consultarlos
     */
    public List<Repartidor> readAll() throws PersistenciaException {
        List<Repartidor> repartidores = new ArrayList<>();
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR);
             ResultSet filas = sentencia.executeQuery()) {
            while (filas.next()) {
                repartidores.add(leerRepartidor(filas));
            }
        } catch (SQLException | IllegalArgumentException e) {
            throw new PersistenciaException("No fue posible consultar los repartidores en la base de datos.", e);
        }
        return repartidores;
    }

    /**
     * Busca un repartidor por su ID.
     *
     * @param idRepartidor ID del repartidor buscado
     * @return el repartidor, o {@code null} si no existe
     * @throws PersistenciaException si no fue posible consultarlo
     */
    public Repartidor readById(int idRepartidor) throws PersistenciaException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR_POR_ID)) {
            sentencia.setInt(1, idRepartidor);
            try (ResultSet fila = sentencia.executeQuery()) {
                return fila.next() ? leerRepartidor(fila) : null;
            }
        } catch (SQLException | IllegalArgumentException e) {
            throw new PersistenciaException("No fue posible consultar el repartidor #" + idRepartidor + ".", e);
        }
    }

    /**
     * Actualiza el nombre de un repartidor existente.
     *
     * @param repartidor repartidor con el nombre nuevo y el ID del registro a modificar
     * @throws PersistenciaException si no fue posible actualizarlo o el repartidor ya no existe
     */
    public void update(Repartidor repartidor) throws PersistenciaException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_ACTUALIZAR)) {
            sentencia.setString(1, repartidor.getNombreCompleto());
            sentencia.setInt(2, repartidor.getIdRepartidor());
            ConexionDB.exigirFilaAfectada(sentencia.executeUpdate(),
                    "el repartidor #" + repartidor.getIdRepartidor());
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "No fue posible actualizar el repartidor #" + repartidor.getIdRepartidor() + ".", e);
        }
    }

    /**
     * Elimina un repartidor. La base de datos rechaza la operación si el
     * repartidor tiene entregas registradas, porque la clave foránea de la
     * tabla {@code entregas} lo referencia.
     *
     * @param idRepartidor ID del repartidor a eliminar
     * @throws PersistenciaException si no fue posible eliminarlo o el repartidor ya no existe
     */
    public void delete(int idRepartidor) throws PersistenciaException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_ELIMINAR)) {
            sentencia.setInt(1, idRepartidor);
            ConexionDB.exigirFilaAfectada(sentencia.executeUpdate(), "el repartidor #" + idRepartidor);
        } catch (SQLException e) {
            throw new PersistenciaException("No fue posible eliminar el repartidor #" + idRepartidor + ".", e);
        }
    }

    /**
     * Construye un repartidor a partir de la fila actual del {@link ResultSet}.
     *
     * @param fila resultado posicionado en la fila a leer
     * @return el repartidor, con el perfil estándar
     * @throws SQLException si no es posible leer alguna columna
     */
    private static Repartidor leerRepartidor(ResultSet fila) throws SQLException {
        return new Repartidor(fila.getInt("id"), fila.getString("nombre"));
    }
}
