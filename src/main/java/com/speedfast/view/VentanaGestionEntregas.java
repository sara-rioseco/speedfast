package com.speedfast.view;

import com.speedfast.exception.OperacionNoPermitidaException;
import com.speedfast.exception.PersistenciaException;
import com.speedfast.model.Entrega;
import com.speedfast.model.Pedido;
import com.speedfast.model.Repartidor;
import com.speedfast.service.ControladorDeEnvios;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Ventana de gestión de entregas: permite registrar, editar, eliminar y listar
 * entregas, con filtros opcionales por pedido y por repartidor.
 *
 * <p>El pedido y el repartidor se eligen desde combos cargados desde la base
 * de datos, que muestran un texto legible (por ejemplo "3 - Ñuñoa (PENDIENTE)")
 * pero conservan internamente el ID (ver {@link OpcionCombo}). Los combos se
 * actualizan cada vez que se crean, editan o eliminan pedidos o repartidores,
 * desde cualquier ventana.</p>
 *
 * <p>Registrar una entrega deja su pedido en reparto, y eliminar la entrega
 * más reciente de un pedido en reparto lo devuelve a pendiente. Al editar una
 * entrega se pueden corregir el repartidor, la fecha y la hora, pero no el
 * pedido: la entrega forma parte de su historial.</p>
 */
public class VentanaGestionEntregas extends VentanaGestion<VentanaGestionEntregas.Datos> {

    /** Fecha en formato día-mes-año. El modo estricto rechaza fechas inexistentes, como el 31-02-2026. */
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT);

    /** Hora en formato de 24 horas. Al escribirla, los segundos son opcionales. */
    private static final DateTimeFormatter FORMATO_HORA =
            DateTimeFormatter.ofPattern("HH:mm[:ss]").withResolverStyle(ResolverStyle.STRICT);

    /** Explica por qué el combo de pedido se deshabilita al editar una entrega. */
    private static final String AYUDA_PEDIDO_FIJO = "El pedido de una entrega registrada no se modifica: "
            + "para asociarla a otro pedido, elimínala y registra una nueva.";

    /**
     * Datos que muestra la ventana, consultados juntos en segundo plano.
     *
     * @param entregas     entregas que cumplen los filtros
     * @param pedidos      pedidos para los combos
     * @param repartidores repartidores para los combos
     */
    record Datos(List<Entrega> entregas, List<Pedido> pedidos, List<Repartidor> repartidores) {
    }

    /**
     * Datos del formulario ya validados.
     *
     * @param idPedido     ID del pedido seleccionado
     * @param idRepartidor ID del repartidor seleccionado
     * @param fecha        fecha ingresada
     * @param hora         hora ingresada
     */
    private record Formulario(int idPedido, int idRepartidor, LocalDate fecha, LocalTime hora) {
    }

    private final JComboBox<OpcionCombo<Integer>> cmbPedido = new JComboBox<>();
    private final JComboBox<OpcionCombo<Integer>> cmbRepartidor = new JComboBox<>();
    private final JTextField txtFecha = new JTextField(10);
    private final JTextField txtHora = new JTextField(8);

    private final JComboBox<OpcionCombo<Integer>> cmbFiltroPedido = new JComboBox<>();
    private final JComboBox<OpcionCombo<Integer>> cmbFiltroRepartidor = new JComboBox<>();

    /** Entregas que muestra la tabla, en el mismo orden que sus filas. */
    private List<Entrega> entregas = List.of();

    /**
     * Crea la ventana y carga las entregas, pedidos y repartidores guardados.
     *
     * @param controlador controlador compartido
     */
    public VentanaGestionEntregas(ControladorDeEnvios controlador) {
        super("Gestión de entregas", "Datos de la entrega", "la entrega", controlador,
                "ID", "Pedido", "Estado del pedido", "Repartidor", "Fecha", "Hora");

        JPanel campos = new JPanel(new GridBagLayout());
        agregarCampo(campos, "Pedido:", cmbPedido);
        agregarCampo(campos, "Repartidor:", cmbRepartidor);
        agregarCampo(campos, "Fecha (dd-mm-aaaa):", txtFecha);
        agregarCampo(campos, "Hora (hh:mm:ss):", txtHora);

        // Ancho fijo para que una dirección larga no ensanche la ventana.
        OpcionCombo<Integer> prototipo = new OpcionCombo<>(0, "000 - Dirección de ejemplo (EN_REPARTO)");
        cmbFiltroPedido.setPrototypeDisplayValue(prototipo);
        cmbFiltroRepartidor.setPrototypeDisplayValue(prototipo);
        recargarAlCambiar(cmbFiltroPedido);
        recargarAlCambiar(cmbFiltroRepartidor);

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.add(new JLabel("Pedido:"));
        filtros.add(cmbFiltroPedido);
        filtros.add(new JLabel("Repartidor:"));
        filtros.add(cmbFiltroRepartidor);

        construir(campos, filtros);
    }

    @Override
    protected Callable<Datos> crearConsulta() {
        Integer idPedido = OpcionCombo.valorSeleccionado(cmbFiltroPedido);
        Integer idRepartidor = OpcionCombo.valorSeleccionado(cmbFiltroRepartidor);
        return () -> new Datos(
                controlador.consultarEntregas(idPedido, idRepartidor),
                controlador.consultarPedidos(),
                controlador.consultarRepartidores());
    }

    @Override
    protected void mostrarDatos(Datos datos) {
        List<OpcionCombo<Integer>> opcionesPedidos = new ArrayList<>();
        for (Pedido pedido : datos.pedidos()) {
            opcionesPedidos.add(new OpcionCombo<>(pedido.getIdPedido(),
                    textoPedido(pedido) + " (" + pedido.getEstado() + ")"));
        }
        List<OpcionCombo<Integer>> opcionesRepartidores = new ArrayList<>();
        for (Repartidor repartidor : datos.repartidores()) {
            opcionesRepartidores.add(new OpcionCombo<>(repartidor.getIdRepartidor(), textoRepartidor(repartidor)));
        }

        OpcionCombo.reemplazarOpciones(cmbPedido, opcionesPedidos);
        OpcionCombo.reemplazarOpciones(cmbRepartidor, opcionesRepartidores);
        boolean filtroPedidoVigente = OpcionCombo.reemplazarOpciones(cmbFiltroPedido, conTodos(opcionesPedidos));
        boolean filtroRepartidorVigente =
                OpcionCombo.reemplazarOpciones(cmbFiltroRepartidor, conTodos(opcionesRepartidores));

        entregas = datos.entregas();
        modelo.setRowCount(0);
        for (Entrega entrega : entregas) {
            modelo.addRow(new Object[]{
                    entrega.getIdEntrega(),
                    textoPedido(entrega.getPedido()),
                    entrega.getPedido().getEstado(),
                    textoRepartidor(entrega.getRepartidor()),
                    entrega.getFecha().format(FORMATO_FECHA),
                    entrega.getHora().format(FORMATO_HORA)});
        }

        // Si se eliminó el pedido o repartidor filtrado, el filtro vuelve a "Todos" y la tabla se consulta de nuevo.
        if (!filtroPedidoVigente || !filtroRepartidorVigente) {
            recargar();
        }
    }

    @Override
    protected void cargarEnFormulario(int fila) {
        Entrega entrega = entregas.get(fila);
        OpcionCombo.seleccionarValor(cmbPedido, entrega.getPedido().getIdPedido());
        OpcionCombo.seleccionarValor(cmbRepartidor, entrega.getRepartidor().getIdRepartidor());
        txtFecha.setText(entrega.getFecha().format(FORMATO_FECHA));
        txtHora.setText(entrega.getHora().format(FORMATO_HORA));
        cmbPedido.setEnabled(false);
        cmbPedido.setToolTipText(AYUDA_PEDIDO_FIJO);
    }

    @Override
    protected void limpiarFormulario() {
        cmbPedido.setEnabled(true);
        cmbPedido.setToolTipText(null);
        txtFecha.setText(LocalDate.now().format(FORMATO_FECHA));
        txtHora.setText(LocalTime.now().truncatedTo(ChronoUnit.SECONDS).format(FORMATO_HORA));
    }

    @Override
    protected void registrar() {
        Formulario datos = leerFormulario();
        if (datos == null) {
            return;
        }
        Entrega entrega;
        try {
            entrega = controlador.registrarEntrega(datos.idPedido(), datos.idRepartidor(), datos.fecha(), datos.hora());
        } catch (PersistenciaException | OperacionNoPermitidaException e) {
            mostrarFallo(e);
            return;
        }
        mostrarExito("Entrega #" + entrega.getIdEntrega() + " registrada correctamente.\n"
                + "El pedido #" + datos.idPedido() + " quedó EN_REPARTO con "
                + entrega.getRepartidor().getNombreCompleto() + ".");
        limpiar();
    }

    @Override
    protected void guardarCambios(int id) {
        Formulario datos = leerFormulario();
        if (datos == null) {
            return;
        }
        try {
            controlador.actualizarEntrega(id, datos.idRepartidor(), datos.fecha(), datos.hora());
        } catch (PersistenciaException | OperacionNoPermitidaException e) {
            mostrarFallo(e);
            return;
        }
        mostrarExito("Entrega #" + id + " actualizada correctamente.");
    }

    @Override
    protected void eliminar(int id) {
        if (!confirmar("¿Eliminar la entrega #" + id + "?")) {
            return;
        }
        boolean pedidoVuelveAPendiente;
        try {
            pedidoVuelveAPendiente = controlador.eliminarEntrega(id);
        } catch (PersistenciaException | OperacionNoPermitidaException e) {
            mostrarFallo(e);
            return;
        }
        mostrarExito("Entrega #" + id + " eliminada correctamente."
                + (pedidoVuelveAPendiente
                        ? "\nSu pedido volvió a PENDIENTE, ya que no queda un repartidor que lo lleve." : ""));
        limpiar();
    }

    /**
     * Valida los campos del formulario: pedido y repartidor seleccionados, y
     * fecha y hora obligatorias y con un formato válido.
     *
     * @return los datos validados, o {@code null} si alguno no es válido (el motivo ya se informó)
     */
    private Formulario leerFormulario() {
        Integer idPedido = OpcionCombo.valorSeleccionado(cmbPedido);
        if (idPedido == null) {
            mostrarAdvertencia("Debes seleccionar el pedido de la entrega.\n"
                    + "Si no hay pedidos, regístralos primero en Gestión de pedidos.");
            return null;
        }
        Integer idRepartidor = OpcionCombo.valorSeleccionado(cmbRepartidor);
        if (idRepartidor == null) {
            mostrarAdvertencia("Debes seleccionar el repartidor de la entrega.\n"
                    + "Si no hay repartidores, regístralos primero en Gestión de repartidores.");
            return null;
        }
        LocalDate fecha = leerFecha();
        if (fecha == null) {
            return null;
        }
        LocalTime hora = leerHora();
        if (hora == null) {
            return null;
        }
        return new Formulario(idPedido, idRepartidor, fecha, hora);
    }

    /**
     * Lee la fecha del formulario.
     *
     * @return la fecha, o {@code null} si está vacía o no es válida (el motivo ya se informó)
     */
    private LocalDate leerFecha() {
        String texto = txtFecha.getText().trim();
        if (texto.isEmpty()) {
            mostrarAdvertencia("Debes ingresar la fecha de la entrega.");
        } else {
            try {
                return LocalDate.parse(texto, FORMATO_FECHA);
            } catch (DateTimeParseException e) {
                mostrarAdvertencia("La fecha debe ser válida y tener el formato dd-mm-aaaa, por ejemplo 05-10-2026.");
            }
        }
        txtFecha.requestFocusInWindow();
        return null;
    }

    /**
     * Lee la hora del formulario.
     *
     * @return la hora, o {@code null} si está vacía o no es válida (el motivo ya se informó)
     */
    private LocalTime leerHora() {
        String texto = txtHora.getText().trim();
        if (texto.isEmpty()) {
            mostrarAdvertencia("Debes ingresar la hora de la entrega.");
        } else {
            try {
                return LocalTime.parse(texto, FORMATO_HORA);
            } catch (DateTimeParseException e) {
                mostrarAdvertencia("La hora debe ser válida y tener el formato hh:mm o hh:mm:ss "
                        + "(24 horas), por ejemplo 14:30.");
            }
        }
        txtHora.requestFocusInWindow();
        return null;
    }

    /**
     * @param pedido pedido a mostrar
     * @return el ID y la dirección del pedido, por ejemplo "3 - Ñuñoa"
     */
    private static String textoPedido(Pedido pedido) {
        return pedido.getIdPedido() + " - " + pedido.getDireccionEntrega();
    }

    /**
     * @param repartidor repartidor a mostrar
     * @return el ID y el nombre del repartidor, por ejemplo "1 - Juan Pérez"
     */
    private static String textoRepartidor(Repartidor repartidor) {
        return repartidor.getIdRepartidor() + " - " + repartidor.getNombreCompleto();
    }

    /**
     * Agrega la opción "Todos" al inicio de las opciones de un filtro.
     *
     * @param opciones opciones del filtro
     * @return una lista nueva con "Todos" y las opciones
     */
    private static List<OpcionCombo<Integer>> conTodos(List<OpcionCombo<Integer>> opciones) {
        List<OpcionCombo<Integer>> resultado = new ArrayList<>();
        resultado.add(OpcionCombo.todos());
        resultado.addAll(opciones);
        return resultado;
    }
}
