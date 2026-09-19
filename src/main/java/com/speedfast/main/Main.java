package com.speedfast.main;

import com.speedfast.model.Pedido;
import com.speedfast.model.PedidoComida;
import com.speedfast.model.PedidoEncomienda;
import com.speedfast.model.PedidoExpress;
import com.speedfast.model.Repartidor;
import com.speedfast.service.ControladorDeEnvios;
import com.speedfast.service.SimuladorEntregas;
import com.speedfast.service.ZonaDeCarga;
import com.speedfast.view.VentanaPrincipal;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.util.List;

/**
 * Punto de entrada de SpeedFast. Arma el sistema —zona de carga, controlador,
 * repartidores y pedidos iniciales— y abre la {@link VentanaPrincipal}, desde
 * la cual el usuario registra pedidos, los consulta, asigna repartidores e
 * inicia las entregas concurrentes.
 */
public class Main {

    /**
     * Punto de entrada de la aplicación.
     *
     * @param args argumentos de línea de comandos (no se utilizan)
     */
    public static void main(String[] args) {
        ZonaDeCarga zonaDeCarga = new ZonaDeCarga();
        ControladorDeEnvios controlador = new ControladorDeEnvios(zonaDeCarga);
        List<Repartidor> repartidores = crearRepartidores(zonaDeCarga, controlador);
        cargarPedidosIniciales(controlador);
        SimuladorEntregas simulador = new SimuladorEntregas(zonaDeCarga, repartidores);

        // Swing exige que las ventanas se creen y modifiquen desde su propio hilo (EDT).
        SwingUtilities.invokeLater(() -> {
            aplicarAspectoDelSistema();
            new VentanaPrincipal(controlador, simulador).setVisible(true);
        });
    }

    /**
     * Crea los repartidores de la plataforma. Cada uno tiene un perfil
     * distinto, por lo que solo podrá tomar los pedidos cuyos requisitos
     * cumpla.
     *
     * @param zonaDeCarga zona de carga compartida
     * @param controlador controlador donde se registran los repartidores
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
     * Registra los pedidos con los que parte el sistema, para que el listado
     * no comience vacío. El usuario puede agregar más desde la interfaz.
     *
     * @param controlador controlador donde se registran los pedidos
     */
    private static void cargarPedidosIniciales(ControladorDeEnvios controlador) {
        List<Pedido> pedidos = List.of(
                new PedidoComida(101, "Santiago Centro", 4.0f, "Sushi Kai", 3),
                new PedidoEncomienda(102, "Providencia", 6.0f, 12.5f, "Caja de cartón sellada"),
                new PedidoExpress(103, "Ñuñoa", 3.0f, "Farmacia Central", 3.0f),
                new PedidoComida(104, "Recoleta", 5.0f, "Pizzería Roma", 2),
                new PedidoEncomienda(105, "Las Condes", 9.0f, 25.0f, "Pallet plastificado"),
                new PedidoExpress(106, "Providencia", 2.0f, "Supermercado Los Leones", 3.0f));

        pedidos.forEach(controlador::registrarPedido);
    }

    /**
     * Usa el aspecto visual del sistema operativo, para que las ventanas se
     * vean familiares al usuario. Si no está disponible, Swing mantiene su
     * aspecto por defecto.
     */
    private static void aplicarAspectoDelSistema() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
            System.out.println("[Sistema] Se usará el aspecto visual por defecto de Swing.");
        }
    }
}
