package com.speedfast.model;

/**
 * Pedido genérico de SpeedFast y clase base de la jerarquía.
 * Define {@code asignarRepartidor()} en tres versiones sobrecargadas que las
 * subclases sobrescriben con las reglas de cada tipo de servicio.
 */
public class Pedido {

    /** Identificador único del pedido. */
    private int idPedido;

    /** Dirección en la que debe entregarse el pedido. */
    private String direccionEntrega;

    /** Tipo de servicio: comida, encomienda o compra express. */
    private String tipoPedido;

    /**
     * Crea un pedido con todos sus atributos.
     *
     * @param idPedido         identificador único del pedido
     * @param direccionEntrega dirección de entrega
     * @param tipoPedido       tipo de servicio asociado
     */
    public Pedido(int idPedido, String direccionEntrega, String tipoPedido) {
        this.idPedido = idPedido;
        this.direccionEntrega = direccionEntrega;
        this.tipoPedido = tipoPedido;
    }

    /** @return el identificador del pedido */
    public int getIdPedido() {
        return idPedido;
    }

    /** @param idPedido nuevo identificador del pedido */
    public void setIdPedido(int idPedido) {
        this.idPedido = idPedido;
    }

    /** @return la dirección de entrega */
    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    /** @param direccionEntrega nueva dirección de entrega */
    public void setDireccionEntrega(String direccionEntrega) {
        this.direccionEntrega = direccionEntrega;
    }

    /** @return el tipo de pedido */
    public String getTipoPedido() {
        return tipoPedido;
    }

    /** @param tipoPedido nuevo tipo de pedido */
    public void setTipoPedido(String tipoPedido) {
        this.tipoPedido = tipoPedido;
    }

    /**
     * Versión genérica: busca un repartidor sin designar aún a ninguno.
     *
     * @return mensaje de asignación para imprimir en consola
     */
    public String asignarRepartidor() {
        return encabezado()
                + String.format("Asignando repartidor...%n")
                + String.format("   Buscando un repartidor disponible en la zona de entrega...%n")
                + String.format("   Aún no se ha designado un repartidor específico.");
    }

    /**
     * Sobrecarga que recibe el nombre del repartidor asignado.
     *
     * @param nombreRepartidor nombre del repartidor que tomará el pedido
     * @return mensaje de asignación para imprimir en consola
     */
    public String asignarRepartidor(String nombreRepartidor) {
        return encabezado()
                + String.format("Asignando repartidor...%n")
                + String.format("   Verificando disponibilidad general... OK%n")
                + String.format("   Pedido asignado a %s", nombreRepartidor);
    }

    /**
     * Sobrecarga que recibe el repartidor completo para validar sus datos.
     *
     * @param repartidor repartidor candidato a tomar el pedido
     * @return mensaje de asignación para imprimir en consola
     */
    public String asignarRepartidor(Repartidor repartidor) {
        return encabezado()
                + String.format("Asignando repartidor...%n")
                + String.format("   Verificando disponibilidad general... OK%n")
                + String.format("   Pedido asignado a %s (%s)",
                        repartidor.getNombreCompleto(), repartidor.getTipoVehiculo());
    }

    /**
     * Encabezado común reutilizado por las subclases en cada sobrescritura.
     *
     * @return tipo de pedido con su identificador y la dirección de entrega
     */
    protected String encabezado() {
        return String.format("[%s] N° %d%n", tipoPedido, idPedido)
                + String.format("Dirección de entrega: %s%n", direccionEntrega);
    }

    /** @return representación textual breve del pedido */
    @Override
    public String toString() {
        return String.format("%s [id=%d, dirección=%s]", tipoPedido, idPedido, direccionEntrega);
    }
}
