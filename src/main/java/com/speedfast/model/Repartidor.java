package com.speedfast.model;

/**
 * Repartidor de SpeedFast. Sus atributos permiten validar si cumple los
 * requisitos de cada tipo de pedido antes de aceptar la asignación.
 */
public class Repartidor {

    /** Identificador único del repartidor. */
    private int idRepartidor;

    /** Nombre del repartidor. */
    private String nombre;

    /** Apellido del repartidor. */
    private String apellido;

    /** Teléfono de contacto. */
    private String telefono;

    /** Dirección particular del repartidor. */
    private String direccion;

    /** Vehículo con el que realiza los repartos. */
    private String tipoVehiculo;

    /** Carga máxima que puede transportar, en kilogramos. */
    private float pesoMaximo;

    /** Indica si cuenta con mochila térmica, requisito de los pedidos de comida. */
    private boolean mochilaTermica;

    /** Indica si puede tomar un pedido de inmediato. */
    private boolean disponibleInmediato;

    /** Distancia a la que se encuentra del punto de retiro, en kilómetros. */
    private float distanciaKm;

    /**
     * Crea un repartidor con todos sus atributos.
     *
     * @param idRepartidor        identificador único
     * @param nombre              nombre del repartidor
     * @param apellido            apellido del repartidor
     * @param telefono            teléfono de contacto
     * @param direccion           dirección particular
     * @param tipoVehiculo        vehículo utilizado para el reparto
     * @param pesoMaximo          carga máxima en kilogramos
     * @param mochilaTermica      {@code true} si cuenta con mochila térmica
     * @param disponibleInmediato {@code true} si puede tomar un pedido de inmediato
     * @param distanciaKm         distancia al punto de retiro en kilómetros
     */
    public Repartidor(int idRepartidor, String nombre, String apellido, String telefono,
                      String direccion, String tipoVehiculo, float pesoMaximo,
                      boolean mochilaTermica, boolean disponibleInmediato, float distanciaKm) {
        this.idRepartidor = idRepartidor;
        this.nombre = nombre;
        this.apellido = apellido;
        this.telefono = telefono;
        this.direccion = direccion;
        this.tipoVehiculo = tipoVehiculo;
        this.pesoMaximo = pesoMaximo;
        this.mochilaTermica = mochilaTermica;
        this.disponibleInmediato = disponibleInmediato;
        this.distanciaKm = distanciaKm;
    }

    /** @return el identificador del repartidor */
    public int getIdRepartidor() {
        return idRepartidor;
    }

    /** @param idRepartidor nuevo identificador del repartidor */
    public void setIdRepartidor(int idRepartidor) {
        this.idRepartidor = idRepartidor;
    }

    /** @return el nombre del repartidor */
    public String getNombre() {
        return nombre;
    }

    /** @param nombre nuevo nombre del repartidor */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /** @return el apellido del repartidor */
    public String getApellido() {
        return apellido;
    }

    /** @param apellido nuevo apellido del repartidor */
    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    /** @return el teléfono de contacto */
    public String getTelefono() {
        return telefono;
    }

    /** @param telefono nuevo teléfono de contacto */
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    /** @return la dirección particular del repartidor */
    public String getDireccion() {
        return direccion;
    }

    /** @param direccion nueva dirección particular */
    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    /** @return el vehículo utilizado para el reparto */
    public String getTipoVehiculo() {
        return tipoVehiculo;
    }

    /** @param tipoVehiculo nuevo vehículo utilizado para el reparto */
    public void setTipoVehiculo(String tipoVehiculo) {
        this.tipoVehiculo = tipoVehiculo;
    }

    /** @return la carga máxima en kilogramos */
    public float getPesoMaximo() {
        return pesoMaximo;
    }

    /** @param pesoMaximo nueva carga máxima en kilogramos */
    public void setPesoMaximo(float pesoMaximo) {
        this.pesoMaximo = pesoMaximo;
    }

    /** @return {@code true} si cuenta con mochila térmica */
    public boolean isMochilaTermica() {
        return mochilaTermica;
    }

    /** @param mochilaTermica {@code true} si cuenta con mochila térmica */
    public void setMochilaTermica(boolean mochilaTermica) {
        this.mochilaTermica = mochilaTermica;
    }

    /** @return {@code true} si puede tomar un pedido de inmediato */
    public boolean isDisponibleInmediato() {
        return disponibleInmediato;
    }

    /** @param disponibleInmediato {@code true} si puede tomar un pedido de inmediato */
    public void setDisponibleInmediato(boolean disponibleInmediato) {
        this.disponibleInmediato = disponibleInmediato;
    }

    /** @return la distancia al punto de retiro en kilómetros */
    public float getDistanciaKm() {
        return distanciaKm;
    }

    /** @param distanciaKm nueva distancia al punto de retiro en kilómetros */
    public void setDistanciaKm(float distanciaKm) {
        this.distanciaKm = distanciaKm;
    }

    /** @return el nombre y el apellido del repartidor */
    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    /** @return representación textual breve del repartidor */
    @Override
    public String toString() {
        return String.format("Repartidor %d: %s (%s)", idRepartidor, getNombreCompleto(), tipoVehiculo);
    }
}
