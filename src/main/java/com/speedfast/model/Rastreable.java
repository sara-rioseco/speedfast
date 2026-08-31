package com.speedfast.model;

import java.util.List;

/**
 * Contrato de todo elemento cuyo avance puede consultarse como historial.
 * Lo implementan tanto los pedidos, que registran sus propios eventos, como
 * el controlador de envíos, que reúne las entregas realizadas.
 */
public interface Rastreable {

    /**
     * Entrega el historial de eventos registrados.
     *
     * @return la lista de eventos, del más antiguo al más reciente
     */
    List<String> verHistorial();
}
