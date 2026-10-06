package com.speedfast.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Entrega de un pedido por parte de un repartidor: registra qué repartidor
 * sacó a reparto qué pedido, y en qué fecha y hora. Corresponde a la tabla
 * {@code entregas}, que relaciona las tablas {@code pedidos} y {@code repartidores}.
 *
 * <p>Un repartidor puede realizar muchas entregas, y un pedido puede tener más
 * de una: cada entrega es un intento, y si uno queda interrumpido el pedido
 * vuelve a pendiente y se registra un nuevo intento al despacharlo otra vez.</p>
 *
 * <p>Las entregas se registran de dos formas: los repartidores de la
 * simulación las crean al salir a reparto, con la fecha y hora de ese momento,
 * y el usuario puede registrarlas desde la ventana de gestión de entregas,
 * indicando la fecha y la hora.</p>
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
     * Crea una entrega con todos sus datos. Valida que asocie un pedido y un
     * repartidor y que tenga fecha y hora, de modo que ninguna entrega
     * incompleta llegue a la base de datos.
     *
     * @param idEntrega  identificador de la entrega, o 0 si aún no se guarda
     * @param pedido     pedido que sale a reparto
     * @param repartidor repartidor que realiza la entrega
     * @param fecha      fecha en que el pedido salió a reparto
     * @param hora       hora en que el pedido salió a reparto
     * @throws IllegalArgumentException si falta el pedido, el repartidor, la fecha o la hora
     */
    public Entrega(int idEntrega, Pedido pedido, Repartidor repartidor, LocalDate fecha, LocalTime hora) {
        if (pedido == null || repartidor == null) {
            throw new IllegalArgumentException("La entrega debe asociar un pedido y un repartidor.");
        }
        if (fecha == null || hora == null) {
            throw new IllegalArgumentException("La entrega debe tener fecha y hora.");
        }
        this.idEntrega = idEntrega;
        this.pedido = pedido;
        this.repartidor = repartidor;
        this.fecha = fecha;
        this.hora = hora;
    }

    /**
     * Crea una entrega que comienza en este momento. La usan los repartidores
     * de la simulación al salir a reparto.
     *
     * @param pedido     pedido que sale a reparto
     * @param repartidor repartidor que realiza la entrega
     */
    public Entrega(Pedido pedido, Repartidor repartidor) {
        this(pedido, repartidor, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    /**
     * Toma la fecha y la hora de un mismo instante, para que no queden
     * desfasadas si la entrega comienza justo a medianoche.
     */
    private Entrega(Pedido pedido, Repartidor repartidor, LocalDateTime momento) {
        this(0, pedido, repartidor, momento.toLocalDate(), momento.toLocalTime());
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
