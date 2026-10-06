package com.speedfast.view;

import javax.swing.JComboBox;
import java.util.List;
import java.util.Objects;

/**
 * Opción de un {@link JComboBox}: muestra un texto legible y conserva
 * internamente el valor que representa. En los combos de pedidos y
 * repartidores el valor es el ID (por ejemplo, la opción "3 - Ñuñoa" conserva
 * el ID 3), y en los filtros la opción "Todos" tiene valor {@code null}.
 *
 * <p>Incluye además las operaciones que comparten todos los combos de la
 * interfaz: leer el valor seleccionado, seleccionar un valor y reemplazar las
 * opciones cuando cambian los datos.</p>
 *
 * @param valor valor que representa la opción
 * @param texto texto que muestra el combo
 * @param <T>   tipo del valor
 */
public record OpcionCombo<T>(T valor, String texto) {

    /**
     * Crea la opción "Todos" de un filtro, cuyo valor {@code null} indica que
     * el filtro no se aplica.
     *
     * @param <T> tipo del valor de las demás opciones del combo
     * @return la opción "Todos"
     */
    public static <T> OpcionCombo<T> todos() {
        return new OpcionCombo<>(null, "Todos");
    }

    /**
     * El combo muestra el {@code toString()} de cada opción.
     *
     * @return el texto de la opción
     */
    @Override
    public String toString() {
        return texto;
    }

    /**
     * Obtiene el valor de la opción seleccionada en un combo.
     *
     * @param combo combo a consultar
     * @param <T>   tipo del valor
     * @return el valor seleccionado, o {@code null} si no hay selección o se eligió "Todos"
     */
    public static <T> T valorSeleccionado(JComboBox<OpcionCombo<T>> combo) {
        OpcionCombo<T> opcion = combo.getItemAt(combo.getSelectedIndex());
        return opcion == null ? null : opcion.valor();
    }

    /**
     * Selecciona en un combo la opción que tiene el valor indicado.
     *
     * @param combo combo en el que se selecciona
     * @param valor valor buscado
     * @param <T>   tipo del valor
     * @return {@code true} si alguna opción tenía ese valor
     */
    public static <T> boolean seleccionarValor(JComboBox<OpcionCombo<T>> combo, T valor) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (Objects.equals(combo.getItemAt(i).valor(), valor)) {
                combo.setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Reemplaza las opciones de un combo, conservando la opción que estaba
     * seleccionada si sigue existiendo. Se usa cada vez que se crean, editan o
     * eliminan pedidos o repartidores.
     *
     * @param combo    combo a actualizar
     * @param opciones opciones nuevas
     * @param <T>      tipo del valor
     * @return {@code true} si la opción seleccionada se conservó
     */
    public static <T> boolean reemplazarOpciones(JComboBox<OpcionCombo<T>> combo, List<OpcionCombo<T>> opciones) {
        T seleccionado = valorSeleccionado(combo);
        combo.removeAllItems();
        for (OpcionCombo<T> opcion : opciones) {
            combo.addItem(opcion);
        }
        return seleccionarValor(combo, seleccionado);
    }
}
