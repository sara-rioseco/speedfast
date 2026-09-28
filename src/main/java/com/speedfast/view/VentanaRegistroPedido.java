package com.speedfast.view;

import com.speedfast.exception.PersistenciaException;
import com.speedfast.model.Pedido;
import com.speedfast.model.TipoPedido;
import com.speedfast.service.ControladorDeEnvios;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/**
 * Formulario para registrar un nuevo pedido a partir de su dirección y tipo.
 * Al presionar Guardar, los datos se validan y el pedido se guarda en la base
 * de datos por medio del {@link ControladorDeEnvios} compartido.
 *
 * <p>El ID ya no se ingresa: lo genera la base de datos ({@code AUTO_INCREMENT})
 * y se informa al confirmar el registro. Los demás datos que exige cada tipo
 * de pedido se completan con los valores estándar de {@link TipoPedido}.</p>
 */
public class VentanaRegistroPedido extends JFrame {

    /** Largo máximo de la dirección, igual al de la columna {@code direccion} (VARCHAR(150)). */
    private static final int LARGO_MAXIMO_DIRECCION = 150;

    /** Controlador compartido donde se registran los pedidos. */
    private final ControladorDeEnvios controlador;

    private final JTextField txtDireccion = new JTextField(20);
    private final JComboBox<TipoPedido> cmbTipo = new JComboBox<>(TipoPedido.values());

    /**
     * Crea el formulario de registro.
     *
     * @param controlador controlador compartido donde se registrarán los pedidos
     */
    public VentanaRegistroPedido(ControladorDeEnvios controlador) {
        super("Registrar pedido");
        this.controlador = controlador;

        JPanel formulario = new JPanel(new GridLayout(2, 2, 10, 10));
        formulario.add(new JLabel("Dirección:"));
        formulario.add(txtDireccion);
        formulario.add(new JLabel("Tipo:"));
        formulario.add(cmbTipo);

        JButton btnGuardar = new JButton("Guardar");
        btnGuardar.addActionListener(e -> guardarPedido());
        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panelBoton.add(btnGuardar);

        JPanel contenido = new JPanel(new BorderLayout(0, 15));
        contenido.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contenido.add(formulario, BorderLayout.CENTER);
        contenido.add(panelBoton, BorderLayout.SOUTH);

        setContentPane(contenido);
        getRootPane().setDefaultButton(btnGuardar);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    /**
     * Valida los campos, crea el pedido del tipo seleccionado y lo guarda en la
     * base de datos a través del controlador. Informa el resultado con un
     * {@link JOptionPane}.
     */
    private void guardarPedido() {
        String direccion = txtDireccion.getText().trim();

        if (direccion.isEmpty()) {
            mostrarAdvertencia("Debes ingresar la dirección de entrega.");
            return;
        }
        if (direccion.length() > LARGO_MAXIMO_DIRECCION) {
            mostrarAdvertencia("La dirección no puede superar los " + LARGO_MAXIMO_DIRECCION + " caracteres.");
            return;
        }

        TipoPedido tipo = (TipoPedido) cmbTipo.getSelectedItem();
        Pedido pedido = tipo.crearPedido(direccion);
        try {
            controlador.registrarPedido(pedido);
        } catch (PersistenciaException e) {
            JOptionPane.showMessageDialog(this, e.getMensajeConDetalle(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this, "Pedido #" + pedido.getIdPedido() + " registrado correctamente.",
                "Pedido registrado", JOptionPane.INFORMATION_MESSAGE);
        txtDireccion.setText("");
        cmbTipo.setSelectedIndex(0);
    }

    /**
     * Muestra un mensaje de validación.
     *
     * @param mensaje motivo por el que no se pudo guardar el pedido
     */
    private void mostrarAdvertencia(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Dato inválido", JOptionPane.WARNING_MESSAGE);
    }
}
