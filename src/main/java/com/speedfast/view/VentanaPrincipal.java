package com.speedfast.view;

import com.speedfast.model.EstadoPedido;
import com.speedfast.service.ControladorDeEnvios;
import com.speedfast.service.SimuladorEntregas;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.concurrent.ExecutionException;

/**
 * Ventana principal de SpeedFast. Desde aquí se abren las ventanas de registro
 * y listado de pedidos, y se inician las entregas.
 *
 * <p>Todas las ventanas reciben el mismo {@link ControladorDeEnvios}, que es
 * donde viven los datos: un pedido registrado en el formulario aparece luego
 * en el listado.</p>
 */
public class VentanaPrincipal extends JFrame {

    private static final String TEXTO_ENTREGAS = "Asignar repartidor / Iniciar entrega";

    /** Controlador compartido por todas las ventanas. */
    private final ControladorDeEnvios controlador;

    /** Ejecuta la ronda de entregas concurrentes. */
    private final SimuladorEntregas simulador;

    private final JButton btnEntregas = new JButton(TEXTO_ENTREGAS);

    /**
     * Crea la ventana principal.
     *
     * @param controlador controlador compartido con los pedidos y repartidores
     * @param simulador   simulador que ejecuta las entregas en paralelo
     */
    public VentanaPrincipal(ControladorDeEnvios controlador, SimuladorEntregas simulador) {
        super("SpeedFast");
        this.controlador = controlador;
        this.simulador = simulador;

        JLabel titulo = new JLabel("Gestión de entregas", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));

        JButton btnRegistrar = new JButton("Registrar pedido");
        JButton btnListar = new JButton("Listar pedidos");
        btnRegistrar.addActionListener(e -> new VentanaRegistroPedido(controlador).setVisible(true));
        btnListar.addActionListener(e -> new VentanaListaPedidos(controlador).setVisible(true));
        btnEntregas.addActionListener(e -> iniciarEntregas());

        JPanel botones = new JPanel(new GridLayout(3, 1, 0, 10));
        botones.add(btnRegistrar);
        botones.add(btnListar);
        botones.add(btnEntregas);

        JPanel contenido = new JPanel(new BorderLayout(0, 15));
        contenido.setBorder(BorderFactory.createEmptyBorder(20, 30, 25, 30));
        contenido.add(titulo, BorderLayout.NORTH);
        contenido.add(botones, BorderLayout.CENTER);

        setContentPane(contenido);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(380, 280);
        setResizable(false);
        setLocationRelativeTo(null);
    }

    /**
     * Inicia las entregas: los repartidores retiran los pedidos pendientes que
     * cumplen con su perfil (quedando asignados a ellos) y los entregan en
     * paralelo.
     *
     * <p>La ronda se ejecuta con un {@link SwingWorker}, es decir, en un hilo de
     * fondo: la ventana sigue respondiendo mientras los repartidores trabajan.
     * Al terminar, {@code done()} vuelve al hilo gráfico para habilitar el
     * botón y mostrar el resultado.</p>
     */
    private void iniciarEntregas() {
        btnEntregas.setEnabled(false);
        btnEntregas.setText("Entregas en curso...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                simulador.ejecutarEntregas();
                return null;
            }

            @Override
            protected void done() {
                btnEntregas.setText(TEXTO_ENTREGAS);
                btnEntregas.setEnabled(true);
                try {
                    get();
                    JOptionPane.showMessageDialog(VentanaPrincipal.this,
                            "Entregas finalizadas.\nPedidos entregados: "
                                    + controlador.contarPedidos(EstadoPedido.ENTREGADO)
                                    + " de " + controlador.getPedidos().size()
                                    + "\n\nRevisa el listado para ver el repartidor de cada pedido.",
                            "Entregas", JOptionPane.INFORMATION_MESSAGE);
                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(VentanaPrincipal.this,
                            "Ocurrió un error durante las entregas: " + e.getCause().getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }
}
