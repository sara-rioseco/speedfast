package com.speedfast.model;

/**
 * Estados por los que puede pasar un pedido dentro del sistema.
 * Centraliza los valores válidos y evita usar textos sueltos en el código.
 *
 * <p>La columna {@code estado} de la tabla {@code pedidos} es un {@code ENUM}
 * que admite solo {@link #registrables() tres de estos estados}. {@code ASIGNADO}
 * y {@code CANCELADO} existen solo en memoria.</p>
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

    /**
     * Estados que pueden guardarse en la base de datos, y por lo tanto los
     * que ofrecen los combos de la interfaz.
     *
     * @return PENDIENTE, EN_REPARTO y ENTREGADO
     */
    public static EstadoPedido[] registrables() {
        return new EstadoPedido[]{PENDIENTE, EN_REPARTO, ENTREGADO};
    }
}
