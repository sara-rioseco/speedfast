package com.speedfast.exception;

/**
 * Excepción que lanzan los DAO cuando una operación con la base de datos no
 * puede completarse: por ejemplo, si el servidor MySQL no está disponible, si
 * las credenciales son incorrectas o si una sentencia SQL es rechazada.
 *
 * <p>Envuelve la {@link java.sql.SQLException} original para que las capas
 * superiores (controlador y ventanas) informen el problema en términos del
 * sistema, sin depender de las clases de JDBC. La causa se conserva para no
 * perder el detalle técnico.</p>
 */
public class PersistenciaException extends Exception {

    /**
     * Crea la excepción con el motivo del error y la excepción que lo originó.
     *
     * @param mensaje descripción de la operación que falló
     * @param causa   excepción original, normalmente una {@code SQLException}
     */
    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    /**
     * @return el mensaje de la excepción junto al detalle informado por la
     *         base de datos, listo para mostrarse al usuario
     */
    public String getMensajeConDetalle() {
        if (getCause() == null || getCause().getMessage() == null) {
            return getMessage();
        }
        return getMessage() + "\nDetalle: " + getCause().getMessage();
    }
}
