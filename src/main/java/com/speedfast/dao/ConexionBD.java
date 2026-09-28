package com.speedfast.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestiona la conexión JDBC con la base de datos MySQL {@code speedfast_db}
 * mediante {@link DriverManager}.
 *
 * <p>Los parámetros de conexión tienen valores por defecto, y cada uno puede
 * reemplazarse con una variable de entorno ({@code SPEEDFAST_DB_URL},
 * {@code SPEEDFAST_DB_USER} y {@code SPEEDFAST_DB_PASSWORD}). Así la
 * contraseña real no necesita escribirse en el código ni subirse al
 * repositorio: basta con definir la variable en la configuración de ejecución
 * de IntelliJ o en la terminal.</p>
 *
 * <p>Desde JDBC 4, el driver de MySQL se registra solo al estar en el
 * classpath (lo agrega la dependencia {@code mysql-connector-j} del
 * {@code pom.xml}), por lo que no es necesario invocar {@code Class.forName()}.</p>
 *
 * <p>Cada operación abre su propia conexión y la cierra al terminar. Una
 * {@link Connection} no debe compartirse entre hilos, y los repartidores
 * registran sus entregas en paralelo.</p>
 */
public final class ConexionBD {

    /** Dirección de la base de datos: servidor local, puerto 3306, base speedfast_db. */
    private static final String URL =
            configuracion("SPEEDFAST_DB_URL", "jdbc:mysql://localhost:3306/speedfast_db");

    /** Usuario de MySQL. */
    private static final String USER = configuracion("SPEEDFAST_DB_USER", "root");

    /** Contraseña del usuario de MySQL. */
    private static final String PASSWORD = configuracion("SPEEDFAST_DB_PASSWORD", "tu_contraseña");

    /** Clase de utilidades: no se instancia. */
    private ConexionBD() {
    }

    /**
     * Abre una nueva conexión con la base de datos. Quien la solicita es
     * responsable de cerrarla, idealmente con try-with-resources.
     *
     * @return una conexión abierta
     * @throws SQLException si el servidor no está disponible, la base de datos
     *                      no existe o las credenciales son incorrectas
     */
    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** @return la URL de conexión en uso, útil para informar errores de conexión */
    public static String getUrl() {
        return URL;
    }

    /**
     * Deshace la transacción en curso de una conexión. Se usa en el bloque
     * {@code catch} cuando una de las operaciones de la transacción falló. Si
     * el propio {@code rollback()} falla, se informa por consola sin ocultar
     * el error original, que es el que debe propagarse.
     *
     * @param conexion conexión con la transacción en curso, o {@code null} si no llegó a abrirse
     */
    public static void deshacer(Connection conexion) {
        if (conexion == null) {
            return;
        }
        try {
            conexion.rollback();
        } catch (SQLException e) {
            System.out.println("[BD] No fue posible deshacer la transacción: " + e.getMessage());
        }
    }

    /**
     * Cierra una conexión. Se usa en el bloque {@code finally}, de modo que la
     * conexión se libere tanto si la operación terminó bien como si falló.
     *
     * @param conexion conexión a cerrar, o {@code null} si no llegó a abrirse
     */
    public static void cerrar(Connection conexion) {
        if (conexion == null) {
            return;
        }
        try {
            conexion.close();
        } catch (SQLException e) {
            System.out.println("[BD] No fue posible cerrar la conexión: " + e.getMessage());
        }
    }

    /**
     * Obtiene el ID que la base de datos generó ({@code AUTO_INCREMENT}) en la
     * última inserción de una sentencia preparada con
     * {@link Statement#RETURN_GENERATED_KEYS}.
     *
     * @param sentencia sentencia que acaba de ejecutar un {@code INSERT}
     * @return el ID generado
     * @throws SQLException si la base de datos no devolvió ningún ID
     */
    static int leerIdGenerado(Statement sentencia) throws SQLException {
        try (ResultSet claves = sentencia.getGeneratedKeys()) {
            if (claves.next()) {
                return claves.getInt(1);
            }
            throw new SQLException("La base de datos no devolvió el ID generado.");
        }
    }

    /**
     * Lee un parámetro de conexión desde una variable de entorno.
     *
     * @param variable        nombre de la variable de entorno
     * @param valorPorDefecto valor que se usa si la variable no está definida
     * @return el valor de la variable, o el valor por defecto
     */
    private static String configuracion(String variable, String valorPorDefecto) {
        String valor = System.getenv(variable);
        return valor == null ? valorPorDefecto : valor;
    }
}
