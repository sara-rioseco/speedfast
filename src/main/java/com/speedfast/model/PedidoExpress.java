package com.speedfast.model;

/**
 * Compra express en supermercado o farmacia. Debe asignarse al repartidor más
 * cercano que tenga disponibilidad inmediata.
 */
public class PedidoExpress extends Pedido {

    /** Tipo de servicio con el que se identifica esta subclase. */
    private static final String TIPO = "Pedido Express";

    /** Minutos considerados como base para una compra express. */
    private static final int TIEMPO_BASE = 10;

    /** Distancia a partir de la cual se aplica un recargo de tiempo, en kilómetros. */
    private static final float DISTANCIA_LIMITE_KM = 5.0f;

    /** Minutos extra que se suman al superar la distancia límite. */
    private static final int RECARGO_DISTANCIA = 5;

    /** Tienda en la que se realiza la compra. */
    private String tienda;

    /** Radio máximo aceptado para considerar cercano a un repartidor, en kilómetros. */
    private float radioMaximoKm;

    /**
     * Crea una compra express.
     *
     * @param idPedido         identificador único del pedido
     * @param direccionEntrega dirección de entrega
     * @param distanciaKm      distancia hasta la entrega, en kilómetros
     * @param tienda           tienda en la que se realiza la compra
     * @param radioMaximoKm    radio máximo aceptado, en kilómetros
     */
    public PedidoExpress(int idPedido, String direccionEntrega, float distanciaKm,
                         String tienda, float radioMaximoKm) {
        super(idPedido, direccionEntrega, distanciaKm, TIPO);
        this.tienda = tienda;
        this.radioMaximoKm = radioMaximoKm;
    }

    /** @return la tienda en la que se realiza la compra */
    public String getTienda() {
        return tienda;
    }

    /** @param tienda nueva tienda en la que se realiza la compra */
    public void setTienda(String tienda) {
        this.tienda = tienda;
    }

    /** @return el radio máximo aceptado, en kilómetros */
    public float getRadioMaximoKm() {
        return radioMaximoKm;
    }

    /** @param radioMaximoKm nuevo radio máximo aceptado, en kilómetros */
    public void setRadioMaximoKm(float radioMaximoKm) {
        this.radioMaximoKm = radioMaximoKm;
    }

    /**
     * Implementación del cálculo para compras express:
     * 10 minutos base, más 5 minutos extra si la distancia supera los 5 km.
     *
     * @return el tiempo estimado de entrega, en minutos
     */
    @Override
    public int calcularTiempoEntrega() {
        if (getDistanciaKm() > DISTANCIA_LIMITE_KM) {
            return TIEMPO_BASE + RECARGO_DISTANCIA;
        }
        return TIEMPO_BASE;
    }

    /**
     * Sobrescritura: busca al repartidor más cercano con disponibilidad inmediata.
     *
     * @return mensaje de asignación para imprimir en consola
     */
    @Override
    public String asignarRepartidor() {
        return encabezado()
                + String.format("Asignando repartidor...%n")
                + String.format("   Compra en: %s%n", tienda)
                + String.format("   Buscando al repartidor más cercano (radio máximo: %.1f km)...%n", radioMaximoKm)
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
                + String.format("   Compra en: %s%n", tienda)
                + String.format("   Repartidor más cercano con disponibilidad inmediata encontrado.%n")
                + String.format("   Pedido asignado a %s", nombreRepartidor);
    }

    /**
     * Sobrecarga sobrescrita: confirma que el repartidor esté disponible de
     * inmediato y dentro del radio de cercanía definido.
     *
     * @param repartidor repartidor candidato a tomar el pedido
     * @return mensaje de asignación para imprimir en consola
     */
    @Override
    public String asignarRepartidor(Repartidor repartidor) {
        String detalle = encabezado()
                + String.format("Asignando repartidor...%n")
                + String.format("   Compra en: %s%n", tienda)
                + String.format("   Candidato: %s | Distancia: %.1f km | Radio máximo: %.1f km%n",
                        repartidor.getNombreCompleto(), repartidor.getDistanciaKm(), radioMaximoKm);

        boolean estaCerca = repartidor.getDistanciaKm() <= radioMaximoKm;

        if (repartidor.isDisponibleInmediato() && estaCerca) {
            return detalle
                    + String.format("   Repartidor más cercano con disponibilidad inmediata encontrado.%n")
                    + String.format("   Pedido asignado a %s", repartidor.getNombreCompleto());
        }
        if (!repartidor.isDisponibleInmediato()) {
            return detalle
                    + String.format("   Verificando disponibilidad inmediata... RECHAZADO%n")
                    + String.format("   %s está ocupado en otro reparto: se buscará otro repartidor.",
                            repartidor.getNombreCompleto());
        }
        return detalle
                + String.format("   Verificando cercanía... RECHAZADO%n")
                + String.format("   %s está fuera del radio de cobertura: se buscará otro repartidor.",
                        repartidor.getNombreCompleto());
    }
}
