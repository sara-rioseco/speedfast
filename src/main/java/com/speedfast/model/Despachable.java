package com.speedfast.model;

/**
 * Contrato de todo elemento que puede ser despachado hacia su destino.
 * Separa la operación de despacho del resto de la lógica del pedido.
 */
public interface Despachable {

    /**
     * Despacha el elemento hacia su destino.
     *
     * @return el resultado de la operación, listo para imprimirse en consola
     */
    String despachar();
}
