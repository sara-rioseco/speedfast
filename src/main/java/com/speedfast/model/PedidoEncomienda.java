package com.speedfast.model;

/**
 * Pedido de encomienda (documentos o paquetes). Requiere validar el peso del
 * bulto y su embalaje antes de asignar un repartidor.
 */
public class PedidoEncomienda extends Pedido {

    /** Tipo de servicio con el que se identifica esta subclase. */
    private static final String TIPO = "Pedido de Encomienda";

    /** Peso de la encomienda, en kilogramos. */
    private float pesoKg;

    /** Embalaje declarado para la encomienda. */
    private String tipoEmbalaje;

    /**
     * Crea un pedido de encomienda.
     *
     * @param idPedido         identificador único del pedido
     * @param direccionEntrega dirección de entrega
     * @param pesoKg           peso de la encomienda en kilogramos
     * @param tipoEmbalaje     embalaje declarado
     */
    public PedidoEncomienda(int idPedido, String direccionEntrega, float pesoKg, String tipoEmbalaje) {
        super(idPedido, direccionEntrega, TIPO);
        this.pesoKg = pesoKg;
        this.tipoEmbalaje = tipoEmbalaje;
    }

    /** @return el peso de la encomienda en kilogramos */
    public float getPesoKg() {
        return pesoKg;
    }

    /** @param pesoKg nuevo peso de la encomienda en kilogramos */
    public void setPesoKg(float pesoKg) {
        this.pesoKg = pesoKg;
    }

    /** @return el embalaje declarado */
    public String getTipoEmbalaje() {
        return tipoEmbalaje;
    }

    /** @param tipoEmbalaje nuevo embalaje declarado */
    public void setTipoEmbalaje(String tipoEmbalaje) {
        this.tipoEmbalaje = tipoEmbalaje;
    }

    /**
     * Sobrescritura: busca un repartidor capaz de transportar el peso declarado.
     *
     * @return mensaje de asignación para imprimir en consola
     */
    @Override
    public String asignarRepartidor() {
        return encabezado()
                + String.format("Asignando repartidor...%n")
                + String.format("   Peso declarado: %.1f kg | Embalaje: %s%n", pesoKg, tipoEmbalaje)
                + String.format("   Se requiere validar peso y embalaje antes de despachar.%n")
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
                + String.format("   Peso declarado: %.1f kg | Embalaje: %s%n", pesoKg, tipoEmbalaje)
                + String.format("   Validando peso y embalaje... OK%n")
                + String.format("   Pedido asignado a %s", nombreRepartidor);
    }

    /**
     * Sobrecarga sobrescrita: compara el peso de la encomienda con la capacidad
     * del repartidor y comprueba que el embalaje esté declarado.
     *
     * @param repartidor repartidor candidato a tomar el pedido
     * @return mensaje de asignación para imprimir en consola
     */
    @Override
    public String asignarRepartidor(Repartidor repartidor) {
        String detalle = encabezado()
                + String.format("Asignando repartidor...%n")
                + String.format("   Peso declarado: %.1f kg | Embalaje: %s%n", pesoKg, tipoEmbalaje)
                + String.format("   Candidato: %s | Capacidad: %.1f kg%n",
                        repartidor.getNombreCompleto(), repartidor.getPesoMaximo());

        boolean embalajeValido = tipoEmbalaje != null && !tipoEmbalaje.isBlank();
        boolean pesoValido = pesoKg <= repartidor.getPesoMaximo();

        if (pesoValido && embalajeValido) {
            return detalle
                    + String.format("   Validando peso y embalaje... OK%n")
                    + String.format("   Pedido asignado a %s", repartidor.getNombreCompleto());
        }
        if (!pesoValido) {
            return detalle
                    + String.format("   Validando peso y embalaje... RECHAZADO%n")
                    + String.format("   El peso supera la capacidad de %s: se buscará otro repartidor.",
                            repartidor.getNombreCompleto());
        }
        return detalle
                + String.format("   Validando peso y embalaje... RECHAZADO%n")
                + String.format("   La encomienda no tiene embalaje declarado: no puede despacharse.");
    }
}
