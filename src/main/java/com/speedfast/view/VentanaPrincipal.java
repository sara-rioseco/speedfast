package com.speedfast.view;

import com.speedfast.exception.OperacionNoPermitidaException;
import com.speedfast.exception.PersistenciaException;
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutionException;

/**
 * Ventana principal de SpeedFast. Desde aquí se abren las ventanas de gestión
 * de pedidos, repartidores y entregas, y se inicia la ronda de entregas
 * concurrentes.
 *
 * <p>Todas las ventanas reciben el mismo {@link ControladorDeEnvios}, que
 * guarda los datos en la base de datos y avisa a las ventanas abiertas cada vez
 * que cambian: un pedido registrado en una ventana aparece en las demás.</p>
 */
public class VentanaPrincipal extends JFrame {

    private static final String TEXTO_ENTREGAS = "Asignar repartidor / Iniciar entrega";

    /** Controlador compartido por todas las ventanas. */
    private final ControladorDeEnvios controlador;

    /** Ejecuta la ronda de entregas concurrentes. */
    private final SimuladorEntregas simulador;

    private final JButton btnEntregas = new JButton(TEXTO_ENTREGAS);

    /** Indica si hay una ronda de entregas en curso. Solo se usa desde el hilo gráfico. */
    private boolean rondaEnCurso;

    /**
     * Resultado de una ronda de entregas, para informarlo al terminar.
     *
     * @param entregados pedidos entregados en la ronda
     * @param pendientes pedidos que siguen pendientes al terminar
     */
    private record ResultadoRonda(int entregados, int pendientes) {
    }

    /**
     * Crea la ventana principal.
     *
     * @param controlador controlador compartido con acceso a los datos
     * @param simulador   simulador que ejecuta las entregas en paralelo
     */
    public VentanaPrincipal(ControladorDeEnvios controlador, SimuladorEntregas simulador) {
        super("SpeedFast");
        this.controlador = controlador;
        this.simulador = simulador;

        JLabel titulo = new JLabel("Panel de gestión", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));

        JButton btnPedidos = new JButton("Gestión de pedidos");
        JButton btnRepartidores = new JButton("Gestión de repartidores");
        JButton btnGestionEntregas = new JButton("Gestión de entregas");
        btnPedidos.addActionListener(e -> new VentanaGestionPedidos(controlador).setVisible(true));
        btnRepartidores.addActionListener(e -> new VentanaGestionRepartidores(controlador).setVisible(true));
        btnGestionEntregas.addActionListener(e -> new VentanaGestionEntregas(controlador).setVisible(true));
        btnEntregas.addActionListener(e -> iniciarEntregas());

        JPanel botones = new JPanel(new GridLayout(4, 1, 0, 10));
        botones.add(btnPedidos);
        botones.add(btnRepartidores);
        botones.add(btnGestionEntregas);
        botones.add(btnEntregas);

        JPanel contenido = new JPanel(new BorderLayout(0, 15));
        contenido.setBorder(BorderFactory.createEmptyBorder(20, 30, 25, 30));
        contenido.add(titulo, BorderLayout.NORTH);
        contenido.add(botones, BorderLayout.CENTER);

        setContentPane(contenido);
        // El cierre se controla en cerrar(), para no interrumpir una ronda en curso.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cerrar();
            }
        });
        setSize(380, 330);
        setResizable(false);
        setLocationRelativeTo(null);
    }

    /**
     * Cierra la aplicación, salvo que haya una ronda en curso: cerrarla en ese
     * momento dejaría pedidos en reparto que nadie terminaría de entregar.
     */
    private void cerrar() {
        if (rondaEnCurso) {
            JOptionPane.showMessageDialog(this,
                    "Hay una ronda de entregas en curso.\nEspera a que termine para cerrar SpeedFast.",
                    "Entregas", JOptionPane.WARNING_MESSAGE);
            return;
        }
        System.exit(0);
    }

    /**
     * Inicia una ronda de entregas: el controlador carga desde la base de
     * datos los repartidores y los pedidos pendientes, y los repartidores los
     * retiran (quedando asignados a ellos) y los entregan en paralelo.
     *
     * <p>La ronda se ejecuta con un {@link SwingWorker}, es decir, en un hilo de
     * fondo: la ventana sigue respondiendo mientras los repartidores trabajan.
     * Al terminar, {@code done()} vuelve al hilo gráfico para habilitar el
     * botón y mostrar el resultado. Si no hay pedidos pendientes o
     * repartidores registrados, se informa en lugar de iniciar la ronda.</p>
     */
    private void iniciarEntregas() {
        rondaEnCurso = true;
        btnEntregas.setEnabled(false);
        btnEntregas.setText("Entregas en curso...");

        new SwingWorker<ResultadoRonda, Void>() {
            @Override
            protected ResultadoRonda doInBackground() throws Exception {
                int entregados = simulador.ejecutarEntregas();
                int pendientes = controlador.consultarPedidos(EstadoPedido.PENDIENTE, null).size();
                return new ResultadoRonda(entregados, pendientes);
            }

            @Override
            protected void done() {
                rondaEnCurso = false;
                btnEntregas.setText(TEXTO_ENTREGAS);
                btnEntregas.setEnabled(true);
                try {
                    ResultadoRonda resultado = get();
                    JOptionPane.showMessageDialog(VentanaPrincipal.this,
                            "Entregas finalizadas.\nPedidos entregados en esta ronda: " + resultado.entregados()
                                    + "\nPedidos que siguen pendientes: " + resultado.pendientes()
                                    + "\n\nRevisa la gestión de pedidos o de entregas para ver "
                                    + "el repartidor de cada pedido.",
                            "Entregas", JOptionPane.INFORMATION_MESSAGE);
                } catch (ExecutionException e) {
                    informarFallo(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /**
     * Informa por qué no se pudo completar una ronda de entregas.
     *
     * @param causa excepción producida durante la ronda
     */
    private void informarFallo(Throwable causa) {
        if (causa instanceof OperacionNoPermitidaException) {
            JOptionPane.showMessageDialog(this, causa.getMessage(), "Entregas", JOptionPane.INFORMATION_MESSAGE);
        } else if (causa instanceof PersistenciaException errorBD) {
            JOptionPane.showMessageDialog(this, errorBD.getMensajeConDetalle(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Ocurrió un error durante las entregas: " + causa.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
