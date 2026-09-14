package com.speedfast.model;

/**
 * Estados por los que puede pasar un pedido dentro del sistema.
 * Centraliza los valores válidos y evita usar textos sueltos en el código.
 */
public enum EstadoPedido {

    /** El pedido fue registrado, pero todavía no tiene repartidor. */
    PENDIENTE("pendiente de asignación"),

    /** El pedido ya cuenta con un repartidor que cumple sus requisitos. */
    ASIGNADO("repartidor asignado"),

    /** El pedido fue retirado de la zona de carga y va en camino. */
    EN_REPARTO("en reparto"),

    /** El pedido llegó a su destino. */
    ENTREGADO("entregado"),

    /** El pedido fue cancelado antes de ser despachado. */
    CANCELADO("cancelado");

    /** Texto descriptivo que se muestra en consola. */
    private final String descripcion;

    /**
     * Crea un estado con su descripción.
     *
     * @param descripcion texto legible del estado
     */
    EstadoPedido(String descripcion) {
        this.descripcion = descripcion;
    }

    /** @return el texto legible del estado */
    public String getDescripcion() {
        return descripcion;
    }
}
