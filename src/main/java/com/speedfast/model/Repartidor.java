package com.speedfast.model;

import com.speedfast.exception.EntregaException;
import com.speedfast.service.ControladorDeEnvios;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Repartidor de SpeedFast. Sus atributos permiten validar si cumple los
 * requisitos de cada tipo de pedido antes de aceptar la asignación.
 *
 * <p>Implementa {@link Runnable}, de modo que cada repartidor se ejecuta como
 * un hilo independiente que recorre su propia lista de pedidos y simula las
 * entregas en paralelo con el resto de los repartidores.</p>
 */
public class Repartidor implements Runnable {

    /** Tiempo mínimo que simula una entrega, en milisegundos. */
    private static final int ESPERA_MINIMA_MS = 500;

    /** Rango aleatorio que se suma a la espera mínima, en milisegundos. */
    private static final int ESPERA_ALEATORIA_MS = 1500;

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

    /** Pedidos que este repartidor debe entregar durante su recorrido. */
    private final List<Pedido> pedidosAsignados = new ArrayList<>();

    /** Controlador que registra las entregas realizadas. */
    private final ControladorDeEnvios controlador;

    /** Generador de las pausas aleatorias que simulan cada entrega. */
    private final Random random = new Random();

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
     * @param controlador         controlador donde se registran las entregas
     */
    public Repartidor(int idRepartidor, String nombre, String apellido, String telefono,
                      String direccion, String tipoVehiculo, float pesoMaximo,
                      boolean mochilaTermica, boolean disponibleInmediato, float distanciaKm,
                      ControladorDeEnvios controlador) {
        this.controlador = controlador;
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

    /**
     * Agrega un pedido al recorrido de este repartidor. Lo invoca el
     * controlador de envíos una vez validada la asignación.
     *
     * @param pedido pedido que el repartidor deberá entregar
     */
    public void agregarPedido(Pedido pedido) {
        pedidosAsignados.add(pedido);
    }

    /** @return una copia de los pedidos asignados a este repartidor */
    public List<Pedido> getPedidosAsignados() {
        return new ArrayList<>(pedidosAsignados);
    }

    /**
     * Recorre los pedidos asignados y los entrega uno a uno. Este método es el
     * que ejecuta el hilo del repartidor, en paralelo con los demás.
     *
     * <p>Si un pedido no puede entregarse, se informa el motivo y el recorrido
     * continúa con el siguiente. Si el hilo es interrumpido, termina de forma
     * controlada restaurando la marca de interrupción.</p>
     */
    @Override
    public void run() {
        if (pedidosAsignados.isEmpty()) {
            System.out.printf("[Repartidor: %s] Sin pedidos asignados.%n", nombre);
            return;
        }

        for (Pedido pedido : pedidosAsignados) {
            try {
                entregarPedido(pedido);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.printf("[Repartidor: %s] Recorrido interrumpido.%n", nombre);
                return;
            } catch (EntregaException e) {
                System.out.printf("[Repartidor: %s] No se pudo entregar el pedido %d: %s%n",
                        nombre, pedido.getIdPedido(), e.getMessage());
            } catch (RuntimeException e) {
                System.out.printf("[Repartidor: %s] Error inesperado en el pedido %d: %s%n",
                        nombre, pedido.getIdPedido(), e.getMessage());
            }
        }
        System.out.printf("[Repartidor: %s] Recorrido finalizado.%n", nombre);
    }

    /**
     * Entrega un pedido: lo despacha, simula el traslado con una pausa
     * aleatoria y confirma la entrega en el controlador.
     *
     * @param pedido pedido a entregar
     * @throws EntregaException     si el pedido no está en condiciones de ser despachado
     * @throws InterruptedException si el hilo es interrumpido durante el traslado
     */
    private void entregarPedido(Pedido pedido) throws EntregaException, InterruptedException {
        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new EntregaException("el pedido fue cancelado");
        }
        if (pedido.getRepartidorAsignado() == null) {
            throw new EntregaException("el pedido no tiene repartidor asignado");
        }

        controlador.despachar(pedido);
        System.out.printf("[Repartidor: %s] Entregando %s #%d... (%d min estimados)%n",
                nombre, pedido.getTipoPedido(), pedido.getIdPedido(), pedido.calcularTiempoEntrega());

        Thread.sleep(ESPERA_MINIMA_MS + random.nextInt(ESPERA_ALEATORIA_MS));

        if (!controlador.registrarEntrega(pedido)) {
            throw new EntregaException("no fue posible confirmar la entrega");
        }
        System.out.printf("[Repartidor: %s] Pedido #%d entregado.%n", nombre, pedido.getIdPedido());
    }

    /** @return representación textual breve del repartidor */
    @Override
    public String toString() {
        return String.format("Repartidor %d: %s (%s)", idRepartidor, getNombreCompleto(), tipoVehiculo);
    }
}
