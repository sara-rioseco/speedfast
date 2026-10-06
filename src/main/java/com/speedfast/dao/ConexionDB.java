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
 * <p>La URL y el usuario tienen valores por defecto, que pueden reemplazarse
 * con las variables de entorno {@code SPEEDFAST_DB_URL} y
 * {@code SPEEDFAST_DB_USER}. La contraseña, en cambio, <strong>no tiene un
 * valor por defecto</strong>: debe configurarse en la variable de entorno
 * {@code SPEEDFAST_DB_PASSWORD} antes de iniciar la aplicación. Así ninguna
 * contraseña, real o de ejemplo, queda escrita en el código ni se sube al
 * repositorio.</p>
 *
 * <p>Desde JDBC 4, el driver de MySQL se registra solo al estar en el
 * classpath (lo agrega la dependencia {@code mysql-connector-j} del
 * {@code pom.xml}), por lo que no es necesario invocar {@code Class.forName()}.</p>
 *
 * <p>Cada operación abre su propia conexión y la cierra al terminar. Una
 * {@link Connection} no debe compartirse entre hilos, y los repartidores
 * registran sus entregas en paralelo.</p>
 */
public final class ConexionDB {

    /** Variable de entorno que debe contener la contraseña de MySQL. */
    public static final String VARIABLE_PASSWORD = "SPEEDFAST_DB_PASSWORD";

    /** Dirección de la base de datos: servidor local, puerto 3306, base speedfast_db. */
    private static final String URL =
            configuracion("SPEEDFAST_DB_URL", "jdbc:mysql://localhost:3306/speedfast_db");

    /** Usuario de MySQL. */
    private static final String USER = configuracion("SPEEDFAST_DB_USER", "root");

    /** Contraseña del usuario de MySQL, o {@code null} si no se configuró. */
    private static final String PASSWORD = System.getenv(VARIABLE_PASSWORD);

    /** Clase de utilidades: no se instancia. */
    private ConexionDB() {
    }

    /**
     * Indica si la contraseña de la base de datos fue configurada. La
     * aplicación lo comprueba al iniciar, para explicar al usuario cómo
     * configurarla en lugar de intentar conectarse sin ella.
     *
     * @return {@code true} si la variable de entorno de la contraseña está definida
     */
    public static boolean estaConfigurada() {
        return PASSWORD != null;
    }

    /**
     * Abre una nueva conexión con la base de datos. Quien la solicita es
     * responsable de cerrarla, idealmente con try-with-resources.
     *
     * @return una conexión abierta
     * @throws SQLException si la contraseña no está configurada, el servidor no
     *                      está disponible, la base de datos no existe o las
     *                      credenciales son incorrectas
     */
    public static Connection conectar() throws SQLException {
        if (!estaConfigurada()) {
            throw new SQLException("No se configuró la contraseña de la base de datos "
                    + "(variable de entorno " + VARIABLE_PASSWORD + ").");
        }
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
     * Comprueba que un {@code UPDATE} o {@code DELETE} haya encontrado el
     * registro buscado. Si no afectó ninguna fila, el registro ya no existe,
     * por ejemplo porque se eliminó desde otra ventana.
     *
     * @param filasAfectadas resultado de {@code executeUpdate()}
     * @param registro       descripción del registro, por ejemplo "el pedido #5"
     * @throws SQLException si la sentencia no afectó ninguna fila
     */
    static void exigirFilaAfectada(int filasAfectadas, String registro) throws SQLException {
        if (filasAfectadas == 0) {
            throw new SQLException("No existe " + registro + " en la base de datos.");
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
