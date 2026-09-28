package com.speedfast.view;

import com.speedfast.model.Pedido;
import com.speedfast.service.ControladorDeEnvios;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Ventana que muestra los pedidos guardados en la base de datos en una
 * {@link JTable}, cuyos datos se administran con un {@link DefaultTableModel}.
 *
 * <p>La tabla se mantiene sincronizada sola: un {@link Timer} de Swing vuelve a
 * consultar la base de datos cada segundo, de modo que los cambios de estado
 * producidos por las entregas (o por registros hechos en otras ventanas)
 * aparecen sin necesidad de presionar Refrescar. Cada consulta se ejecuta con
 * un {@link SwingWorker}, fuera del hilo gráfico, para que la ventana nunca se
 * congele esperando a la base de datos.</p>
 */
public class VentanaListaPedidos extends JFrame {

    /** Milisegundos entre dos consultas automáticas a la base de datos. */
    private static final int INTERVALO_ACTUALIZACION_MS = 1000;

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Controlador compartido del que se obtienen los pedidos. */
    private final ControladorDeEnvios controlador;

    /** Datos de la tabla. Sus celdas no son editables: el listado es solo de consulta. */
    private final DefaultTableModel modelo =
            new DefaultTableModel(new String[]{"ID", "Tipo", "Dirección", "Estado", "Repartidor"}, 0) {
                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            };

    /** Informa cuándo se actualizó la tabla por última vez, o si la consulta falló. */
    private final JLabel lblEstado = new JLabel(" ");

    /** Dispara la consulta periódica. Se detiene al cerrar la ventana. */
    private final Timer temporizador = new Timer(INTERVALO_ACTUALIZACION_MS, e -> refrescarTabla());

    /** Filas que muestra la tabla, para no redibujarla si los datos no cambiaron. Solo se usa desde el EDT. */
    private List<List<Object>> filasMostradas = List.of();

    /** Indica si hay una consulta en curso, para no acumular consultas. Solo se usa desde el EDT. */
    private boolean consultaEnCurso;

    /**
     * Crea la ventana del listado, carga los pedidos actuales e inicia la
     * actualización automática.
     *
     * @param controlador controlador compartido con acceso a los pedidos guardados
     */
    public VentanaListaPedidos(ControladorDeEnvios controlador) {
        super("Listado de pedidos");
        this.controlador = controlador;

        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.addActionListener(e -> refrescarTabla());

        JPanel panelInferior = new JPanel(new BorderLayout(10, 0));
        panelInferior.add(lblEstado, BorderLayout.CENTER);
        panelInferior.add(btnRefrescar, BorderLayout.EAST);

        JPanel contenido = new JPanel(new BorderLayout(0, 10));
        contenido.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contenido.add(new JScrollPane(new JTable(modelo)), BorderLayout.CENTER);
        contenido.add(panelInferior, BorderLayout.SOUTH);

        setContentPane(contenido);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(700, 350);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                temporizador.stop();
            }
        });

        refrescarTabla();
        temporizador.start();
    }

    /**
     * Consulta los pedidos en la base de datos en un hilo de fondo y, al
     * terminar, actualiza la tabla desde el hilo gráfico. Si ya hay una
     * consulta en curso, no se inicia otra.
     */
    private void refrescarTabla() {
        if (consultaEnCurso) {
            return;
        }
        consultaEnCurso = true;

        new SwingWorker<List<List<Object>>, Void>() {
            @Override
            protected List<List<Object>> doInBackground() throws Exception {
                return aFilas(controlador.consultarPedidos());
            }

            @Override
            protected void done() {
                consultaEnCurso = false;
                try {
                    mostrarFilas(get());
                    lblEstado.setText("Actualizado desde la base de datos a las "
                            + LocalTime.now().format(FORMATO_HORA));
                    lblEstado.setToolTipText(null);
                } catch (ExecutionException e) {
                    lblEstado.setText("Sin conexión: " + e.getCause().getMessage());
                    lblEstado.setToolTipText(e.getCause().getCause() == null
                            ? null : e.getCause().getCause().getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /**
     * Convierte los pedidos en las filas de la tabla.
     *
     * @param pedidos pedidos leídos desde la base de datos
     * @return una fila por pedido: ID, tipo, dirección, estado y repartidor
     */
    private static List<List<Object>> aFilas(List<Pedido> pedidos) {
        List<List<Object>> filas = new ArrayList<>();
        for (Pedido pedido : pedidos) {
            filas.add(List.of(
                    pedido.getIdPedido(),
                    pedido.getTipoPedido(),
                    pedido.getDireccionEntrega(),
                    pedido.getEstado(),
                    pedido.getRepartidorAsignado() == null
                            ? "Sin asignar"
                            : pedido.getRepartidorAsignado().getNombreCompleto()));
        }
        return filas;
    }

    /**
     * Carga las filas en la tabla, solo si cambiaron desde la última
     * consulta: así la tabla no se redibuja cada segundo ni pierde la fila
     * seleccionada mientras los datos se mantienen iguales.
     *
     * @param filas filas a mostrar
     */
    private void mostrarFilas(List<List<Object>> filas) {
        if (filas.equals(filasMostradas)) {
            return;
        }
        modelo.setRowCount(0);
        for (List<Object> fila : filas) {
            modelo.addRow(fila.toArray());
        }
        filasMostradas = filas;
    }
}
