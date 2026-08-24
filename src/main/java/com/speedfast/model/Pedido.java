package com.speedfast.model;

/**
 * Pedido genérico de SpeedFast y clase base abstracta de la jerarquía.
 * Reúne los atributos y el comportamiento comunes a todo pedido, y delega en
 * cada subclase el cálculo de su tiempo de entrega mediante el método abstracto
 * {@link #calcularTiempoEntrega()}.
 */
public abstract class Pedido {

    /** Identificador único del pedido. */
    private int idPedido;

    /** Dirección en la que debe entregarse el pedido. */
    private String direccionEntrega;

    /** Distancia a recorrer hasta la dirección de entrega, en kilómetros. */
    private float distanciaKm;

    /** Tipo de servicio: comida, encomienda o compra express. */
    private String tipoPedido;

    /**
     * Crea un pedido con todos sus atributos comunes.
     *
     * @param idPedido         identificador único del pedido
     * @param direccionEntrega dirección de entrega
     * @param distanciaKm      distancia hasta la entrega, en kilómetros
     * @param tipoPedido       tipo de servicio asociado
     */
    public Pedido(int idPedido, String direccionEntrega, float distanciaKm, String tipoPedido) {
        this.idPedido = idPedido;
        this.direccionEntrega = direccionEntrega;
        this.distanciaKm = distanciaKm;
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

    /** @return la distancia hasta la entrega, en kilómetros */
    public float getDistanciaKm() {
        return distanciaKm;
    }

    /** @param distanciaKm nueva distancia hasta la entrega, en kilómetros */
    public void setDistanciaKm(float distanciaKm) {
        this.distanciaKm = distanciaKm;
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
     * Calcula el tiempo estimado de entrega del pedido.
     * Cada subclase aplica su propia fórmula según el tipo de servicio.
     *
     * @return el tiempo estimado de entrega, en minutos
     */
    public abstract int calcularTiempoEntrega();

    /**
     * Imprime en consola los datos básicos del pedido junto con su tiempo
     * estimado de entrega, obtenido desde la subclase correspondiente.
     */
    public void mostrarResumen() {
        System.out.print(encabezado());
        System.out.printf("Distancia: %.1f km%n", distanciaKm);
        System.out.printf("Tiempo estimado de entrega: %d minutos%n", calcularTiempoEntrega());
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
        return String.format("%s [id=%d, dirección=%s, %.1f km]",
                tipoPedido, idPedido, direccionEntrega, distanciaKm);
    }
}
