package eu.niton.signaling;

import eu.niton.signaling.TodoApp.TodoItem;
import eu.niton.signaling.swing.ReactiveSwing;
import eu.nitonfx.signaling.api.Context;
import eu.nitonfx.signaling.api.EffectHandle;
import eu.nitonfx.signaling.api.ListSignal;
import eu.nitonfx.signaling.processors.elementbuilder.Element;
import eu.nitonfx.signaling.processors.reactiveproxy.Reactive;

import javax.swing.*;
import java.util.Collection;
import java.util.function.Supplier;

import static eu.nitonfx.signaling.processors.reactiveproxy.ProxyFactory.create;

public class TodoAppWithDebuggingSymbols {
    private static final Context cx = Context.create();
    private static final ReactiveSwing jsx = new ReactiveSwing(cx);
    private static Collection<? extends EffectHandle> effectHandles;
    public static void main(String[] args) {
        cx.setPostEffectExecutionHook(effect -> {
            System.out.println("Execute:\n"+effect.formatAsTree());
        });
        effectHandles = cx.run(() -> {
            var frame = jsx.JFrame();
            frame.getContentPane().add(App().get());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 450);
            frame.setVisible(true);
        });
    }

    private static Element<JPanel> App() {
        var todos = cx.<TodoItem>createListSignal();
        final var pane = jsx.JPanel();
        pane.setLayout(new BoxLayout(pane.get(), BoxLayout.Y_AXIS));
        var label = jsx.JLabel();
        label.setText(()->todos.size() + " TODOs").name("set todo count text");
        pane.add(label).name("add todo count label");
        pane.add(TodoInput(todos)).name("add todo count component");
        pane.add(TodoList(todos)).name("add todo list component");

        return pane;
    }

    private static Element<JPanel> TodoList(ListSignal<TodoItem> todos) {
        var pane = jsx.JPanel();
        pane.setLayout(new BoxLayout(pane.get(), BoxLayout.Y_AXIS)).name("set todo list layout");
        todos.onAdd((todo, index) -> {
            var todoItem = TodoItem(todo, () -> todos.remove(todo.getUntracked()));
            pane.add(todoItem).name("add todo item component");
            effectHandles.forEach(it->System.out.println(it.formatAsTree()));
        }).name("on new todo item");
        return pane;
    }

    private static Element<JPanel> TodoItem(Supplier<TodoItem> todo, Runnable onRemove) {
        var item = jsx.JPanel();

        var checkbox = jsx.JCheckBox();
        checkbox.setSelected(()-> todo.get().done()).name("update checkbox state");
        checkbox.addActionListener(e -> todo.get().setDone(checkbox.isSelected()));
        item.add(checkbox).name("Add checkbox to todo item component");

        var label = jsx.JLabel();
        label.setText(()->todo.get().text()).name("set todo item label text");
        label.setEnabled(() -> !todo.get().done()).name("set item label enabled state");
        item.add(label).name("add item name label");


        var deleteButton = jsx.JButton(new JButton("remove"));
        deleteButton.addActionListener(e -> onRemove.run());
        item.add(deleteButton).name("add delete item button");

        return item;
    }

    private static Element<JPanel> TodoInput(ListSignal<TodoItem> todos) {
        var pane = jsx.JPanel();
        var input = jsx.JTextField();
        input.setColumns(20);
        var button = jsx.JButton(new JButton("Add"));
        button.addActionListener(e -> todos.add(new ReactiveTodoItem(cx, input.getText(), false)));
        pane.add(input).name("add todo text input");
        pane.add(button).name("add todo button");
        return pane;
    }
}
