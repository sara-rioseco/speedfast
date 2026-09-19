package com.speedfast.view;

import com.speedfast.model.Pedido;
import com.speedfast.service.ControladorDeEnvios;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * Ventana que muestra los pedidos registrados en una {@link JTable}, cuyos
 * datos se administran con un {@link DefaultTableModel}. La tabla se carga al
 * abrir la ventana y el botón Refrescar la vuelve a cargar, por ejemplo después
 * de registrar un pedido o mientras avanzan las entregas.
 */
public class VentanaListaPedidos extends JFrame {

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

    /**
     * Crea la ventana del listado y carga los pedidos actuales.
     *
     * @param controlador controlador compartido con los pedidos registrados
     */
    public VentanaListaPedidos(ControladorDeEnvios controlador) {
        super("Listado de pedidos");
        this.controlador = controlador;

        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.addActionListener(e -> refrescarTabla());
        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panelBoton.add(btnRefrescar);

        JPanel contenido = new JPanel(new BorderLayout(0, 10));
        contenido.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contenido.add(new JScrollPane(new JTable(modelo)), BorderLayout.CENTER);
        contenido.add(panelBoton, BorderLayout.SOUTH);

        setContentPane(contenido);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(700, 350);
        setLocationRelativeTo(null);

        refrescarTabla();
    }

    /** Vuelve a cargar la tabla con los pedidos que tiene el controlador en este momento. */
    private void refrescarTabla() {
        modelo.setRowCount(0);
        for (Pedido pedido : controlador.getPedidos()) {
            modelo.addRow(new Object[]{
                    pedido.getIdPedido(),
                    pedido.getTipoPedido(),
                    pedido.getDireccionEntrega(),
                    pedido.getEstado(),
                    pedido.getRepartidorAsignado() == null
                            ? "Sin asignar"
                            : pedido.getRepartidorAsignado().getNombreCompleto()
            });
        }
    }
}
