package com.speedfast.app;

import com.speedfast.model.Pedido;
import com.speedfast.model.PedidoComida;
import com.speedfast.model.PedidoEncomienda;
import com.speedfast.model.PedidoExpress;
import com.speedfast.model.Repartidor;
import com.speedfast.service.ControladorDeEnvios;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Simulación concurrente del sistema de entregas de SpeedFast. Prepara los
 * pedidos y repartidores, ejecuta a cada repartidor como un hilo independiente
 * mediante {@link ExecutorService} y muestra los resultados una vez que todos
 * terminaron su recorrido.
 */
public class Main {

    /** Ancho de las líneas separadoras de sección. */
    private static final int ANCHO_SEPARADOR = 62;

    /** Segundos máximos de espera antes de dar por cerrada la simulación. */
    private static final int ESPERA_MAXIMA_SEGUNDOS = 30;

    /** Pedido reservado para demostrar la cancelación al final de la simulación. */
    private static Pedido pedidoParaCancelar;

    /**
     * Punto de entrada de la aplicación.
     *
     * @param args argumentos de línea de comandos (no se utilizan)
     */
    public static void main(String[] args) {
        ControladorDeEnvios controlador = new ControladorDeEnvios();

        List<Repartidor> repartidores = prepararSimulacion(controlador);
        ejecutarSimulacionConcurrente(repartidores);
        mostrarResultados(controlador);
    }

    /**
     * Registra los repartidores y sus pedidos, y realiza las asignaciones.
     * Cada repartidor recibe pedidos acordes a su perfil: Juan cuenta con
     * mochila térmica, Camila dispone de furgón y Luis se mueve en bicicleta
     * dentro del radio de cobertura.
     *
     * @param controlador controlador donde se registra todo el sistema
     * @return los repartidores listos para ejecutarse como hilos
     */
    private static List<Repartidor> prepararSimulacion(ControladorDeEnvios controlador) {
        imprimirTitulo("1. PREPARACIÓN DE LA SIMULACIÓN");

        Repartidor juan = new Repartidor(1, "Juan", "Pérez", "+56 9 1111 1111",
                "Av. Matta 210, Santiago", "Motocicleta", 15.0f, true, true, 1.8f, controlador);
        Repartidor camila = new Repartidor(2, "Camila", "Soto", "+56 9 2222 2222",
                "Calle Lira 45, Santiago", "Furgón", 50.0f, false, true, 4.5f, controlador);
        Repartidor luis = new Repartidor(3, "Luis", "Díaz", "+56 9 3333 3333",
                "Pasaje Los Olmos 78, Ñuñoa", "Bicicleta", 8.0f, false, true, 1.2f, controlador);

        controlador.registrarRepartidor(juan);
        controlador.registrarRepartidor(camila);
        controlador.registrarRepartidor(luis);

        Pedido comida1 = new PedidoComida(101, "Av. Italia 456, Providencia",
                4.0f, "Sushi Kai", 3);
        Pedido comida2 = new PedidoComida(102, "Av. Pedro de Valdivia 320, Providencia",
                6.0f, "Pizzería Roma", 2);
        Pedido encomienda1 = new PedidoEncomienda(103, "Av. Santa Rosa 567, Santiago",
                6.0f, 12.5f, "Caja de cartón sellada");
        Pedido encomienda2 = new PedidoEncomienda(104, "Gran Avenida 1220, San Miguel",
                9.0f, 25.0f, "Pallet plastificado");
        Pedido express1 = new PedidoExpress(105, "Av. Apoquindo 1500, Las Condes",
                7.0f, "Farmacia Central", 3.0f);
        Pedido express2 = new PedidoExpress(106, "Irarrázaval 890, Ñuñoa",
                3.0f, "Supermercado Los Leones", 3.0f);

        pedidoParaCancelar = new PedidoComida(107, "Av. Recoleta 145, Recoleta",
                5.0f, "Cocina Peruana", 1);

        for (Pedido pedido : List.of(comida1, comida2, encomienda1, encomienda2,
                express1, express2, pedidoParaCancelar)) {
            controlador.registrarPedido(pedido);
        }

        // Asignación por objeto: valida los requisitos y encola el pedido en un solo paso.
        System.out.println(controlador.asignarPedidoA(comida1, juan));
        System.out.println(controlador.asignarPedidoA(comida2, juan));
        System.out.println(controlador.asignarPedidoA(encomienda1, camila));

        // Asignación por nombre: el controlador resuelve el nombre y delega en la misma lógica.
        System.out.println(controlador.asignarRepartidor(encomienda2, "Camila"));

        System.out.println(controlador.asignarPedidoA(express1, luis));
        System.out.println(controlador.asignarPedidoA(express2, luis));
        System.out.println();

        return List.of(juan, camila, luis);
    }

    /**
     * Ejecuta a todos los repartidores en paralelo mediante un pool de hilos y
     * espera a que cada uno termine su recorrido completo.
     *
     * @param repartidores repartidores que se ejecutarán como hilos
     */
    private static void ejecutarSimulacionConcurrente(List<Repartidor> repartidores) {
        imprimirTitulo("2. SIMULACIÓN CONCURRENTE DE ENTREGAS");

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
            }
        }

        System.out.println();
        System.out.println("[Sistema] Todos los repartidores finalizaron sus entregas.");
        System.out.println();
    }

    /**
     * Muestra el estado final de los pedidos, prueba la cancelación de un
     * pedido pendiente y despliega el historial de entregas del sistema.
     *
     * @param controlador controlador con el estado final del sistema
     */
    private static void mostrarResultados(ControladorDeEnvios controlador) {
        imprimirTitulo("3. ESTADO FINAL DE LOS PEDIDOS");
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

        imprimirTitulo("4. CANCELACIÓN DE UN PEDIDO PENDIENTE (Cancelable)");
        System.out.printf("Cancelando %s #%d...%n",
                pedidoParaCancelar.getTipoPedido(), pedidoParaCancelar.getIdPedido());
        System.out.println(controlador.cancelar(pedidoParaCancelar));
        System.out.println();

        imprimirTitulo("5. HISTORIAL DE ENTREGAS (Rastreable)");
        for (String entrega : controlador.verHistorial()) {
            System.out.println(" - " + entrega);
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
