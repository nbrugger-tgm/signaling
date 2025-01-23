package eu.niton.signaling;

import eu.nitonfx.signaling.api.Context;
import eu.nitonfx.signaling.api.ListSignal;
import eu.nitonfx.signaling.api.Signal;
import eu.nitonfx.signaling.api.SignalLike;

import javax.swing.*;
import java.util.function.Function;
import java.util.function.Supplier;

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

    private record TodoItem(String text, boolean done) { }

    private static JPanel app() {
        var todos = cx.createSignal(new TodoItem[0]);
        final var pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        var label = new JLabel();
        cx.createEffect(() -> label.setText(todos.size() + " TODOs"));
        pane.add(label);

        var adder = todoInput(todos);
        pane.add(adder);

        var todoList = todoList(todos);
       pane.add(todoList);

        return pane;
    }
    private static JPanel todoList(ListSignal<TodoItem> todos) {
        var pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        insert(todos, App::todoItem, pane);
        return pane;
    }

    private static JPanel todoItem(Signal<TodoItem> todo) {
        var item = new JPanel();
        var label = new JLabel();
        var checkbox = new JCheckBox();
        cx.createEffect(()->{
            checkbox.setSelected(todo.get().done());
            label.setText(todo.get().text());
            label.setEnabled(!todo.get().done());
        });
        checkbox.addActionListener(e -> todo.set(new TodoItem(todo.get().text(), checkbox.isSelected())));
        item.add(checkbox);
        item.add(label);
        return item;
    }

    private static JPanel todoInput(ListSignal<TodoItem> todos) {
        var pane = new JPanel();
        var input = new JTextField();
        input.setColumns(20);
        var button = new JButton("Add");
        button.addActionListener(e -> todos.add(new TodoItem(input.getText(), false)));
        pane.add(input);
        pane.add(button);
        return pane;
    }

    private static<T extends JComponent> void insert(Supplier<T> element, JComponent parent) {
        cx.createEffect(()->{
            var base = element.get();
            parent.add(base);
            parent.validate();
            cx.cleanup(()->parent.remove(base));
        });
    }
    private static<T,E extends JComponent> void insert(ListSignal<T> elements, Function<SignalLike<T>,E> mapper, JComponent parent) {
        cx.createEffect(()-> {
            for (int i = 0; i < elements.size(); i++) {
                var elem = elements.getSignal(i);
                insert(() -> mapper.apply(elem), parent);
            }
        });
    }
}
