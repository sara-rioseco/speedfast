package com.speedfast.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Entrega de un pedido por parte de un repartidor: registra qué repartidor
 * sacó a reparto qué pedido, y en qué fecha y hora. Corresponde a la tabla
 * {@code entrega}, que relaciona las tablas {@code pedido} y {@code repartidor}.
 *
 * <p>Un repartidor puede realizar muchas entregas, y un pedido puede tener más
 * de una: cada entrega es un intento, y si uno queda interrumpido el pedido
 * vuelve a la zona de carga y se registra un nuevo intento al retirarlo otra vez.</p>
 */
public class Entrega {

    /** Identificador de la entrega. Lo asigna la base de datos al guardarla. */
    private int idEntrega;

    /** Pedido que sale a reparto. */
    private final Pedido pedido;

    /** Repartidor que realiza la entrega. */
    private final Repartidor repartidor;

    /** Fecha en que el pedido salió a reparto. */
    private final LocalDate fecha;

    /** Hora en que el pedido salió a reparto, sin fracciones de segundo. */
    private final LocalTime hora;

    /**
     * Crea una entrega que comienza en este momento. La fecha y la hora se
     * toman de un mismo instante, para que no queden desfasadas si la entrega
     * comienza justo a medianoche.
     *
     * @param pedido     pedido que sale a reparto
     * @param repartidor repartidor que realiza la entrega
     */
    public Entrega(Pedido pedido, Repartidor repartidor) {
        LocalDateTime ahora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        this.pedido = pedido;
        this.repartidor = repartidor;
        this.fecha = ahora.toLocalDate();
        this.hora = ahora.toLocalTime();
    }

    /** @return el identificador de la entrega, o 0 si aún no se guarda */
    public int getIdEntrega() {
        return idEntrega;
    }

    /**
     * Actualiza el identificador de la entrega. Lo usa {@code EntregaDAO} para
     * asignar el ID que genera la base de datos al guardarla.
     *
     * @param idEntrega nuevo identificador de la entrega
     */
    public void setIdEntrega(int idEntrega) {
        this.idEntrega = idEntrega;
    }

    /** @return el pedido que sale a reparto */
    public Pedido getPedido() {
        return pedido;
    }

    /** @return el repartidor que realiza la entrega */
    public Repartidor getRepartidor() {
        return repartidor;
    }

    /** @return la fecha en que el pedido salió a reparto */
    public LocalDate getFecha() {
        return fecha;
    }

    /** @return la hora en que el pedido salió a reparto */
    public LocalTime getHora() {
        return hora;
    }

    /** @return representación textual breve de la entrega */
    @Override
    public String toString() {
        return String.format("Entrega %d: pedido #%d con %s (%s %s)", idEntrega,
                pedido.getIdPedido(), repartidor.getNombreCompleto(), fecha, hora);
    }
}
