package eu.niton.signaling;

import eu.niton.signaling.swing.ReactiveJPanelBuilder;
import eu.niton.signaling.swing.ReactiveSwing;
import eu.nitonfx.signaling.api.Context;
import eu.nitonfx.signaling.api.MapSignal;
import eu.nitonfx.signaling.api.SetSignal;
import eu.nitonfx.signaling.processors.elementbuilder.Element;

import javax.swing.*;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class MapApp {
    private static final Context cx = Context.create();
    private static final ReactiveSwing jsx = new ReactiveSwing(cx);

    public static void main(String[] args) {
        cx.run(() -> {
            var frame = jsx.JFrame();
            frame.getContentPane().add(MapExample().get());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 450);
            frame.setVisible(true);
        });
    }
    private static Element<JPanel> MapExample(){
        var pane = jsx.JPanel();
        var table = cx.createSignal(Map.of(
                1, "Franz",
                2, "John",
                3, "Claire"
        ));
        pane.setLayout(new BoxLayout(pane.get(), BoxLayout.Y_AXIS));
        pane.add(new JLabel("Map Example"));

        pane.add(MapLookupTable(table.keySetSignal(), table::get));
        pane.add(Table(table));
        pane.add(MapInserter(table::put));

        return pane;
    }

    private static ReactiveJPanelBuilder MapLookupTable(SetSignal<Integer> keys, Function<Supplier<Integer>, Supplier<String>> lookupFunction) {
        var lookupKey = cx.createSignal(1);

        var lookupPanel = jsx.JPanel();
        lookupPanel.add(new JLabel("Lookup"));

        var valueChoice = jsx.<Integer>JComboBox();
        valueChoice.setModel(createComboBoxModel(keys));
        valueChoice.addItemListener(a -> lookupKey.set((int) a.getItem()));
        lookupPanel.add(valueChoice);

        var valueLabel = jsx.JLabel();
        valueLabel.setText(lookupFunction.apply(lookupKey));
        lookupPanel.add(valueLabel);
        return lookupPanel;
    }

    private static DefaultComboBoxModel<Integer> createComboBoxModel(SetSignal<Integer> keys) {
        var model = new DefaultComboBoxModel<Integer>();
        keys.onAdd(key -> {
            model.addElement(key);
            cx.cleanup(()->model.removeElement(key));
        });
        return model;
    }

    private static ReactiveJPanelBuilder Table(MapSignal<Integer, String> table) {
        var tablePane = jsx.JPanel();
        tablePane.setLayout(new BoxLayout(tablePane.get(), BoxLayout.Y_AXIS));
        table.onPut((key, value)->{
            tablePane.add(new JLabel(key+": " + value.get()),key);
        });
        return tablePane;
    }

    private static ReactiveJPanelBuilder MapInserter(BiConsumer<Integer, String> onAdd) {
        var insertPanel = jsx.JPanel();
        var keyInput = jsx.JSpinner();
        var valueInput = jsx.JTextField();
        valueInput.setColumns(12);
        var button = jsx.JButton(new JButton("Insert"));
        button.addActionListener(e -> {
            onAdd.accept((int) keyInput.getValue(), valueInput.getText());
        });
        insertPanel.add(keyInput);
        insertPanel.add(valueInput);
        insertPanel.add(button);
        return insertPanel;
    }
}
