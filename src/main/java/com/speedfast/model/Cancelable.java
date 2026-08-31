package com.speedfast.model;

/**
 * Contrato de todo elemento que puede ser cancelado antes de completarse.
 * Separa la operación de cancelación del resto de la lógica del pedido.
 */
public interface Cancelable {

    /**
     * Cancela el elemento si su estado actual lo permite.
     *
     * @return el resultado de la operación, listo para imprimirse en consola
     */
    String cancelar();
}
