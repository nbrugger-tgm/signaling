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
    private static int i = 0;

    public static void main(String[] args) {
        cx.createEffect(() -> {
            JFrame frame = new JFrame("App");
            frame.getContentPane().add(app());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 450);
            frame.setVisible(true);
        });
    }

    private static JPanel app() {
        System.out.println("App.app");
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
        System.out.println("App.todoList");
        var pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        insert(
                todos,
                it -> todoItem(it, () -> todos.remove(it.get())),
                pane
        );
        return pane;
    }

    private static JPanel todoItem(SignalLike<TodoItem> todo, Runnable onRemove) {
        System.out.println("App.todoItem");
        var item = new JPanel();
        var label = new JLabel();
        var checkbox = new JCheckBox();
        var deleteButton = new JButton("remove");
        deleteButton.addActionListener(e -> onRemove.run());

        cx.createEffect(() -> {
            var todoItem = todo.get();
            checkbox.setSelected(todoItem.done().get());
            label.setText(todoItem.text().get());
            label.setEnabled(!todoItem.done().get());
        });
        checkbox.addActionListener(e -> todo.get().done().set(checkbox.isSelected()));
        item.add(checkbox);
        item.add(label);
        item.add(deleteButton);
        return item;
    }

    private static JPanel todoInput(ListSignal<TodoItem> todos) {
        System.out.println("App.todoInput");
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

    private static <T, E extends JComponent> void insert(ListSignal<T> elements, Function<? super SignalLike<T>, E> mapper, JComponent parent) {
        elements.onAdd((element, index) -> insert(() -> mapper.apply(element), parent));
    }

    private record TodoItem(Signal<String> text, Signal<Boolean> done) {
        TodoItem(String text, boolean done) {
            this(cx.createSignal(text), cx.createSignal(done));
        }
    }
}
