package eu.niton.signaling.swing;

import eu.nitonfx.signaling.api.Context;

import javax.swing.*;
import java.awt.*;

public class SwingExtension {
    public static void add(Context cx, Container element, Component comp) {
        cx.createEffect(() -> {
            element.add(comp);
            element.revalidate();
            cx.cleanup(() -> {
                element.remove(comp);
                element.revalidate();
            });
        });
    }


    public static void add(Context cx, Container element, String name, Component comp) {
        cx.createEffect(() -> {
            element.add(name, comp);
            element.revalidate();
            cx.cleanup(() -> {
                element.remove(comp);
                element.revalidate();
            });
        });
    }

    public static void add(Context cx, Container element, Component comp, int index) {
        cx.createEffect(() -> {
            element.add(comp, index);
            element.revalidate();
            cx.cleanup(() -> {
                element.remove(comp);
                element.revalidate();
            });
        });
    }

    public static void add(Context cx, Container element, Component comp, Object constraints) {
        cx.createEffect(() -> {
            element.add(comp, constraints);
            element.revalidate();
            cx.cleanup(() -> {
                element.remove(comp);
                element.revalidate();
            });
        });
    }

    public static void add(Context cx, Container element, Component comp, Object constraints, int index) {
        cx.createEffect(() -> {
            element.add(comp, constraints, index);
            element.revalidate();
            cx.cleanup(() -> {
                element.remove(comp);
                element.revalidate();
            });
        });
    }

    public static<E> void setModel(Context cx, JComboBox<E> element, ComboBoxModel<E> comp) {
        element.setModel(comp);
        var toSelect = element.getSelectedItem();
        element.setSelectedItem(null);
        element.setSelectedItem(toSelect);
    }
}
