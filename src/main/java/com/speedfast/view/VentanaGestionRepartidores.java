package com.speedfast.view;

import com.speedfast.exception.OperacionNoPermitidaException;
import com.speedfast.exception.PersistenciaException;
import com.speedfast.model.Repartidor;
import com.speedfast.service.ControladorDeEnvios;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Ventana de gestión de repartidores: permite registrar, editar, eliminar y
 * listar repartidores.
 *
 * <p>La tabla {@code repartidores} solo guarda el nombre, por lo que es el
 * único dato que se solicita. Un repartidor nuevo participa desde la próxima
 * ronda de entregas. Un repartidor con entregas registradas no puede
 * eliminarse.</p>
 */
public class VentanaGestionRepartidores extends VentanaGestion<List<Repartidor>> {

    private final JTextField txtNombre = new JTextField(30);

    /** Repartidores que muestra la tabla, en el mismo orden que sus filas. */
    private List<Repartidor> repartidores = List.of();

    /**
     * Crea la ventana y carga los repartidores guardados.
     *
     * @param controlador controlador compartido
     */
    public VentanaGestionRepartidores(ControladorDeEnvios controlador) {
        super("Gestión de repartidores", "Datos del repartidor", "el repartidor", controlador,
                "ID", "Nombre");

        JPanel campos = new JPanel(new GridBagLayout());
        agregarCampo(campos, "Nombre completo:", txtNombre);

        construir(campos, null);
    }

    @Override
    protected Callable<List<Repartidor>> crearConsulta() {
        return controlador::consultarRepartidores;
    }

    @Override
    protected void mostrarDatos(List<Repartidor> repartidores) {
        this.repartidores = repartidores;
        modelo.setRowCount(0);
        for (Repartidor repartidor : repartidores) {
            modelo.addRow(new Object[]{repartidor.getIdRepartidor(), repartidor.getNombreCompleto()});
        }
    }

    @Override
    protected void cargarEnFormulario(int fila) {
        txtNombre.setText(repartidores.get(fila).getNombreCompleto());
    }

    @Override
    protected void limpiarFormulario() {
        txtNombre.setText("");
        txtNombre.requestFocusInWindow();
    }

    @Override
    protected void registrar() {
        Repartidor repartidor = leerFormulario(0);
        if (repartidor == null) {
            return;
        }
        try {
            controlador.registrarRepartidor(repartidor);
        } catch (PersistenciaException e) {
            mostrarFallo(e);
            return;
        }
        mostrarExito("Repartidor #" + repartidor.getIdRepartidor() + " registrado correctamente.\n"
                + "Participará desde la próxima ronda de entregas.");
        limpiar();
    }

    @Override
    protected void guardarCambios(int id) {
        Repartidor repartidor = leerFormulario(id);
        if (repartidor == null) {
            return;
        }
        try {
            controlador.actualizarRepartidor(repartidor);
        } catch (PersistenciaException | OperacionNoPermitidaException e) {
            mostrarFallo(e);
            return;
        }
        mostrarExito("Repartidor #" + id + " actualizado correctamente.");
    }

    @Override
    protected void eliminar(int id) {
        if (!confirmar("¿Eliminar el repartidor #" + id + "?")) {
            return;
        }
        try {
            controlador.eliminarRepartidor(id);
        } catch (PersistenciaException | OperacionNoPermitidaException e) {
            mostrarFallo(e);
            return;
        }
        mostrarExito("Repartidor #" + id + " eliminado correctamente.");
        limpiar();
    }

    /**
     * Valida el nombre del formulario y crea el repartidor que representa. El
     * nombre se valida con la misma regla del modelo
     * ({@link Repartidor#validarNombre(String)}): obligatorio y de hasta 100 caracteres.
     *
     * @param idRepartidor ID del repartidor, o 0 si es nuevo
     * @return el repartidor, o {@code null} si el nombre no es válido (el motivo ya se informó)
     */
    private Repartidor leerFormulario(int idRepartidor) {
        try {
            return new Repartidor(idRepartidor, Repartidor.validarNombre(txtNombre.getText()));
        } catch (IllegalArgumentException e) {
            mostrarAdvertencia(e.getMessage());
            txtNombre.requestFocusInWindow();
            return null;
        }
    }
}
