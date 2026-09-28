package com.speedfast.view;

import com.speedfast.exception.PersistenciaException;
import com.speedfast.model.Repartidor;
import com.speedfast.service.ControladorDeEnvios;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Formulario para registrar un nuevo repartidor en la base de datos. Bajo el
 * formulario, una {@link JTable} muestra los repartidores ya guardados, y se
 * recarga desde la base de datos después de cada registro.
 *
 * <p>La tabla {@code repartidor} solo guarda el nombre, por lo que es el único
 * dato que se solicita. El repartidor se incorpora de inmediato a la
 * operación y participa desde la próxima ronda de entregas.</p>
 */
public class VentanaRegistroRepartidor extends JFrame {

    /** Largo máximo del nombre, igual al de la columna {@code nombre} (VARCHAR(100)). */
    private static final int LARGO_MAXIMO_NOMBRE = 100;

    /** Controlador compartido donde se registran los repartidores. */
    private final ControladorDeEnvios controlador;

    private final JTextField txtNombre = new JTextField(20);

    /** Datos de la tabla de repartidores. Sus celdas no son editables. */
    private final DefaultTableModel modelo =
            new DefaultTableModel(new String[]{"ID", "Nombre"}, 0) {
                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            };

    /**
     * Crea el formulario de registro y carga los repartidores guardados.
     *
     * @param controlador controlador compartido donde se registrarán los repartidores
     */
    public VentanaRegistroRepartidor(ControladorDeEnvios controlador) {
        super("Registrar repartidor");
        this.controlador = controlador;

        JButton btnGuardar = new JButton("Guardar");
        btnGuardar.addActionListener(e -> guardarRepartidor());

        JPanel formulario = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        formulario.add(new JLabel("Nombre completo:"));
        formulario.add(txtNombre);
        formulario.add(btnGuardar);

        JTable tabla = new JTable(modelo);
        tabla.getColumnModel().getColumn(0).setMaxWidth(60);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createTitledBorder("Repartidores registrados"));
        scroll.setPreferredSize(new Dimension(420, 200));

        JPanel contenido = new JPanel(new BorderLayout(0, 15));
        contenido.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contenido.add(formulario, BorderLayout.NORTH);
        contenido.add(scroll, BorderLayout.CENTER);

        setContentPane(contenido);
        getRootPane().setDefaultButton(btnGuardar);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);

        cargarRepartidores();
    }

    /**
     * Valida el nombre, crea el repartidor y lo guarda en la base de datos a
     * través del controlador. Informa el resultado con un {@link JOptionPane}.
     */
    private void guardarRepartidor() {
        String nombre = txtNombre.getText().trim();

        if (nombre.isEmpty()) {
            mostrarAdvertencia("Debes ingresar el nombre del repartidor.");
            return;
        }
        if (nombre.length() > LARGO_MAXIMO_NOMBRE) {
            mostrarAdvertencia("El nombre no puede superar los " + LARGO_MAXIMO_NOMBRE + " caracteres.");
            return;
        }

        Repartidor repartidor = new Repartidor(0, nombre);
        try {
            controlador.registrarRepartidor(repartidor);
        } catch (PersistenciaException e) {
            mostrarError(e);
            return;
        }

        JOptionPane.showMessageDialog(this,
                "Repartidor #" + repartidor.getIdRepartidor() + " registrado correctamente.\n"
                        + "Participará desde la próxima ronda de entregas.",
                "Repartidor registrado", JOptionPane.INFORMATION_MESSAGE);
        txtNombre.setText("");
        cargarRepartidores();
    }

    /** Vuelve a cargar la tabla con los repartidores guardados en la base de datos. */
    private void cargarRepartidores() {
        try {
            modelo.setRowCount(0);
            for (Repartidor repartidor : controlador.consultarRepartidores()) {
                modelo.addRow(new Object[]{repartidor.getIdRepartidor(), repartidor.getNombreCompleto()});
            }
        } catch (PersistenciaException e) {
            mostrarError(e);
        }
    }

    /**
     * Muestra un mensaje de validación.
     *
     * @param mensaje motivo por el que no se pudo guardar el repartidor
     */
    private void mostrarAdvertencia(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Dato inválido", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Muestra un error de la base de datos.
     *
     * @param e excepción con el motivo y el detalle del error
     */
    private void mostrarError(PersistenciaException e) {
        JOptionPane.showMessageDialog(this, e.getMensajeConDetalle(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
    }
}
