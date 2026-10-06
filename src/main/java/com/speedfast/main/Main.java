package com.speedfast.main;

import com.speedfast.dao.ConexionDB;
import com.speedfast.exception.PersistenciaException;
import com.speedfast.service.ControladorDeEnvios;
import com.speedfast.service.SimuladorEntregas;
import com.speedfast.service.ZonaDeCarga;
import com.speedfast.view.VentanaPrincipal;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Punto de entrada de SpeedFast. Arma el sistema —zona de carga, controlador y
 * simulador de entregas—, comprueba que la base de datos esté configurada y
 * disponible, y abre la {@link VentanaPrincipal}, desde la cual el usuario
 * gestiona pedidos, repartidores y entregas e inicia las entregas concurrentes.
 */
public class Main {

    /**
     * Punto de entrada de la aplicación.
     *
     * @param args argumentos de línea de comandos (no se utilizan)
     */
    public static void main(String[] args) {
        ZonaDeCarga zonaDeCarga = new ZonaDeCarga();
        ControladorDeEnvios controlador = new ControladorDeEnvios(zonaDeCarga);
        SimuladorEntregas simulador = new SimuladorEntregas(zonaDeCarga, controlador);

        // Swing exige que las ventanas se creen y modifiquen desde su propio hilo (EDT).
        SwingUtilities.invokeLater(() -> {
            aplicarAspectoDelSistema();
            if (!ConexionDB.estaConfigurada()) {
                informarFaltaDeContrasena();
                return;
            }
            try {
                controlador.verificarBaseDeDatos();
            } catch (PersistenciaException e) {
                informarErrorDeConexion(e);
                return;
            }
            new VentanaPrincipal(controlador, simulador).setVisible(true);
        });
    }

    /**
     * Informa que falta configurar la contraseña de la base de datos y cómo
     * hacerlo. Sin ella la aplicación no puede conectarse, por lo que se
     * cierra tras el aviso.
     */
    private static void informarFaltaDeContrasena() {
        String variable = ConexionDB.VARIABLE_PASSWORD;
        System.out.println("[Sistema] Falta la variable de entorno " + variable + " con la contraseña de MySQL.");
        JOptionPane.showMessageDialog(null,
                "No se configuró la contraseña de la base de datos."
                        + "\n\nPor seguridad, la contraseña no se escribe en el código: define la variable"
                        + "\nde entorno " + variable + " y vuelve a iniciar la aplicación."
                        + "\n\n  • IntelliJ IDEA: Run → Edit Configurations… → Environment variables"
                        + "\n  • PowerShell: $env:" + variable + "=\"tu_contraseña\""
                        + "\n  • bash: export " + variable + "=tu_contraseña",
                "SpeedFast — Configuración", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Informa que no fue posible acceder a la base de datos. Sin ella la
     * aplicación no puede funcionar, por lo que se cierra tras el aviso.
     *
     * @param e excepción con el motivo y el detalle del error
     */
    private static void informarErrorDeConexion(PersistenciaException e) {
        System.out.println("[Sistema] " + e.getMensajeConDetalle().replace('\n', ' '));
        JOptionPane.showMessageDialog(null,
                e.getMensajeConDetalle()
                        + "\n\nConexión: " + ConexionDB.getUrl()
                        + "\n\nVerifica que:"
                        + "\n  • El servidor MySQL esté en ejecución."
                        + "\n  • La base de datos se haya creado con sql/01_crear_base_datos.sql."
                        + "\n  • La contraseña de " + ConexionDB.VARIABLE_PASSWORD + " sea correcta.",
                "SpeedFast — Base de datos", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Usa el aspecto visual del sistema operativo, para que las ventanas se
     * vean familiares al usuario. Si no está disponible, Swing mantiene su
     * aspecto por defecto.
     */
    private static void aplicarAspectoDelSistema() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
            System.out.println("[Sistema] Se usará el aspecto visual por defecto de Swing.");
        }
    }
}
