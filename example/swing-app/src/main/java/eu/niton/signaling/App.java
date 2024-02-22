package eu.niton.signaling;

import eu.nitonfx.signaling.api.Context;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class App {
    private static final Context cx = Context.create();
    private static   int i = 0;
    public static void main(String[] args) {
        cx.createEffect(()->{
            JFrame frame = new JFrame("App");
            frame.getContentPane().add(app());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 450);
            frame.setVisible(true);
        });
    }

    public static JPanel app() {
        var count = cx.createSignal(0);
        final var pane = new JPanel();
        var label = new JLabel();
        cx.createEffect(() -> label.setText(count.get().toString()));
        pane.add(label);
        var button = new JButton("Increment");
        button.addActionListener(e -> count.update(i -> i + 1));
        pane.add(button);
        insertStream(
                ()-> IntStream.range(0, count.get()).mapToObj(i -> new JButton("Button " + i)),
                comp -> {
                    System.out.println("Adding "+(i++));
                    pane.add(comp);
                },
                comp1 -> {
                    pane.remove(comp1);
                    System.out.println("Removing "+(i++));
                }
        );

        return pane;
    }
    public static<T> void insert(Supplier<T> element, Consumer<T> adder, Consumer<T> remover) {
        cx.createEffect(()->{
            var base = element.get();
            adder.accept(base);
            cx.cleanup(()->remover.accept(base));
        });
    }


    public static<T> void insertStream(Supplier<Stream<T>> element, Consumer<T> adder, Consumer<T> remover) {
        insertIter(()->element.get().toList(), adder, remover);
    }
    public static<T> void insertIter(Supplier<Iterable<T>> element, Consumer<T> adder, Consumer<T> remover) {
        cx.createEffect(()->element.get().forEach(t->insert(()->t, adder, remover)));
    }
}
