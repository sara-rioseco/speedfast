package com.speedfast.view;

import com.speedfast.exception.PersistenciaException;
import com.speedfast.service.ControladorDeEnvios;
import com.speedfast.service.ObservadorDeCambios;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Base de las ventanas de gestión de pedidos, repartidores y entregas. Reúne
 * lo que las tres tienen en común, para no repetirlo en cada una:
 * <ul>
 *   <li><b>Estructura</b>: formulario con los botones Registrar, Guardar
 *       cambios, Eliminar y Limpiar; filtros opcionales; tabla con los
 *       registros y barra de estado.</li>
 *   <li><b>Modo del formulario</b>: al seleccionar una fila de la tabla, sus
 *       datos pasan al formulario para editarlos o eliminarlos; Limpiar
 *       vuelve al modo de registro.</li>
 *   <li><b>Actualización de los datos</b>: la ventana se registra como
 *       {@link ObservadorDeCambios} del controlador. Cada vez que los datos
 *       cambian (desde cualquier ventana, o por las entregas en curso), vuelve
 *       a consultar la base de datos con un {@link SwingWorker}, fuera del
 *       hilo gráfico, y actualiza su tabla y sus combos.</li>
 *   <li><b>Mensajes al usuario</b> con {@link JOptionPane}.</li>
 * </ul>
 *
 * <p>Cada subclase define sus campos, sus columnas y sus operaciones CRUD. La
 * primera columna de la tabla debe ser siempre el ID del registro.</p>
 *
 * @param <D> datos que la ventana consulta a la base de datos para mostrarse
 */
public abstract class VentanaGestion<D> extends JFrame implements ObservadorDeCambios {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Controlador compartido por todas las ventanas. */
    protected final ControladorDeEnvios controlador;

    /** Datos de la tabla. Sus celdas no son editables: los cambios se hacen desde el formulario. */
    protected final DefaultTableModel modelo;

    /** Tabla con los registros guardados. */
    protected final JTable tabla;

    /** Título del formulario, por ejemplo "Datos del pedido". */
    private final String tituloFormulario;

    /** Nombre del registro con su artículo, por ejemplo "el pedido", para el texto del modo de edición. */
    private final String nombreRegistro;

    private final JButton btnRegistrar = new JButton("Registrar");
    private final JButton btnGuardar = new JButton("Guardar cambios");
    private final JButton btnEliminar = new JButton("Eliminar");

    /** Indica si el formulario registra un dato nuevo o edita uno existente. */
    private final JLabel lblModo = new JLabel();

    /** Informa cuándo se actualizaron los datos por última vez, o si la consulta falló. */
    private final JLabel lblEstado = new JLabel(" ");

    /** ID del registro cargado en el formulario, o {@code null} si se está registrando uno nuevo. */
    private Integer idEnEdicion;

    /**
     * Indica que la ventana está cargando datos en la tabla y los combos, para
     * que los eventos que eso produce no se traten como acciones del usuario.
     */
    private boolean mostrandoDatos;

    /** Indica si hay una consulta en curso. Solo se usa desde el hilo gráfico. */
    private boolean consultaEnCurso;

    /** Indica si llegó otro aviso de cambios durante la consulta en curso. Solo se usa desde el hilo gráfico. */
    private boolean consultaPendiente;

    /**
     * Prepara la ventana. La subclase crea sus campos y luego invoca
     * {@link #construir(JComponent, JComponent)} para armarla.
     *
     * @param titulo           título de la ventana
     * @param tituloFormulario título del formulario, por ejemplo "Datos del pedido"
     * @param nombreRegistro   nombre del registro con su artículo, por ejemplo "el pedido"
     * @param controlador      controlador compartido
     * @param columnas         columnas de la tabla; la primera debe ser el ID
     */
    protected VentanaGestion(String titulo, String tituloFormulario, String nombreRegistro,
                             ControladorDeEnvios controlador, String... columnas) {
        super(titulo);
        this.controlador = controlador;
        this.tituloFormulario = tituloFormulario;
        this.nombreRegistro = nombreRegistro;
        this.modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        this.tabla = new JTable(modelo);
    }

    /**
     * Arma la ventana, la registra para recibir los avisos de cambios y carga
     * los datos por primera vez. La subclase lo invoca al final de su constructor.
     *
     * @param campos  panel con los campos del formulario
     * @param filtros panel con los filtros de la tabla, o {@code null} si no tiene
     */
    protected final void construir(JComponent campos, JComponent filtros) {
        btnRegistrar.addActionListener(e -> registrar());
        btnGuardar.addActionListener(e -> guardarCambios(idEnEdicion));
        btnEliminar.addActionListener(e -> eliminar(idEnEdicion));
        JButton btnLimpiar = new JButton("Limpiar");
        btnLimpiar.addActionListener(e -> limpiar());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.add(btnRegistrar);
        botones.add(btnGuardar);
        botones.add(btnEliminar);
        botones.add(btnLimpiar);

        JPanel formulario = new JPanel(new BorderLayout(0, 10));
        formulario.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(tituloFormulario),
                BorderFactory.createEmptyBorder(5, 10, 10, 10)));
        formulario.add(campos, BorderLayout.NORTH);
        formulario.add(lblModo, BorderLayout.CENTER);
        formulario.add(botones, BorderLayout.SOUTH);

        JPanel superior = new JPanel(new BorderLayout(0, 10));
        superior.add(formulario, BorderLayout.NORTH);
        if (filtros != null) {
            filtros.setBorder(BorderFactory.createTitledBorder("Filtros"));
            superior.add(filtros, BorderLayout.SOUTH);
        }

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getColumnModel().getColumn(0).setMaxWidth(50);
        tabla.setPreferredScrollableViewportSize(new Dimension(680, 220));
        tabla.getSelectionModel().addListSelectionListener(e -> {
            int fila = tabla.getSelectedRow();
            if (!e.getValueIsAdjusting() && !mostrandoDatos && fila >= 0) {
                idEnEdicion = (Integer) modelo.getValueAt(fila, 0);
                cargarEnFormulario(fila);
                actualizarModo();
            }
        });

        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.addActionListener(e -> recargar());
        JPanel barraEstado = new JPanel(new BorderLayout(10, 0));
        barraEstado.add(lblEstado, BorderLayout.CENTER);
        barraEstado.add(btnRefrescar, BorderLayout.EAST);

        JPanel contenido = new JPanel(new BorderLayout(0, 10));
        contenido.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contenido.add(superior, BorderLayout.NORTH);
        contenido.add(new JScrollPane(tabla), BorderLayout.CENTER);
        contenido.add(barraEstado, BorderLayout.SOUTH);

        setContentPane(contenido);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);

        // Al cerrar la ventana deja de recibir avisos, para no consultar datos que nadie verá.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                controlador.quitarObservador(VentanaGestion.this);
            }
        });
        controlador.agregarObservador(this);

        limpiar();
        recargar();
    }

    /**
     * Recibe el aviso de que los datos cambiaron. El aviso puede llegar desde
     * el hilo de un repartidor, por lo que la recarga se agenda en el hilo gráfico.
     */
    @Override
    public void datosActualizados() {
        SwingUtilities.invokeLater(this::recargar);
    }

    /**
     * Vuelve a consultar la base de datos en un hilo de fondo y, al terminar,
     * muestra los datos desde el hilo gráfico. Si ya hay una consulta en
     * curso, no se inicia otra: se repite una sola vez al terminar, para que
     * varios avisos seguidos no acumulen consultas.
     */
    protected final void recargar() {
        if (consultaEnCurso) {
            consultaPendiente = true;
            return;
        }
        consultaEnCurso = true;
        Callable<D> consulta = crearConsulta();

        new SwingWorker<D, Void>() {
            @Override
            protected D doInBackground() throws Exception {
                return consulta.call();
            }

            @Override
            protected void done() {
                consultaEnCurso = false;
                try {
                    mostrar(get());
                    lblEstado.setText("Datos actualizados desde la base de datos a las "
                            + LocalTime.now().format(FORMATO_HORA));
                    lblEstado.setToolTipText(null);
                } catch (ExecutionException e) {
                    // Sin diálogo: los avisos pueden llegar seguidos, y la etiqueta basta para informar.
                    Throwable causa = e.getCause();
                    lblEstado.setText("Sin conexión con la base de datos: " + causa.getMessage());
                    lblEstado.setToolTipText(causa instanceof PersistenciaException errorBD
                            ? errorBD.getMensajeConDetalle() : null);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (consultaPendiente) {
                    consultaPendiente = false;
                    recargar();
                }
            }
        }.execute();
    }

    /**
     * Muestra los datos consultados y vuelve a seleccionar la fila del
     * registro en edición, sin que eso reemplace lo que el usuario está
     * escribiendo en el formulario.
     *
     * @param datos datos consultados
     */
    private void mostrar(D datos) {
        mostrandoDatos = true;
        try {
            mostrarDatos(datos);
            for (int fila = 0; fila < modelo.getRowCount(); fila++) {
                if (modelo.getValueAt(fila, 0).equals(idEnEdicion)) {
                    tabla.setRowSelectionInterval(fila, fila);
                }
            }
        } finally {
            mostrandoDatos = false;
        }
    }

    /** Vuelve al modo de registro: limpia el formulario y la selección de la tabla. */
    protected final void limpiar() {
        idEnEdicion = null;
        tabla.clearSelection();
        limpiarFormulario();
        actualizarModo();
    }

    /** Habilita los botones y actualiza el texto según el formulario registre o edite. */
    private void actualizarModo() {
        boolean editando = idEnEdicion != null;
        btnRegistrar.setEnabled(!editando);
        btnGuardar.setEnabled(editando);
        btnEliminar.setEnabled(editando);
        getRootPane().setDefaultButton(editando ? btnGuardar : btnRegistrar);
        lblModo.setText(editando
                ? "Editando " + nombreRegistro + " #" + idEnEdicion
                        + ": modifica los datos y presiona Guardar cambios, o Eliminar."
                : "Completa los datos y presiona Registrar, o selecciona una fila para editarla o eliminarla.");
    }

    /**
     * Agrega una fila al formulario: la etiqueta a la izquierda y el campo a
     * la derecha, ocupando el ancho disponible.
     *
     * @param panel    panel de campos, con {@link GridBagLayout}
     * @param etiqueta texto de la etiqueta
     * @param campo    componente donde el usuario ingresa el dato
     */
    protected static void agregarCampo(JPanel panel, String etiqueta, JComponent campo) {
        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.gridy = panel.getComponentCount() / 2;
        restricciones.anchor = GridBagConstraints.WEST;
        restricciones.insets = new Insets(4, 0, 4, 10);
        panel.add(new JLabel(etiqueta), restricciones);

        restricciones.gridx = 1;
        restricciones.weightx = 1;
        restricciones.fill = GridBagConstraints.HORIZONTAL;
        restricciones.insets = new Insets(4, 0, 4, 0);
        panel.add(campo, restricciones);
    }

    /**
     * Hace que un combo de filtro vuelva a consultar la tabla al cambiar su
     * selección. Los cambios que produce la propia carga de datos se ignoran.
     *
     * @param filtro combo de filtro
     */
    protected void recargarAlCambiar(JComboBox<?> filtro) {
        filtro.addActionListener(e -> {
            if (!mostrandoDatos) {
                recargar();
            }
        });
    }

    /**
     * Informa un dato inválido del formulario.
     *
     * @param mensaje motivo por el que no se puede realizar la operación
     */
    protected void mostrarAdvertencia(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Dato inválido", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Informa que una operación se realizó correctamente.
     *
     * @param mensaje resultado de la operación
     */
    protected void mostrarExito(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Operación exitosa", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Informa por qué no se pudo realizar una operación: un error de la base
     * de datos se muestra como error, junto al detalle que entrega MySQL; una
     * regla del negocio, como advertencia.
     *
     * @param e excepción con el motivo
     */
    protected void mostrarFallo(Exception e) {
        if (e instanceof PersistenciaException errorBD) {
            JOptionPane.showMessageDialog(this, errorBD.getMensajeConDetalle(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Operación no permitida", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Pide al usuario que confirme una eliminación.
     *
     * @param mensaje pregunta a mostrar
     * @return {@code true} si el usuario confirmó
     */
    protected boolean confirmar(String mensaje) {
        return JOptionPane.showConfirmDialog(this, mensaje, "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /**
     * Crea, en el hilo gráfico, la consulta que se ejecutará en segundo plano.
     * Los filtros seleccionados se leen aquí, porque los componentes de Swing
     * solo deben usarse desde el hilo gráfico.
     *
     * @return la consulta a la base de datos
     */
    protected abstract Callable<D> crearConsulta();

    /**
     * Muestra en la tabla (y en los combos, si corresponde) los datos consultados.
     *
     * @param datos datos consultados
     */
    protected abstract void mostrarDatos(D datos);

    /**
     * Copia al formulario los datos de una fila seleccionada.
     *
     * @param fila fila seleccionada en la tabla
     */
    protected abstract void cargarEnFormulario(int fila);

    /** Deja el formulario listo para registrar un dato nuevo. */
    protected abstract void limpiarFormulario();

    /** Valida el formulario y registra un dato nuevo. */
    protected abstract void registrar();

    /**
     * Valida el formulario y guarda los cambios del registro en edición.
     *
     * @param id ID del registro en edición
     */
    protected abstract void guardarCambios(int id);

    /**
     * Pide confirmación y elimina el registro en edición.
     *
     * @param id ID del registro en edición
     */
    protected abstract void eliminar(int id);
}
