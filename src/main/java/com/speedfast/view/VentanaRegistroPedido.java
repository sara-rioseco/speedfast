package com.speedfast.view;

import com.speedfast.model.Pedido;
import com.speedfast.model.PedidoComida;
import com.speedfast.model.PedidoEncomienda;
import com.speedfast.model.PedidoExpress;
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
 * Formulario para registrar un nuevo pedido a partir de su ID, dirección y
 * tipo. Al presionar Guardar, los datos se validan antes de crear el pedido y
 * agregarlo al {@link ControladorDeEnvios} compartido.
 *
 * <p>Los demás datos que exige cada tipo de pedido (distancia, peso, tienda,
 * etc.) se completan con valores estándar, definidos como constantes.</p>
 */
public class VentanaRegistroPedido extends JFrame {

    /** Distancia estándar hasta el destino, en kilómetros. */
    private static final float DISTANCIA_KM = 3.0f;

    /** Peso estándar de una encomienda, en kilogramos. */
    private static final float PESO_ENCOMIENDA_KG = 5.0f;

    /** Radio de cobertura estándar de una compra express, en kilómetros. */
    private static final float RADIO_EXPRESS_KM = 3.0f;

    /** Controlador compartido donde se registran los pedidos. */
    private final ControladorDeEnvios controlador;

    private final JTextField txtId = new JTextField(15);
    private final JTextField txtDireccion = new JTextField(15);
    private final JComboBox<String> cmbTipo = new JComboBox<>(new String[]{"Comida", "Encomienda", "Express"});

    /**
     * Crea el formulario de registro.
     *
     * @param controlador controlador compartido donde se registrarán los pedidos
     */
    public VentanaRegistroPedido(ControladorDeEnvios controlador) {
        super("Registrar pedido");
        this.controlador = controlador;

        JPanel formulario = new JPanel(new GridLayout(3, 2, 10, 10));
        formulario.add(new JLabel("ID:"));
        formulario.add(txtId);
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
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    /**
     * Valida los campos, crea el pedido del tipo seleccionado y lo registra en
     * el controlador. Informa el resultado con un {@link JOptionPane}.
     */
    private void guardarPedido() {
        String textoId = txtId.getText().trim();
        String direccion = txtDireccion.getText().trim();

        if (textoId.isEmpty() || direccion.isEmpty()) {
            mostrarError("Debes completar el ID y la dirección.");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(textoId);
        } catch (NumberFormatException e) {
            mostrarError("El ID debe ser un número entero.");
            return;
        }
        if (id <= 0) {
            mostrarError("El ID debe ser mayor que cero.");
            return;
        }

        try {
            controlador.registrarPedido(crearPedido(id, direccion, (String) cmbTipo.getSelectedItem()));
        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
            return;
        }

        JOptionPane.showMessageDialog(this, "Pedido #" + id + " registrado correctamente.",
                "Pedido registrado", JOptionPane.INFORMATION_MESSAGE);
        txtId.setText("");
        txtDireccion.setText("");
        cmbTipo.setSelectedIndex(0);
    }

    /**
     * Crea el pedido que corresponde al tipo elegido en el combo.
     *
     * @param id        identificador del pedido
     * @param direccion dirección de entrega
     * @param tipo      tipo seleccionado: comida, encomienda o express
     * @return el pedido creado
     */
    private static Pedido crearPedido(int id, String direccion, String tipo) {
        return switch (tipo) {
            case "Encomienda" -> new PedidoEncomienda(id, direccion, DISTANCIA_KM, PESO_ENCOMIENDA_KG, "Caja");
            case "Express" -> new PedidoExpress(id, direccion, DISTANCIA_KM, "Tienda asociada", RADIO_EXPRESS_KM);
            default -> new PedidoComida(id, direccion, DISTANCIA_KM, "Restaurante asociado", 1);
        };
    }

    /**
     * Muestra un mensaje de validación.
     *
     * @param mensaje motivo por el que no se pudo guardar el pedido
     */
    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Dato inválido", JOptionPane.WARNING_MESSAGE);
    }
}
