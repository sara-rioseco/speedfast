package com.speedfast.model;

import com.speedfast.exception.EntregaException;
import com.speedfast.service.ControladorDeEnvios;
import com.speedfast.service.ZonaDeCarga;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Repartidor de SpeedFast. Sus atributos permiten validar si cumple los
 * requisitos de cada tipo de pedido antes de aceptar la asignación.
 *
 * <p>Implementa {@link Runnable}, de modo que cada repartidor se ejecuta como
 * un hilo independiente que retira pedidos desde la {@link ZonaDeCarga}
 * compartida y simula las entregas en paralelo con el resto de repartidores.</p>
 */
public class Repartidor implements Runnable {

    /** Tiempo mínimo que simula una entrega, en milisegundos. */
    private static final int ESPERA_MINIMA_MS = 500;

    /** Rango aleatorio que se suma a la espera mínima, en milisegundos. */
    private static final int ESPERA_ALEATORIA_MS = 1500;

    /** Identificador único del repartidor. */
    private final int idRepartidor;

    /** Nombre del repartidor. */
    private final String nombre;

    /** Apellido del repartidor. */
    private final String apellido;

    /** Teléfono de contacto. */
    private final String telefono;

    /** Dirección particular del repartidor. */
    private final String direccion;

    /** Vehículo con el que realiza los repartos. */
    private final String tipoVehiculo;

    /** Carga máxima que puede transportar, en kilogramos. */
    private final float pesoMaximo;

    /** Indica si cuenta con mochila térmica, requisito de los pedidos de comida. */
    private final boolean mochilaTermica;

    /**
     * Indica si puede tomar un pedido de inmediato. Es el único atributo que
     * cambia durante la simulación, y se declara {@code volatile} porque se
     * escribe desde el controlador y se lee desde la zona de carga, que usan
     * bloqueos distintos.
     */
    private volatile boolean disponibleInmediato;

    /** Distancia a la que se encuentra del punto de retiro, en kilómetros. */
    private final float distanciaKm;

    /** Pedidos que este repartidor retiró y entregó durante su recorrido. */
    private final List<Pedido> pedidosAsignados = new ArrayList<>();

    /** Zona de carga compartida desde la que retira sus pedidos. */
    private final ZonaDeCarga zonaDeCarga;

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
     * @param zonaDeCarga         zona de carga compartida desde la que retira pedidos
     * @param controlador         controlador donde se registran las entregas
     */
    public Repartidor(int idRepartidor, String nombre, String apellido, String telefono,
                      String direccion, String tipoVehiculo, float pesoMaximo,
                      boolean mochilaTermica, boolean disponibleInmediato, float distanciaKm,
                      ZonaDeCarga zonaDeCarga, ControladorDeEnvios controlador) {
        this.zonaDeCarga = zonaDeCarga;
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

    /** @return el nombre del repartidor */
    public String getNombre() {
        return nombre;
    }

    /** @return el apellido del repartidor */
    public String getApellido() {
        return apellido;
    }

    /** @return el teléfono de contacto */
    public String getTelefono() {
        return telefono;
    }

    /** @return la dirección particular del repartidor */
    public String getDireccion() {
        return direccion;
    }

    /** @return el vehículo utilizado para el reparto */
    public String getTipoVehiculo() {
        return tipoVehiculo;
    }

    /** @return la carga máxima en kilogramos */
    public float getPesoMaximo() {
        return pesoMaximo;
    }

    /** @return {@code true} si cuenta con mochila térmica */
    public boolean isMochilaTermica() {
        return mochilaTermica;
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
     * Retira pedidos de la zona de carga y los entrega uno a uno, hasta que ya
     * no quede ninguno que este repartidor pueda atender. Este método es el que
     * ejecuta el hilo del repartidor, en paralelo con los demás.
     *
     * <p>Como la zona de carga es un recurso compartido, cada retiro se realiza
     * dentro de un método sincronizado: así un mismo pedido nunca es tomado por
     * dos repartidores. Si un pedido no puede entregarse, se informa el motivo
     * y el recorrido continúa con el siguiente; si el hilo es interrumpido,
     * termina de forma controlada restaurando la marca de interrupción.</p>
     */
    @Override
    public void run() {
        Pedido pedido = zonaDeCarga.retirarPedido(this);

        if (pedido == null) {
            System.out.printf("[Repartidor - %s] No hay pedidos compatibles en la zona de carga.%n", nombre);
            return;
        }

        while (pedido != null) {
            try {
                entregarPedido(pedido);
                pedidosAsignados.add(pedido);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.printf("[Repartidor - %s] Recorrido interrumpido.%n", nombre);
                return;
            } catch (EntregaException e) {
                System.out.printf("[Repartidor - %s] No se pudo entregar el pedido #%d: %s%n",
                        nombre, pedido.getIdPedido(), e.getMessage());
            } catch (RuntimeException e) {
                System.out.printf("[Repartidor - %s] Error inesperado en el pedido #%d: %s%n",
                        nombre, pedido.getIdPedido(), e.getMessage());
            }
            pedido = zonaDeCarga.retirarPedido(this);
        }

        System.out.printf("[Repartidor - %s] Recorrido finalizado: %d pedidos entregados.%n",
                nombre, pedidosAsignados.size());
    }

    /**
     * Entrega un pedido ya retirado de la zona de carga: lo pone en reparto,
     * simula el traslado con una pausa aleatoria y confirma la entrega.
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

        System.out.printf("[Repartidor - %s] Retirando pedido #%d... Destino: %s%n",
                nombre, pedido.getIdPedido(), pedido.getDireccionEntrega());

        controlador.despachar(pedido);
        System.out.printf("[Repartidor - %s] Estado: %s%n", nombre, pedido.getEstado());

        System.out.printf("[Repartidor - %s] Entregando pedido #%d... (%d min estimados)%n",
                nombre, pedido.getIdPedido(), pedido.calcularTiempoEntrega());

        Thread.sleep(ESPERA_MINIMA_MS + random.nextInt(ESPERA_ALEATORIA_MS));

        if (!controlador.registrarEntrega(pedido)) {
            throw new EntregaException("no fue posible confirmar la entrega");
        }
        zonaDeCarga.confirmarEntrega();
        System.out.printf("[Repartidor - %s] Estado: %s%n", nombre, pedido.getEstado());
    }

    /** @return representación textual breve del repartidor */
    @Override
    public String toString() {
        return String.format("Repartidor %d: %s (%s)", idRepartidor, getNombreCompleto(), tipoVehiculo);
    }
}
