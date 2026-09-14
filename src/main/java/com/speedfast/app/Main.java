package com.speedfast.app;

import com.speedfast.model.EstadoPedido;
import com.speedfast.model.Pedido;
import com.speedfast.model.PedidoComida;
import com.speedfast.model.PedidoEncomienda;
import com.speedfast.model.PedidoExpress;
import com.speedfast.model.Repartidor;
import com.speedfast.service.ControladorDeEnvios;
import com.speedfast.service.MonitorEstado;
import com.speedfast.service.ZonaDeCarga;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Simulación concurrente del despacho de SpeedFast. Los pedidos llegan a una
 * {@link ZonaDeCarga} compartida y tres repartidores, ejecutados como hilos
 * independientes mediante {@link ExecutorService}, los retiran de a uno y los
 * entregan. Un {@link MonitorEstado} informa el avance en tiempo real.
 */
public class Main {

    /** Ancho de las líneas separadoras de sección. */
    private static final int ANCHO_SEPARADOR = 62;

    /** Segundos máximos de espera antes de dar por cerrada la simulación. */
    private static final int ESPERA_MAXIMA_SEGUNDOS = 60;

    /**
     * Punto de entrada de la aplicación.
     *
     * @param args argumentos de línea de comandos (no se utilizan)
     */
    public static void main(String[] args) {
        ZonaDeCarga zonaDeCarga = new ZonaDeCarga();
        ControladorDeEnvios controlador = new ControladorDeEnvios();

        cargarPedidos(zonaDeCarga, controlador);
        List<Repartidor> repartidores = crearRepartidores(zonaDeCarga, controlador);
        ejecutarSimulacion(repartidores, zonaDeCarga);
        mostrarResultados(zonaDeCarga, controlador);
    }

    /**
     * Registra los pedidos y los deja disponibles en la zona de carga.
     *
     * @param zonaDeCarga zona de carga compartida
     * @param controlador controlador donde se registran los pedidos
     */
    private static void cargarPedidos(ZonaDeCarga zonaDeCarga, ControladorDeEnvios controlador) {
        imprimirTitulo("1. ZONA DE CARGA INICIALIZADA");

        List<Pedido> pedidos = List.of(
                new PedidoComida(101, "Santiago Centro", 4.0f, "Sushi Kai", 3),
                new PedidoEncomienda(102, "Providencia", 6.0f, 12.5f, "Caja de cartón sellada"),
                new PedidoExpress(103, "Ñuñoa", 3.0f, "Farmacia Central", 3.0f),
                new PedidoComida(104, "Recoleta", 5.0f, "Pizzería Roma", 2),
                new PedidoEncomienda(105, "Las Condes", 9.0f, 25.0f, "Pallet plastificado"),
                new PedidoExpress(106, "Providencia", 2.0f, "Supermercado Los Leones", 3.0f));

        for (Pedido pedido : pedidos) {
            controlador.registrarPedido(pedido);
            zonaDeCarga.agregarPedido(pedido);
        }
        System.out.println();
    }

    /**
     * Crea los repartidores que competirán por los pedidos de la zona de carga.
     * Cada uno tiene un perfil distinto, por lo que solo retirará los pedidos
     * cuyos requisitos pueda cumplir.
     *
     * @param zonaDeCarga zona de carga compartida
     * @param controlador controlador donde se registran las entregas
     * @return los repartidores listos para ejecutarse como hilos
     */
    private static List<Repartidor> crearRepartidores(ZonaDeCarga zonaDeCarga,
                                                      ControladorDeEnvios controlador) {
        Repartidor juan = new Repartidor(1, "Juan", "Pérez", "+56 9 1111 1111",
                "Av. Matta 210, Santiago", "Motocicleta", 15.0f, true, true, 1.8f,
                zonaDeCarga, controlador);
        Repartidor camila = new Repartidor(2, "Camila", "Soto", "+56 9 2222 2222",
                "Calle Lira 45, Santiago", "Furgón", 50.0f, false, true, 2.5f,
                zonaDeCarga, controlador);
        Repartidor pedro = new Repartidor(3, "Pedro", "Díaz", "+56 9 3333 3333",
                "Pasaje Los Olmos 78, Ñuñoa", "Bicicleta", 8.0f, true, true, 1.2f,
                zonaDeCarga, controlador);

        List<Repartidor> repartidores = List.of(juan, camila, pedro);
        repartidores.forEach(controlador::registrarRepartidor);
        return repartidores;
    }

    /**
     * Lanza a los repartidores y al monitor en paralelo, y espera a que todos
     * los pedidos hayan sido retirados y entregados.
     *
     * @param repartidores repartidores que se ejecutarán como hilos
     * @param zonaDeCarga  zona de carga que el monitor auditará
     */
    private static void ejecutarSimulacion(List<Repartidor> repartidores, ZonaDeCarga zonaDeCarga) {
        imprimirTitulo("2. REPARTIDORES TRABAJANDO EN PARALELO");

        MonitorEstado monitor = new MonitorEstado(zonaDeCarga);
        Thread hiloMonitor = new Thread(monitor, "Monitor");
        hiloMonitor.setDaemon(true);
        hiloMonitor.start();

        try (ExecutorService executor = Executors.newFixedThreadPool(repartidores.size())) {
            for (Repartidor repartidor : repartidores) {
                executor.execute(repartidor);
            }
            executor.shutdown();

            try {
                if (!executor.awaitTermination(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS)) {
                    System.out.println("[Sistema] La simulación superó el tiempo máximo de espera.");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
                System.out.println("[Sistema] Simulación interrumpida.");
            } finally {
                monitor.detener();
            }
        }

        System.out.println();
        monitor.informarEstado();
        System.out.println();
    }

    /**
     * Muestra el estado final de cada pedido, el historial de entregas y el
     * mensaje de cierre de la simulación.
     *
     * @param zonaDeCarga zona de carga con los contadores finales
     * @param controlador controlador con el historial del sistema
     */
    private static void mostrarResultados(ZonaDeCarga zonaDeCarga, ControladorDeEnvios controlador) {
        imprimirEstadoFinal(controlador);
        imprimirHistorial(controlador);
        imprimirCierre(zonaDeCarga);
    }

    /**
     * Imprime una tabla con el estado final de cada pedido y su repartidor.
     *
     * @param controlador controlador con los pedidos registrados
     */
    private static void imprimirEstadoFinal(ControladorDeEnvios controlador) {
        imprimirTitulo("3. ESTADO FINAL DE LOS PEDIDOS");
        System.out.printf("%-10s %-24s %-16s %s%n", "PEDIDO", "DESTINO", "ESTADO", "REPARTIDOR");
        System.out.println("-".repeat(ANCHO_SEPARADOR));
        for (Pedido pedido : controlador.getPedidos()) {
            System.out.printf("%-10s %-24s %-16s %s%n",
                    "#" + pedido.getIdPedido(),
                    pedido.getDireccionEntrega(),
                    pedido.getEstado(),
                    pedido.getRepartidorAsignado() == null
                            ? "sin asignar"
                            : pedido.getRepartidorAsignado().getNombreCompleto());
        }
        System.out.println("-".repeat(ANCHO_SEPARADOR));
        System.out.println();
    }

    /**
     * Imprime el historial de entregas registrado por el sistema.
     *
     * @param controlador controlador que implementa {@code Rastreable}
     */
    private static void imprimirHistorial(ControladorDeEnvios controlador) {
        imprimirTitulo("4. HISTORIAL DE ENTREGAS (Rastreable)");
        for (String entrega : controlador.verHistorial()) {
            System.out.println(" - " + entrega);
        }
        System.out.println();
    }

    /**
     * Informa el cierre de la simulación: confirma que todos los pedidos fueron
     * entregados o detalla cuáles quedaron pendientes y por qué.
     *
     * @param zonaDeCarga zona de carga con los contadores finales
     */
    private static void imprimirCierre(ZonaDeCarga zonaDeCarga) {
        if (zonaDeCarga.todoEntregado()) {
            System.out.println("Todos los pedidos han sido entregados correctamente");
        } else {
            System.out.printf("Quedaron %d pedidos sin entregar de un total de %d.%n",
                    zonaDeCarga.getCantidadRecibidos() - zonaDeCarga.getCantidadEntregados(),
                    zonaDeCarga.getCantidadRecibidos());
            for (Pedido pendiente : zonaDeCarga.getPedidosPendientes()) {
                System.out.printf(" - Pedido #%d (%s) quedó en estado %s%n",
                        pendiente.getIdPedido(), pendiente.getTipoPedido(),
                        pendiente.getEstado() == EstadoPedido.PENDIENTE
                                ? "pendiente: ningún repartidor cumplía sus requisitos"
                                : pendiente.getEstado().getDescripcion());
            }
        }
    }

    /**
     * Imprime un título de sección para separar las etapas en consola.
     *
     * @param titulo texto de la sección
     */
    private static void imprimirTitulo(String titulo) {
        System.out.println("=".repeat(ANCHO_SEPARADOR));
        System.out.println(titulo);
        System.out.println("=".repeat(ANCHO_SEPARADOR));
    }
}
