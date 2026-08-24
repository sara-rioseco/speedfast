package com.speedfast.model;

/**
 * Pedido de comida de restaurante. Requiere un repartidor con mochila térmica
 * para mantener la temperatura de los alimentos.
 */
public class PedidoComida extends Pedido {

    /** Tipo de servicio con el que se identifica esta subclase. */
    private static final String TIPO = "Pedido de Comida";

    /** Minutos de preparación considerados como base. */
    private static final int TIEMPO_BASE = 15;

    /** Minutos adicionales por cada kilómetro recorrido. */
    private static final int MINUTOS_POR_KM = 2;

    /** Restaurante que prepara el pedido. */
    private String restaurante;

    /** Cantidad de platos incluidos en el pedido. */
    private int cantidadPlatos;

    /**
     * Crea un pedido de comida.
     *
     * @param idPedido         identificador único del pedido
     * @param direccionEntrega dirección de entrega
     * @param distanciaKm      distancia hasta la entrega, en kilómetros
     * @param restaurante      restaurante que prepara el pedido
     * @param cantidadPlatos   cantidad de platos incluidos
     */
    public PedidoComida(int idPedido, String direccionEntrega, float distanciaKm,
                        String restaurante, int cantidadPlatos) {
        super(idPedido, direccionEntrega, distanciaKm, TIPO);
        this.restaurante = restaurante;
        this.cantidadPlatos = cantidadPlatos;
    }

    /** @return el restaurante que prepara el pedido */
    public String getRestaurante() {
        return restaurante;
    }

    /** @param restaurante nuevo restaurante que prepara el pedido */
    public void setRestaurante(String restaurante) {
        this.restaurante = restaurante;
    }

    /** @return la cantidad de platos incluidos */
    public int getCantidadPlatos() {
        return cantidadPlatos;
    }

    /** @param cantidadPlatos nueva cantidad de platos incluidos */
    public void setCantidadPlatos(int cantidadPlatos) {
        this.cantidadPlatos = cantidadPlatos;
    }

    /**
     * Implementación del cálculo para pedidos de comida:
     * 15 minutos base más 2 minutos por cada kilómetro de distancia.
     *
     * @return el tiempo estimado de entrega, en minutos
     */
    @Override
    public int calcularTiempoEntrega() {
        return Math.round(TIEMPO_BASE + MINUTOS_POR_KM * getDistanciaKm());
    }

    /**
     * Sobrescritura: busca un repartidor que disponga de mochila térmica.
     *
     * @return mensaje de asignación para imprimir en consola
     */
    @Override
    public String asignarRepartidor() {
        return encabezado()
                + String.format("Asignando repartidor...%n")
                + String.format("   Retiro en: %s (%d platos)%n", restaurante, cantidadPlatos)
                + String.format("   Se requiere un repartidor con mochila térmica.%n")
                + String.format("   Aún no se ha designado un repartidor específico.");
    }

    /**
     * Sobrecarga sobrescrita: asigna al repartidor indicado por su nombre.
     *
     * @param nombreRepartidor nombre del repartidor que tomará el pedido
     * @return mensaje de asignación para imprimir en consola
     */
    @Override
    public String asignarRepartidor(String nombreRepartidor) {
        return encabezado()
                + String.format("Asignando repartidor...%n")
                + String.format("   Retiro en: %s (%d platos)%n", restaurante, cantidadPlatos)
                + String.format("   Verificando mochila térmica... OK%n")
                + String.format("   Pedido asignado a %s", nombreRepartidor);
    }

    /**
     * Sobrecarga sobrescrita: valida la mochila térmica del repartidor recibido
     * y solo confirma la asignación si cumple el requisito.
     *
     * @param repartidor repartidor candidato a tomar el pedido
     * @return mensaje de asignación para imprimir en consola
     */
    @Override
    public String asignarRepartidor(Repartidor repartidor) {
        String detalle = encabezado()
                + String.format("Asignando repartidor...%n")
                + String.format("   Retiro en: %s (%d platos)%n", restaurante, cantidadPlatos)
                + String.format("   Candidato: %s en %s%n",
                        repartidor.getNombreCompleto(), repartidor.getTipoVehiculo());

        if (repartidor.isMochilaTermica()) {
            return detalle
                    + String.format("   Verificando mochila térmica... OK%n")
                    + String.format("   Pedido asignado a %s", repartidor.getNombreCompleto());
        }
        return detalle
                + String.format("   Verificando mochila térmica... RECHAZADO%n")
                + String.format("   %s no cuenta con mochila térmica: se buscará otro repartidor.",
                        repartidor.getNombreCompleto());
    }
}
