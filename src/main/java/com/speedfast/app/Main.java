package com.speedfast.app;

import com.speedfast.model.Pedido;
import com.speedfast.model.PedidoComida;
import com.speedfast.model.PedidoEncomienda;
import com.speedfast.model.PedidoExpress;
import com.speedfast.model.Repartidor;
import com.speedfast.service.ControladorDeEnvios;

/**
 * Simulación del sistema de entregas de SpeedFast. Registra pedidos y
 * repartidores en el {@link ControladorDeEnvios} y recorre el ciclo completo:
 * asignación automática y manual, cálculo de tiempos, despacho, cancelación e
 * historial de entregas.
 */
public class Main {

    /** Ancho de las líneas separadoras de sección. */
    private static final int ANCHO_SEPARADOR = 62;

    /**
     * Punto de entrada de la aplicación.
     *
     * @param args argumentos de línea de comandos (no se utilizan)
     */
    public static void main(String[] args) {

        ControladorDeEnvios controlador = new ControladorDeEnvios();

        // Repartidores disponibles en la plataforma.
        Repartidor juan = new Repartidor(1, "Juan", "Pérez", "+56 9 1111 1111",
                "Av. Matta 210, Santiago", "Motocicleta", 15.0f, true, true, 1.8f);
        Repartidor camila = new Repartidor(2, "Camila", "Soto", "+56 9 2222 2222",
                "Calle Lira 45, Santiago", "Furgón", 50.0f, false, true, 4.5f);
        Repartidor luis = new Repartidor(3, "Luis", "Díaz", "+56 9 3333 3333",
                "Pasaje Los Olmos 78, Ñuñoa", "Bicicleta", 8.0f, false, true, 1.2f);

        controlador.registrarRepartidor(juan);
        controlador.registrarRepartidor(camila);
        controlador.registrarRepartidor(luis);

        // Un pedido de cada tipo, referenciados con el tipo de la clase abstracta.
        Pedido comida = new PedidoComida(101, "Av. Italia 456, Providencia",
                4.0f, "Sushi Kai", 3);
        Pedido encomienda = new PedidoEncomienda(102, "Av. Santa Rosa 567, Santiago",
                6.0f, 12.5f, "Caja de cartón sellada");
        Pedido express = new PedidoExpress(103, "Av. Apoquindo 1500, Las Condes",
                7.0f, "Farmacia Central", 3.0f);

        controlador.registrarPedido(comida);
        controlador.registrarPedido(encomienda);
        controlador.registrarPedido(express);

        // 1. Estado inicial del sistema.
        imprimirTitulo("1. PEDIDOS REGISTRADOS EN EL SISTEMA");
        for (Pedido pedido : controlador.getPedidos()) {
            System.out.println(pedido);
        }
        System.out.println();

        // 2. Asignación automática: el controlador consulta los requisitos de cada pedido.
        imprimirTitulo("2. ASIGNACIÓN AUTOMÁTICA DE REPARTIDORES");
        System.out.println(controlador.asignarAutomaticamente(comida));
        System.out.println();
        System.out.println(controlador.asignarAutomaticamente(encomienda));
        System.out.println();

        // 3. Asignación manual mediante las tres versiones sobrecargadas del método.
        imprimirTitulo("3. ASIGNACIÓN MANUAL (SOBRECARGA Y VALIDACIONES)");
        System.out.println(express.asignarRepartidor());
        System.out.println();
        System.out.println(express.asignarRepartidor("Luis Díaz"));
        System.out.println();
        System.out.println(express.asignarRepartidor(camila));  // rechazado: ocupada en otro reparto
        System.out.println();
        System.out.println(express.asignarRepartidor(luis));    // aceptado: cercano y disponible
        System.out.println();

        // 4. Resumen y comparación de los tiempos calculados por cada subclase.
        imprimirTitulo("4. RESUMEN DE PEDIDOS Y TIEMPO ESTIMADO");
        for (Pedido pedido : controlador.getPedidos()) {
            pedido.mostrarResumen();
            System.out.println();
        }
        imprimirTablaComparativa(controlador);

        // 5. Despacho de pedidos (interfaz Despachable).
        imprimirTitulo("5. DESPACHO DE PEDIDOS (Despachable)");
        System.out.println(controlador.despachar(comida));
        System.out.println(controlador.despachar(encomienda));
        System.out.println();

        // 6. Cancelación de pedidos (interfaz Cancelable).
        imprimirTitulo("6. CANCELACIÓN DE PEDIDOS (Cancelable)");
        System.out.printf("Cancelando %s #%d...%n", express.getTipoPedido(), express.getIdPedido());
        System.out.println(controlador.cancelar(express));
        System.out.println();
        System.out.printf("Cancelando %s #%d...%n", comida.getTipoPedido(), comida.getIdPedido());
        System.out.println(controlador.cancelar(comida));  // rechazado: ya fue despachado
        System.out.println();

        // 7. Historial del sistema y de un pedido en particular (interfaz Rastreable).
        imprimirTitulo("7. HISTORIAL DE ENTREGAS (Rastreable)");
        System.out.println("Entregas realizadas por el sistema:");
        for (String entrega : controlador.verHistorial()) {
            System.out.println(" - " + entrega);
        }
        System.out.println();
        System.out.printf("Seguimiento del %s #%d:%n", express.getTipoPedido(), express.getIdPedido());
        for (String evento : express.verHistorial()) {
            System.out.println(" - " + evento);
        }
    }

    /**
     * Imprime una tabla comparativa con los tiempos estimados de cada pedido.
     *
     * @param controlador controlador que contiene los pedidos registrados
     */
    private static void imprimirTablaComparativa(ControladorDeEnvios controlador) {
        System.out.printf("%-24s %-6s %-11s %-24s %s%n",
                "TIPO DE PEDIDO", "N°", "DISTANCIA", "ESTADO", "TIEMPO");
        System.out.println("-".repeat(ANCHO_SEPARADOR));
        for (Pedido pedido : controlador.getPedidos()) {
            System.out.printf("%-24s %-6d %-11s %-24s %d min%n",
                    pedido.getTipoPedido(),
                    pedido.getIdPedido(),
                    String.format("%.1f km", pedido.getDistanciaKm()),
                    pedido.getEstado().getDescripcion(),
                    pedido.calcularTiempoEntrega());
        }
        System.out.println("-".repeat(ANCHO_SEPARADOR));
        System.out.println();
    }

    /**
     * Imprime un título de sección para separar las pruebas en consola.
     *
     * @param titulo texto de la sección
     */
    private static void imprimirTitulo(String titulo) {
        System.out.println("=".repeat(ANCHO_SEPARADOR));
        System.out.println(titulo);
        System.out.println("=".repeat(ANCHO_SEPARADOR));
    }
}
