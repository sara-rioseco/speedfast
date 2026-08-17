package com.speedfast.app;

import com.speedfast.model.Pedido;
import com.speedfast.model.PedidoComida;
import com.speedfast.model.PedidoEncomienda;
import com.speedfast.model.PedidoExpress;
import com.speedfast.model.Repartidor;

/**
 * Clase de prueba del sistema SpeedFast. Instancia un pedido de cada tipo y
 * ejecuta las tres versiones de {@code asignarRepartidor()} para evidenciar la
 * sobrescritura y la sobrecarga del método.
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

        // Un pedido de cada tipo, referenciados con el tipo de la clase base.
        Pedido comida = new PedidoComida(101, "Av. Providencia 1234, Providencia",
                "Sushi Kai", 3);
        Pedido encomienda = new PedidoEncomienda(102, "Los Leones 456, Providencia",
                12.5f, "Caja de cartón sellada");
        Pedido express = new PedidoExpress(103, "Irarrázaval 789, Ñuñoa",
                "Farmacia Central", 3.0f);

        Pedido[] pedidos = {comida, encomienda, express};
        Repartidor[] repartidores = {juan, camila, luis};

        // 1. Sobrescritura: el mismo llamado se resuelve según el tipo real del objeto.
        imprimirTitulo("1. SOBRESCRITURA: asignarRepartidor()");
        for (Pedido pedido : pedidos) {
            System.out.println(pedido.asignarRepartidor());
            System.out.println();
        }

        // 2. Sobrecarga con el nombre del repartidor.
        imprimirTitulo("2. SOBRECARGA: asignarRepartidor(String nombreRepartidor)");
        System.out.println(comida.asignarRepartidor("Juan Pérez"));
        System.out.println();
        System.out.println(encomienda.asignarRepartidor("Camila Soto"));
        System.out.println();
        System.out.println(express.asignarRepartidor("Luis Díaz"));
        System.out.println();

        // 3. Sobrecarga con el objeto completo: valida los datos reales del repartidor.
        imprimirTitulo("3. SOBRECARGA: asignarRepartidor(Repartidor repartidor)");
        for (int i = 0; i < pedidos.length; i++) {
            System.out.println(pedidos[i].asignarRepartidor(repartidores[i]));
            System.out.println();
        }

        // 4. Casos que no cumplen los requisitos de cada tipo de pedido.
        imprimirTitulo("4. VALIDACIONES RECHAZADAS");
        System.out.println(comida.asignarRepartidor(camila));  // sin mochila térmica
        System.out.println();
        System.out.println(encomienda.asignarRepartidor(luis)); // capacidad insuficiente
        System.out.println();
        System.out.println(express.asignarRepartidor(camila));  // fuera del radio de cobertura
        System.out.println();

        // 5. Comportamiento genérico heredado de la clase base.
        imprimirTitulo("5. PEDIDO GENÉRICO (CLASE BASE)");
        Pedido generico = new Pedido(104, "San Diego 321, Santiago", "Pedido Genérico");
        System.out.println(generico.asignarRepartidor());
        System.out.println();
        System.out.println(generico.asignarRepartidor("Juan Pérez"));
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
