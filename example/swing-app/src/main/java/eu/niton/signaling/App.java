package eu.niton.signaling;

import eu.nitonfx.signaling.api.Context;
import eu.nitonfx.signaling.api.ListSignal;
import eu.nitonfx.signaling.api.Signal;

import javax.swing.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class App {
    private static final Context cx = Context.create();

    public static void main(String[] args) {
        JFrame frame = new JFrame("App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 450);
        frame.setVisible(true);
        frame.getContentPane().add(app());
    }

    private static JPanel app() {
        var todos = cx.<TodoItem>createListSignal();
        final var pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        var label = new JLabel();
        cx.createEffect(() -> label.setText(todos.size() + " TODOs"));
        pane.add(label);
        pane.add(todoInput(todos));
        pane.add(todoList(todos));

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
        cx.createEffect(() -> {
            var todoItem = todo.get();
            cx.createEffect(() -> checkbox.setSelected(todoItem.isDone()));
            cx.createEffect(() -> label.setText(todoItem.getText()));
            cx.createEffect(() -> label.setEnabled(!todoItem.isDone()));
        });
        checkbox.addActionListener(e -> todo.get().done.set(checkbox.isSelected()));
        item.add(checkbox);
        item.add(label);
        return item;
    }

    private static JPanel todoInput(List<TodoItem> todos) {
        var pane = new JPanel();
        var input = new JTextField();
        input.setColumns(20);
        var button = new JButton("Add");
        button.addActionListener(e -> todos.add(new TodoItem(input.getText(), false)));
        pane.add(input);
        pane.add(button);
        return pane;
    }

    private static <T extends JComponent> void insert(Supplier<T> element, JComponent parent) {
        cx.createEffect(() -> {
            var base = element.get();
            parent.add(base);
            parent.validate();
            cx.cleanup(() -> parent.remove(base));
        });
    }

    private static <T, E extends JComponent> void insert(ListSignal<T> elements, Function<Signal<T>, E> mapper, JComponent parent) {
        cx.createEffect(() -> {
            for (int i = 0; i < elements.size(); i++) {
                var elem = elements.getSignal(i);
                insert(() -> mapper.apply(elem), parent);
            }
        });
    }

    private record TodoItem(Signal<String> text, Signal<Boolean> done) {
        TodoItem(String text, boolean done){
            this(cx.createSignal(text), cx.createSignal(done));
        }

        public boolean isDone() {
            return done.get();
        }

        public String getText() {
            return text.get();
        }
    }
}
