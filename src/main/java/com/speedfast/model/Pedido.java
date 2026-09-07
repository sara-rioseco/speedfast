package com.speedfast.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Pedido genérico de SpeedFast y clase base abstracta de la jerarquía.
 * Reúne los atributos y el comportamiento comunes a todo pedido, y delega en
 * cada subclase el cálculo de su tiempo de entrega y los requisitos que debe
 * cumplir el repartidor.
 *
 * <p>Implementa las interfaces {@link Despachable}, {@link Cancelable} y
 * {@link Rastreable}, de modo que las operaciones de despacho, cancelación y
 * seguimiento quedan separadas del resto de la lógica del pedido.</p>
 */
public abstract class Pedido implements Despachable, Cancelable, Rastreable {

    /** Identificador único del pedido. */
    private int idPedido;

    /** Dirección en la que debe entregarse el pedido. */
    private String direccionEntrega;

    /** Distancia a recorrer hasta la dirección de entrega, en kilómetros. */
    private float distanciaKm;

    /** Tipo de servicio: comida, encomienda o compra express. */
    private String tipoPedido;

    /** Estado actual del pedido dentro del sistema. */
    private EstadoPedido estado;

    /** Repartidor que tomó el pedido, o {@code null} si aún no se asigna. */
    private Repartidor repartidorAsignado;

    /** Eventos registrados durante la vida del pedido. */
    private final List<String> historial = new ArrayList<>();

    /**
     * Crea un pedido con todos sus atributos comunes, en estado pendiente.
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
        this.estado = EstadoPedido.PENDIENTE;
        registrarEvento("Pedido registrado en el sistema");
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

    /** @return el estado actual del pedido */
    public synchronized EstadoPedido getEstado() {
        return estado;
    }

    /** @return el repartidor asignado, o {@code null} si aún no se asigna */
    public synchronized Repartidor getRepartidorAsignado() {
        return repartidorAsignado;
    }

    /**
     * Calcula el tiempo estimado de entrega del pedido.
     * Cada subclase aplica su propia fórmula según el tipo de servicio.
     *
     * @return el tiempo estimado de entrega, en minutos
     */
    public abstract int calcularTiempoEntrega();

    /**
     * Indica si un repartidor cumple los requisitos propios del tipo de pedido.
     * Permite que el controlador busque candidatos sin conocer cada regla.
     *
     * @param repartidor repartidor a evaluar
     * @return {@code true} si el repartidor puede tomar este pedido
     */
    public abstract boolean cumpleRequisitos(Repartidor repartidor);

    /**
     * Imprime en consola los datos básicos del pedido, su estado y su tiempo
     * estimado de entrega, obtenido desde la subclase correspondiente.
     */
    public void mostrarResumen() {
        System.out.print(encabezado());
        System.out.printf("Distancia: %.1f km%n", distanciaKm);
        System.out.printf("Repartidor asignado: %s%n", nombreRepartidorAsignado());
        System.out.printf("Estado: %s%n", estado.getDescripcion());
        System.out.printf("Tiempo estimado de entrega: %d minutos%n", calcularTiempoEntrega());
    }

    /**
     * Despacha el pedido, siempre que tenga repartidor y no esté cancelado.
     *
     * <p>Se declara {@code synchronized} porque varios repartidores trabajan en
     * paralelo: la comprobación del estado y su actualización deben ocurrir de
     * forma indivisible para evitar condiciones de carrera.</p>
     *
     * @return el resultado de la operación, listo para imprimirse en consola
     */
    @Override
    public synchronized String despachar() {
        if (estado == EstadoPedido.CANCELADO) {
            return String.format("No se puede despachar el pedido %d: se encuentra cancelado.", idPedido);
        }
        if (estado == EstadoPedido.DESPACHADO) {
            return String.format("El pedido %d ya había sido despachado.", idPedido);
        }
        if (repartidorAsignado == null) {
            return String.format("No se puede despachar el pedido %d: aún no tiene repartidor asignado.", idPedido);
        }
        estado = EstadoPedido.DESPACHADO;
        registrarEvento("Pedido despachado con " + repartidorAsignado.getNombreCompleto());
        return String.format("Pedido %d despachado correctamente con %s (%d minutos estimados).",
                idPedido, repartidorAsignado.getNombreCompleto(), calcularTiempoEntrega());
    }

    /**
     * Cancela el pedido, siempre que todavía no haya sido despachado.
     *
     * @return el resultado de la operación, listo para imprimirse en consola
     */
    @Override
    public synchronized String cancelar() {
        if (estado == EstadoPedido.ENTREGADO) {
            return String.format("No se puede cancelar el pedido %d: ya fue entregado.", idPedido);
        }
        if (estado == EstadoPedido.DESPACHADO) {
            return String.format("No se puede cancelar el pedido %d: ya fue despachado.", idPedido);
        }
        if (estado == EstadoPedido.CANCELADO) {
            return String.format("El pedido %d ya se encontraba cancelado.", idPedido);
        }
        estado = EstadoPedido.CANCELADO;
        registrarEvento("Pedido cancelado");
        return String.format("Pedido %d cancelado exitosamente.", idPedido);
    }

    /**
     * Confirma que el pedido llegó a destino, cerrando su ciclo de vida.
     * Solo puede confirmarse un pedido que fue despachado previamente.
     *
     * @return {@code true} si el pedido pasó a estado entregado
     */
    public synchronized boolean confirmarEntrega() {
        if (estado != EstadoPedido.DESPACHADO) {
            return false;
        }
        estado = EstadoPedido.ENTREGADO;
        registrarEvento("Pedido entregado en " + direccionEntrega);
        return true;
    }

    /**
     * Entrega el historial de eventos del pedido.
     *
     * @return una copia de la lista de eventos registrados
     */
    @Override
    public synchronized List<String> verHistorial() {
        return new ArrayList<>(historial);
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
     * Sobrecarga que recibe solo el nombre del repartidor. Deja constancia del
     * candidato en el historial, pero no confirma la asignación: sin el objeto
     * {@link Repartidor} no es posible validar los requisitos del pedido, y el
     * mensaje en consola no debe afirmar algo que el estado del objeto no refleja.
     *
     * <p>Para asignar por nombre de forma efectiva, el controlador de envíos
     * resuelve el nombre y delega en {@link #asignarRepartidor(Repartidor)}.</p>
     *
     * @param nombreRepartidor nombre del repartidor propuesto
     * @return mensaje de asignación para imprimir en consola
     */
    public synchronized String asignarRepartidor(String nombreRepartidor) {
        registrarEvento("Candidato propuesto: " + nombreRepartidor);
        return encabezado()
                + String.format("Asignando repartidor...%n")
                + String.format("   Candidato propuesto: %s%n", nombreRepartidor)
                + String.format("   La asignación se confirmará al validar sus datos.");
    }

    /**
     * Sobrecarga que recibe el repartidor completo para validar sus datos.
     * Cada subclase la sobrescribe con los requisitos de su tipo de servicio.
     *
     * @param repartidor repartidor candidato a tomar el pedido
     * @return mensaje de asignación para imprimir en consola
     */
    public String asignarRepartidor(Repartidor repartidor) {
        confirmarAsignacion(repartidor);
        return encabezado()
                + String.format("Asignando repartidor...%n")
                + String.format("   Verificando disponibilidad general... OK%n")
                + String.format("   Pedido asignado a %s (%s)",
                        repartidor.getNombreCompleto(), repartidor.getTipoVehiculo());
    }

    /**
     * Registra la asignación de un repartidor y actualiza el estado del pedido.
     * Lo utilizan las subclases una vez validados sus propios requisitos.
     *
     * @param repartidor repartidor que tomará el pedido
     */
    protected synchronized void confirmarAsignacion(Repartidor repartidor) {
        this.repartidorAsignado = repartidor;
        this.estado = EstadoPedido.ASIGNADO;
        registrarEvento("Repartidor asignado: " + repartidor.getNombreCompleto());
    }

    /**
     * Agrega un evento al historial del pedido. Se mantiene privado porque el
     * constructor lo invoca: un método sobrescribible llamado desde el
     * constructor se ejecutaría antes de que la subclase termine de inicializarse.
     *
     * @param evento descripción del evento ocurrido
     */
    private void registrarEvento(String evento) {
        historial.add(evento);
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

    /** @return el nombre del repartidor asignado, o un texto por defecto */
    private String nombreRepartidorAsignado() {
        return repartidorAsignado == null ? "sin asignar" : repartidorAsignado.getNombreCompleto();
    }

    /** @return representación textual breve del pedido */
    @Override
    public String toString() {
        return String.format("%s [id=%d, dirección=%s, %.1f km, %s]",
                tipoPedido, idPedido, direccionEntrega, distanciaKm, estado.getDescripcion());
    }
}
