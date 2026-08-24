package com.speedfast.app;

import com.speedfast.model.Pedido;
import com.speedfast.model.PedidoComida;
import com.speedfast.model.PedidoEncomienda;
import com.speedfast.model.PedidoExpress;
import com.speedfast.model.Repartidor;

/**
 * Clase de prueba del sistema SpeedFast. Instancia un pedido de cada tipo y
 * ejecuta los métodos de la jerarquía para evidenciar el uso de la clase
 * abstracta {@link Pedido}, el cálculo diferenciado del tiempo de entrega y la
 * sobrecarga y sobrescritura de {@code asignarRepartidor()}.
 */
public class Main {

    /**
     * Punto de entrada de la aplicación.
     *
     * @param args argumentos de línea de comandos (no se utilizan)
     */
    public static void main(String[] args) {

        // Repartidores disponibles en la plataforma.
        Repartidor juan = new Repartidor(1, "Juan", "Pérez", "+56 9 1111 1111",
                "Av. Matta 210, Santiago", "Motocicleta", 15.0f, true, true, 1.8f);
        Repartidor camila = new Repartidor(2, "Camila", "Soto", "+56 9 2222 2222",
                "Calle Lira 45, Santiago", "Furgón", 50.0f, false, true, 4.5f);
        Repartidor luis = new Repartidor(3, "Luis", "Díaz", "+56 9 3333 3333",
                "Pasaje Los Olmos 78, Ñuñoa", "Bicicleta", 8.0f, false, true, 1.2f);

        // Un pedido de cada tipo, referenciados con el tipo de la clase abstracta.
        Pedido comida = new PedidoComida(101, "Av. Italia 456, Providencia",
                4.0f, "Sushi Kai", 3);
        Pedido encomienda = new PedidoEncomienda(102, "Av. Independencia 123, Independencia",
                6.0f, 12.5f, "Caja de cartón sellada");
        Pedido express = new PedidoExpress(103, "Av. Apoquindo 1500, Las Condes",
                7.0f, "Farmacia Central", 3.0f);

        Pedido[] pedidos = {comida, encomienda, express};
        Repartidor[] repartidores = {juan, camila, luis};

        // 1. Clase abstracta: mostrarResumen() es común y calcularTiempoEntrega() lo aporta cada subclase.
        imprimirTitulo("1. RESUMEN DE PEDIDOS Y TIEMPO ESTIMADO DE ENTREGA");
        for (Pedido pedido : pedidos) {
            pedido.mostrarResumen();
            System.out.println();
        }

        // 2. Comparación de los tiempos calculados por cada subclase.
        imprimirTitulo("2. COMPARACIÓN DE TIEMPOS ESTIMADOS");
        System.out.printf("%-24s %-6s %-12s %s%n", "TIPO DE PEDIDO", "N°", "DISTANCIA", "TIEMPO ESTIMADO");
        System.out.println("-".repeat(60));
        for (Pedido pedido : pedidos) {
            System.out.printf("%-24s %-6d %-12s %d minutos%n",
                    pedido.getTipoPedido(),
                    pedido.getIdPedido(),
                    String.format("%.1f km", pedido.getDistanciaKm()),
                    pedido.calcularTiempoEntrega());
        }
        System.out.println("-".repeat(60));
        System.out.println();

        // 3. Sobrescritura: el mismo llamado se resuelve según el tipo real del objeto.
        imprimirTitulo("3. SOBRESCRITURA: asignarRepartidor()");
        for (Pedido pedido : pedidos) {
            System.out.println(pedido.asignarRepartidor());
            System.out.println();
        }

        // 4. Sobrecarga con el nombre del repartidor.
        imprimirTitulo("4. SOBRECARGA: asignarRepartidor(String nombreRepartidor)");
        System.out.println(comida.asignarRepartidor("Juan Pérez"));
        System.out.println();
        System.out.println(encomienda.asignarRepartidor("Camila Soto"));
        System.out.println();
        System.out.println(express.asignarRepartidor("Luis Díaz"));
        System.out.println();

        // 5. Sobrecarga con el objeto completo: valida los datos reales del repartidor.
        imprimirTitulo("5. SOBRECARGA: asignarRepartidor(Repartidor repartidor)");
        for (int i = 0; i < pedidos.length; i++) {
            System.out.println(pedidos[i].asignarRepartidor(repartidores[i]));
            System.out.println();
        }

        // 6. Casos que no cumplen los requisitos de cada tipo de pedido.
        imprimirTitulo("6. VALIDACIONES RECHAZADAS");
        System.out.println(comida.asignarRepartidor(camila));   // sin mochila térmica
        System.out.println();
        System.out.println(encomienda.asignarRepartidor(luis)); // capacidad insuficiente
        System.out.println();
        System.out.println(express.asignarRepartidor(camila));  // fuera del radio de cobertura
    }

    /**
     * Imprime un título de sección para separar las pruebas en consola.
     *
     * @param titulo texto de la sección
     */
    private static void imprimirTitulo(String titulo) {
        System.out.println("=".repeat(60));
        System.out.println(titulo);
        System.out.println("=".repeat(60));
    }
}
