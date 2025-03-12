package eu.niton.signaling;

import eu.niton.signaling.swing.ReactiveSwing;
import eu.nitonfx.signaling.api.Context;
import eu.nitonfx.signaling.api.ListSignal;
import eu.nitonfx.signaling.processors.elementbuilder.Element;
import eu.nitonfx.signaling.processors.reactiveproxy.Reactive;

import javax.swing.*;
import java.util.function.Supplier;

import static eu.nitonfx.signaling.processors.reactiveproxy.ProxyFactory.create;

public class App {
    private static final Context cx = Context.create();
    private static final ReactiveSwing jsx = new ReactiveSwing(cx);

    public static void main(String[] args) {
        cx.run(() -> {
            var frame = jsx.JFrame();
            frame.getContentPane().add(App().get());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 450);
            frame.setVisible(true);
        });
    }

    private static Element<JPanel> App() {
        System.out.println("App.app");
        var todos = cx.<TodoItem>createListSignal();
        final var pane = jsx.JPanel();
        pane.setLayout(new BoxLayout(pane.get(), BoxLayout.Y_AXIS));
        var label = jsx.JLabel();
        label.setText(()->todos.size() + " TODOs");
        pane.add(label);
        pane.add(TodoInput(todos));
        pane.add(TodoList(todos));

        return pane;
    }

    private static Element<JPanel> TodoList(ListSignal<TodoItem> todos) {
        System.out.println("App.todoList");
        var pane = jsx.JPanel();
        pane.setLayout(new BoxLayout(pane.get(), BoxLayout.Y_AXIS));
        todos.onAdd((todo, index) -> {
            var todoItem = TodoItem(todo, () -> todos.remove(todo.getUntracked()));
            pane.add(todoItem);
        });
        return pane;
    }

    private static Element<JPanel> TodoItem(Supplier<TodoItem> todo, Runnable onRemove) {
        System.out.println("App.todoItem");
        var item = jsx.JPanel();

        var checkbox = jsx.JCheckBox();
        checkbox.setSelected(()-> todo.get().done());
        checkbox.addActionListener(e -> todo.get().setDone(checkbox.isSelected()));
        item.add(checkbox);

        var label = jsx.JLabel();
        label.setText(todo.get().text());
        label.setEnabled(() -> !todo.get().done());
        item.add(label);


        var deleteButton = jsx.JButton(new JButton("remove"));
        deleteButton.addActionListener(e -> onRemove.run());
        item.add(deleteButton);

        return item;
    }

    private static Element<JPanel> TodoInput(ListSignal<TodoItem> todos) {
        System.out.println("App.todoInput");
        var pane = jsx.JPanel();
        var input = jsx.JTextField();
        input.setColumns(20);
        var button = jsx.JButton(new JButton("Add"));
        button.addActionListener(e -> todos.add(create(cx,new TodoItem$Init(input.getText(), false))));
        pane.add(input);
        pane.add(button);
        return pane;
    }

    @Reactive
    interface TodoItem {
        boolean done();
        String text();
        void setText(String text);
        void setDone(boolean done);
    }
}
