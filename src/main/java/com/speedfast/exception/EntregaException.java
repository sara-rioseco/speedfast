package com.speedfast.exception;

/**
 * Excepción de dominio que se lanza cuando un pedido no puede ser entregado:
 * por ejemplo, si fue cancelado o si no tiene un repartidor asignado.
 * Permite que un repartidor continúe con el resto de sus pedidos en lugar de
 * interrumpir todo su recorrido.
 */
public class EntregaException extends Exception {

    /**
     * Crea la excepción con el motivo por el que falló la entrega.
     *
     * @param mensaje descripción del problema detectado
     */
    public EntregaException(String mensaje) {
        super(mensaje);
    }
}
