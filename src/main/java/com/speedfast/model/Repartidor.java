package com.speedfast.model;

import com.speedfast.exception.EntregaException;
import com.speedfast.exception.PersistenciaException;
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
 *
 * <p>La tabla {@code repartidores} solo guarda el identificador y el nombre. La
 * zona de carga y el controlador no forman parte de esos datos: se asignan con
 * {@link #vincular(ZonaDeCarga, ControladorDeEnvios)} cuando el repartidor se
 * incorpora a una ronda de entregas.</p>
 */
public class Repartidor implements Runnable {

    /** Largo máximo del nombre, igual al de la columna {@code nombre} (VARCHAR(100)). */
    public static final int LARGO_MAXIMO_NOMBRE = 100;

    /** Tiempo mínimo que simula una entrega, en milisegundos. */
    private static final int ESPERA_MINIMA_MS = 500;

    /** Rango aleatorio que se suma a la espera mínima, en milisegundos. */
    private static final int ESPERA_ALEATORIA_MS = 1500;

    /** Vehículo del perfil estándar. */
    private static final String VEHICULO_ESTANDAR = "Motocicleta";

    /** Carga máxima del perfil estándar, en kilogramos. */
    private static final float PESO_MAXIMO_ESTANDAR_KG = 15.0f;

    /** Distancia al punto de retiro del perfil estándar, en kilómetros. */
    private static final float DISTANCIA_ESTANDAR_KM = 1.5f;

    /**
     * Identificador único del repartidor. Lo asigna la base de datos al
     * guardarlo, antes de que el repartidor se incorpore a la operación.
     */
    private int idRepartidor;

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

    /**
     * Zona de carga compartida desde la que retira sus pedidos. Se asigna al
     * vincular al repartidor, antes de lanzar su hilo: {@code ExecutorService}
     * garantiza que el hilo vea los valores asignados antes de su ejecución.
     */
    private ZonaDeCarga zonaDeCarga;

    /** Controlador que registra las entregas realizadas. Se asigna al vincularlo. */
    private ControladorDeEnvios controlador;

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
     * @throws IllegalArgumentException si el nombre está vacío o es demasiado largo
     */
    public Repartidor(int idRepartidor, String nombre, String apellido, String telefono,
                      String direccion, String tipoVehiculo, float pesoMaximo,
                      boolean mochilaTermica, boolean disponibleInmediato, float distanciaKm) {
        this.idRepartidor = idRepartidor;
        this.nombre = validarNombre(nombre);
        this.apellido = apellido;
        this.telefono = telefono;
        this.direccion = direccion;
        this.tipoVehiculo = tipoVehiculo;
        this.pesoMaximo = pesoMaximo;
        this.mochilaTermica = mochilaTermica;
        this.disponibleInmediato = disponibleInmediato;
        this.distanciaKm = distanciaKm;
    }

    /**
     * Crea un repartidor a partir de los datos que guarda la base de datos:
     * identificador y nombre. El resto de su perfil toma valores estándar
     * (motocicleta con mochila térmica, 15 kg de carga, disponible y a 1,5 km),
     * que le permiten atender cualquier pedido registrado desde la interfaz.
     *
     * @param idRepartidor identificador único
     * @param nombre       nombre del repartidor
     * @throws IllegalArgumentException si el nombre está vacío o es demasiado largo
     */
    public Repartidor(int idRepartidor, String nombre) {
        this(idRepartidor, nombre, "", "", "", VEHICULO_ESTANDAR, PESO_MAXIMO_ESTANDAR_KG,
                true, true, DISTANCIA_ESTANDAR_KM);
    }

    /**
     * Valida el nombre de un repartidor: es obligatorio y no puede superar el
     * largo de la columna {@code nombre}. La usa el constructor, de modo que
     * ningún repartidor pueda tener un nombre inválido, sin importar desde
     * dónde se cree. El formulario también la usa para validar antes de guardar.
     *
     * @param nombre nombre a validar
     * @return el nombre sin espacios al inicio ni al final
     * @throws IllegalArgumentException si el nombre está vacío o es demasiado largo
     */
    public static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del repartidor es obligatorio.");
        }
        String limpio = nombre.trim();
        if (limpio.length() > LARGO_MAXIMO_NOMBRE) {
            throw new IllegalArgumentException("El nombre del repartidor no puede superar los "
                    + LARGO_MAXIMO_NOMBRE + " caracteres.");
        }
        return limpio;
    }

    /**
     * Incorpora al repartidor a una ronda: le indica desde qué zona de carga
     * retira pedidos y en qué controlador registra sus entregas. Lo invoca el
     * controlador al preparar cada ronda de entregas.
     *
     * @param zonaDeCarga zona de carga compartida
     * @param controlador controlador donde se registran las entregas
     */
    public void vincular(ZonaDeCarga zonaDeCarga, ControladorDeEnvios controlador) {
        this.zonaDeCarga = zonaDeCarga;
        this.controlador = controlador;
    }

    /** @return el identificador del repartidor */
    public int getIdRepartidor() {
        return idRepartidor;
    }

    /**
     * Actualiza el identificador del repartidor. Lo usa {@code RepartidorDAO}
     * para asignar el ID que genera la base de datos al guardarlo.
     *
     * @param idRepartidor nuevo identificador del repartidor
     */
    public void setIdRepartidor(int idRepartidor) {
        this.idRepartidor = idRepartidor;
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

    /**
     * @return el nombre y el apellido del repartidor; solo el nombre si no
     *         tiene apellido registrado, como ocurre con los leídos desde la
     *         base de datos, cuya columna {@code nombre} guarda el nombre completo
     */
    public String getNombreCompleto() {
        return apellido.isBlank() ? nombre : nombre + " " + apellido;
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
     *
     * <p>Un mismo repartidor puede ejecutar varios recorridos (uno cada vez que
     * se inician las entregas desde la interfaz), por lo que el total
     * informado al final corresponde solo al recorrido actual.</p>
     *
     * @throws IllegalStateException si el repartidor aún no fue vinculado a una zona de carga
     */
    @Override
    public void run() {
        if (zonaDeCarga == null || controlador == null) {
            throw new IllegalStateException(
                    "El repartidor " + nombre + " no está vinculado a una zona de carga.");
        }
        int entregadosEnRecorrido = 0;
        Pedido pedido = zonaDeCarga.retirarPedido(this);

        if (pedido == null) {
            System.out.printf("[Repartidor - %s] No hay pedidos compatibles en la zona de carga.%n", nombre);
            return;
        }

        while (pedido != null) {
            try {
                entregarPedido(pedido);
                pedidosAsignados.add(pedido);
                entregadosEnRecorrido++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.printf("[Repartidor - %s] Recorrido interrumpido.%n", nombre);
                return;
            } catch (EntregaException e) {
                System.out.printf("[Repartidor - %s] No se pudo entregar el pedido #%d: %s%n",
                        nombre, pedido.getIdPedido(), e.getMessage());
            } catch (PersistenciaException e) {
                System.out.printf("[Repartidor - %s] Error de base de datos en el pedido #%d: %s%n",
                        nombre, pedido.getIdPedido(), e.getMensajeConDetalle());
            } catch (RuntimeException e) {
                System.out.printf("[Repartidor - %s] Error inesperado en el pedido #%d: %s%n",
                        nombre, pedido.getIdPedido(), e.getMessage());
            }
            pedido = zonaDeCarga.retirarPedido(this);
        }

        System.out.printf("[Repartidor - %s] Recorrido finalizado: %d pedidos entregados.%n",
                nombre, entregadosEnRecorrido);
    }

    /**
     * Entrega un pedido ya retirado de la zona de carga: lo pone en reparto,
     * simula el traslado con una pausa aleatoria y confirma la entrega. Cada
     * cambio de estado queda registrado en la base de datos por el controlador.
     *
     * @param pedido pedido a entregar
     * @throws EntregaException      si el pedido no está en condiciones de ser despachado
     * @throws PersistenciaException si no se pudo registrar el avance en la base de datos
     * @throws InterruptedException  si el hilo es interrumpido durante el traslado
     */
    private void entregarPedido(Pedido pedido)
            throws EntregaException, PersistenciaException, InterruptedException {
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

        if (!controlador.confirmarEntrega(pedido)) {
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
