package com.speedfast.exception;

/**
 * Excepción que lanza el controlador cuando una operación contradice una regla
 * del negocio: por ejemplo, registrar la entrega de un pedido que no está
 * pendiente, eliminar un repartidor que tiene entregas registradas o modificar
 * datos mientras una ronda de entregas está en curso.
 *
 * <p>A diferencia de {@link PersistenciaException}, no indica que la base de
 * datos haya fallado, sino que la operación no corresponde. Por eso la interfaz
 * la informa como advertencia y no como error.</p>
 */
public class OperacionNoPermitidaException extends Exception {

    /**
     * Crea la excepción con el motivo por el que no se permite la operación.
     *
     * @param mensaje motivo, listo para mostrarse al usuario
     */
    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
