package com.speedfast.view;

import com.speedfast.exception.OperacionNoPermitidaException;
import com.speedfast.exception.PersistenciaException;
import com.speedfast.model.EstadoPedido;
import com.speedfast.model.Pedido;
import com.speedfast.model.TipoPedido;
import com.speedfast.service.ControladorDeEnvios;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Ventana de gestión de pedidos: permite registrar, editar, eliminar y listar
 * pedidos, con filtros opcionales por estado y por tipo.
 *
 * <p>El formulario solicita los datos que guarda la tabla {@code pedidos}:
 * dirección, tipo y estado. El ID lo asigna la base de datos. Los demás datos
 * que exige cada tipo de pedido se completan con los valores estándar de
 * {@link TipoPedido}.</p>
 *
 * <p>La tabla muestra también el repartidor de la última entrega de cada
 * pedido, y se actualiza sola mientras avanzan las entregas.</p>
 */
public class VentanaGestionPedidos extends VentanaGestion<List<Pedido>> {

    private final JTextField txtDireccion = new JTextField(30);
    private final JComboBox<TipoPedido> cmbTipo = new JComboBox<>(TipoPedido.values());
    private final JComboBox<EstadoPedido> cmbEstado = new JComboBox<>(EstadoPedido.registrables());

    private final JComboBox<OpcionCombo<EstadoPedido>> cmbFiltroEstado = new JComboBox<>();
    private final JComboBox<OpcionCombo<TipoPedido>> cmbFiltroTipo = new JComboBox<>();

    /** Pedidos que muestra la tabla, en el mismo orden que sus filas. */
    private List<Pedido> pedidos = List.of();

    /**
     * Crea la ventana y carga los pedidos guardados.
     *
     * @param controlador controlador compartido
     */
    public VentanaGestionPedidos(ControladorDeEnvios controlador) {
        super("Gestión de pedidos", "Datos del pedido", "el pedido", controlador,
                "ID", "Tipo", "Dirección", "Estado", "Repartidor");

        JPanel campos = new JPanel(new GridBagLayout());
        agregarCampo(campos, "Dirección:", txtDireccion);
        agregarCampo(campos, "Tipo:", cmbTipo);
        agregarCampo(campos, "Estado:", cmbEstado);

        cmbFiltroEstado.addItem(OpcionCombo.todos());
        for (EstadoPedido estado : EstadoPedido.registrables()) {
            cmbFiltroEstado.addItem(new OpcionCombo<>(estado, estado.name()));
        }
        cmbFiltroTipo.addItem(OpcionCombo.todos());
        for (TipoPedido tipo : TipoPedido.values()) {
            cmbFiltroTipo.addItem(new OpcionCombo<>(tipo, tipo.name()));
        }
        recargarAlCambiar(cmbFiltroEstado);
        recargarAlCambiar(cmbFiltroTipo);

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.add(new JLabel("Estado:"));
        filtros.add(cmbFiltroEstado);
        filtros.add(new JLabel("Tipo:"));
        filtros.add(cmbFiltroTipo);

        construir(campos, filtros);
    }

    @Override
    protected Callable<List<Pedido>> crearConsulta() {
        EstadoPedido estado = OpcionCombo.valorSeleccionado(cmbFiltroEstado);
        TipoPedido tipo = OpcionCombo.valorSeleccionado(cmbFiltroTipo);
        return () -> controlador.consultarPedidos(estado, tipo);
    }

    @Override
    protected void mostrarDatos(List<Pedido> pedidos) {
        this.pedidos = pedidos;
        modelo.setRowCount(0);
        for (Pedido pedido : pedidos) {
            modelo.addRow(new Object[]{
                    pedido.getIdPedido(),
                    pedido.getTipo(),
                    pedido.getDireccionEntrega(),
                    pedido.getEstado(),
                    pedido.getRepartidorAsignado() == null
                            ? "Sin asignar"
                            : pedido.getRepartidorAsignado().getNombreCompleto()});
        }
    }

    @Override
    protected void cargarEnFormulario(int fila) {
        Pedido pedido = pedidos.get(fila);
        txtDireccion.setText(pedido.getDireccionEntrega());
        cmbTipo.setSelectedItem(pedido.getTipo());
        cmbEstado.setSelectedItem(pedido.getEstado());
    }

    @Override
    protected void limpiarFormulario() {
        txtDireccion.setText("");
        cmbTipo.setSelectedIndex(0);
        cmbEstado.setSelectedItem(EstadoPedido.PENDIENTE);
        txtDireccion.requestFocusInWindow();
    }

    @Override
    protected void registrar() {
        Pedido pedido = leerFormulario(0);
        if (pedido == null) {
            return;
        }
        try {
            controlador.registrarPedido(pedido);
        } catch (PersistenciaException e) {
            mostrarFallo(e);
            return;
        }
        mostrarExito("Pedido #" + pedido.getIdPedido() + " registrado correctamente.");
        limpiar();
    }

    @Override
    protected void guardarCambios(int id) {
        Pedido pedido = leerFormulario(id);
        if (pedido == null) {
            return;
        }
        try {
            controlador.actualizarPedido(pedido);
        } catch (PersistenciaException | OperacionNoPermitidaException e) {
            mostrarFallo(e);
            return;
        }
        mostrarExito("Pedido #" + id + " actualizado correctamente.");
    }

    @Override
    protected void eliminar(int id) {
        if (!confirmar("¿Eliminar el pedido #" + id + "?\n"
                + "Si tiene entregas registradas, también se eliminarán.")) {
            return;
        }
        int entregasEliminadas;
        try {
            entregasEliminadas = controlador.eliminarPedido(id);
        } catch (PersistenciaException | OperacionNoPermitidaException e) {
            mostrarFallo(e);
            return;
        }
        mostrarExito("Pedido #" + id + " eliminado correctamente."
                + (entregasEliminadas > 0 ? "\nSe eliminaron también sus " + entregasEliminadas + " entrega(s)." : ""));
        limpiar();
    }

    /**
     * Valida los campos del formulario y crea el pedido que representan. La
     * dirección se valida con la misma regla del modelo
     * ({@link Pedido#validarDireccion(String)}): obligatoria y de hasta 100 caracteres.
     *
     * @param idPedido ID del pedido, o 0 si es nuevo
     * @return el pedido, o {@code null} si algún dato no es válido (el motivo ya se informó)
     */
    private Pedido leerFormulario(int idPedido) {
        String direccion;
        try {
            direccion = Pedido.validarDireccion(txtDireccion.getText());
        } catch (IllegalArgumentException e) {
            mostrarAdvertencia(e.getMessage());
            txtDireccion.requestFocusInWindow();
            return null;
        }
        TipoPedido tipo = (TipoPedido) cmbTipo.getSelectedItem();
        EstadoPedido estado = (EstadoPedido) cmbEstado.getSelectedItem();
        if (tipo == null || estado == null) {
            mostrarAdvertencia("Debes seleccionar el tipo y el estado del pedido.");
            return null;
        }

        Pedido pedido = tipo.crearPedido(idPedido, direccion);
        pedido.setEstado(estado);
        return pedido;
    }
}
