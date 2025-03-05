package eu.niton.signaling;

import eu.nitonfx.signaling.api.Context;
import eu.nitonfx.signaling.api.ListSignal;
import eu.nitonfx.signaling.api.Signal;
import eu.nitonfx.signaling.api.SignalLike;

import javax.swing.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class App {
    private static final Context cx = Context.create();
    private static int i = 0;

    public static void main(String[] args) {
        cx.run(() -> {
            JFrame frame = new JFrame("App");
            frame.getContentPane().add(app());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 450);
            frame.setVisible(true);
        });
    }

    private static JPanel app() {
        System.out.println("App.app");
        var todos = cx.<TodoItem>createListSignal();
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

    private static JPanel todoItem(Supplier<TodoItem> todo, Runnable onRemove) {
        System.out.println("App.todoItem");
        var item = new JPanel();

        var checkbox = new JCheckBox();
        set(checkbox::setSelected, todo.get().done());
        checkbox.addActionListener(e -> todo.get().done().set(checkbox.isSelected()));
        item.add(checkbox);

        var label = new JLabel();
        set(label::setText, todo.get().text());
        set(label::setEnabled, todo.get().done(), bool -> !bool);
        item.add(label);


        var deleteButton = new JButton("remove");
        deleteButton.addActionListener(e -> onRemove.run());
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

    private static <T> void set(Consumer<T> setter, Supplier<T> signal) {
        cx.createEffect(() -> setter.accept(signal.get()));
    }
    private static <T,R> void set(Consumer<R> setter, Supplier<T> signal, Function<T, R> mapping) {
        var memo = cx.createMemo(()-> mapping.apply(signal.get()));
        cx.createEffect(() -> setter.accept(memo.get()));
    }
    private record TodoItem(Signal<String> text, Signal<Boolean> done) {
        TodoItem(String text, boolean done) {
            this(cx.createSignal(text), cx.createSignal(done));
        }
    }
}
