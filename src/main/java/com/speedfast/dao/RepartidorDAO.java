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
 * Acceso a datos de la tabla {@code repartidor}. Guarda repartidores nuevos y
 * los lista, tanto para iniciar la operación como para mostrarlos en la
 * interfaz.
 */
public class RepartidorDAO {

    private static final String SQL_INSERTAR = "INSERT INTO repartidor (nombre) VALUES (?)";

    private static final String SQL_LISTAR = "SELECT id, nombre FROM repartidor ORDER BY id";

    /**
     * Inserta un repartidor nuevo y le asigna el ID generado por la base de
     * datos. La columna {@code nombre} guarda el nombre completo.
     *
     * @param repartidor repartidor a guardar; al terminar queda con su ID definitivo
     * @throws PersistenciaException si no fue posible guardarlo
     */
    public void guardar(Repartidor repartidor) throws PersistenciaException {
        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement sentencia =
                     conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, repartidor.getNombreCompleto());
            sentencia.executeUpdate();
            repartidor.setIdRepartidor(ConexionBD.leerIdGenerado(sentencia));
        } catch (SQLException e) {
            throw new PersistenciaException("No fue posible guardar el repartidor en la base de datos.", e);
        }
    }

    /**
     * Lista todos los repartidores guardados. La tabla solo guarda el ID y el
     * nombre, por lo que cada repartidor recibe el perfil estándar.
     *
     * @return los repartidores, ordenados por ID
     * @throws PersistenciaException si no fue posible consultarlos
     */
    public List<Repartidor> listarTodos() throws PersistenciaException {
        List<Repartidor> repartidores = new ArrayList<>();
        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR);
             ResultSet filas = sentencia.executeQuery()) {
            while (filas.next()) {
                repartidores.add(new Repartidor(filas.getInt("id"), filas.getString("nombre")));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No fue posible consultar los repartidores en la base de datos.", e);
        }
        return repartidores;
    }
}
