package com.speedfast.model;

/**
 * Tipos de servicio que ofrece SpeedFast. El nombre de cada constante es el
 * valor que se guarda en la columna {@code tipo} de la tabla {@code pedido}.
 *
 * <p>La tabla solo guarda la dirección, el tipo y el estado de cada pedido. Por
 * eso los demás datos que exige cada subclase (distancia, peso, tienda, etc.)
 * se completan con valores estándar, tanto al registrar un pedido desde el
 * formulario como al reconstruirlo desde la base de datos. Los valores se
 * eligieron de modo que cualquier repartidor con el perfil estándar pueda
 * atender cualquier pedido.</p>
 */
public enum TipoPedido {

    /** Pedido de comida de restaurante. */
    COMIDA("Pedido de Comida"),

    /** Encomienda: documentos o paquetes. */
    ENCOMIENDA("Pedido de Encomienda"),

    /** Compra express en supermercado o farmacia. */
    EXPRESS("Pedido Express");

    /** Distancia estándar hasta el destino, en kilómetros. */
    private static final float DISTANCIA_KM = 3.0f;

    /** Peso estándar de una encomienda, en kilogramos. */
    private static final float PESO_ENCOMIENDA_KG = 5.0f;

    /** Radio de cobertura estándar de una compra express, en kilómetros. */
    private static final float RADIO_EXPRESS_KM = 3.0f;

    /** Texto descriptivo que se muestra en consola y en la interfaz. */
    private final String descripcion;

    /**
     * Crea un tipo de pedido con su descripción.
     *
     * @param descripcion texto legible del tipo
     */
    TipoPedido(String descripcion) {
        this.descripcion = descripcion;
    }

    /** @return el texto legible del tipo */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Crea un pedido nuevo de este tipo, aún sin identificador: el ID lo
     * asigna la base de datos al guardarlo.
     *
     * @param direccion dirección de entrega
     * @return el pedido creado, en estado pendiente
     */
    public Pedido crearPedido(String direccion) {
        return crearPedido(0, direccion);
    }

    /**
     * Crea un pedido de este tipo con los valores estándar de su subclase.
     *
     * @param idPedido  identificador del pedido
     * @param direccion dirección de entrega
     * @return el pedido creado, en estado pendiente
     */
    public Pedido crearPedido(int idPedido, String direccion) {
        return switch (this) {
            case COMIDA -> new PedidoComida(idPedido, direccion, DISTANCIA_KM, "Restaurante asociado", 1);
            case ENCOMIENDA -> new PedidoEncomienda(idPedido, direccion, DISTANCIA_KM, PESO_ENCOMIENDA_KG, "Caja");
            case EXPRESS -> new PedidoExpress(idPedido, direccion, DISTANCIA_KM, "Tienda asociada", RADIO_EXPRESS_KM);
        };
    }

    /**
     * Se sobrescribe para que el {@code JComboBox} del formulario muestre la
     * descripción del tipo en lugar del nombre de la constante.
     *
     * @return la descripción del tipo
     */
    @Override
    public String toString() {
        return descripcion;
    }
}
