package com.niton.jsx;

import eu.nitonfx.signaling.api.Context;
import eu.nitonfx.signaling.api.Signal;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.xml.Node;
import org.teavm.jso.dom.xml.Text;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class JSX {
    private final HTMLDocument doc;
    private final Context cx;

    public JSX(HTMLDocument doc, Context cx) {
        this.doc = doc;
        this.cx = cx;
    }

    public HTMLElement div(HTMLElement... children) {
        return containerComponent("div", children);
    }

    private HTMLElement containerComponent(String tag, HTMLElement[] children) {
        var div = doc.createElement(tag);
        for (HTMLElement child : children) {
            div.appendChild(child);
        }
        return div;
    }

    public HTMLElement span(Supplier<?> text) {
        return textComponent("span", text);
    }

    public HTMLElement span(String text) {
        return span(() -> text);
    }

    private HTMLElement textComponent(String tag, Supplier<?> text) {
        var span = doc.createElement(tag);
        cx.createEffect(
                () -> span.setInnerText(text.get().toString())
        );
        return span;
    }

    public HTMLElement button(Supplier<?> count, Consumer<? super Event> listener) {
        var btn = doc.createElement("button");
        cx.createEffect(() -> {
            btn.setInnerText(count.get().toString());
        });
        btn.listenClick(listener::accept);
        return btn;
    }

    public HTMLElement h1(String text) {
        return h1(() -> text);
    }

    public HTMLElement h2(String text) {
        return h2(() -> text);
    }

    public HTMLElement h3(String text) {
        return h3(() -> text);
    }

    public HTMLElement h1(Supplier<String> text) {
        return textComponent("h1", text);
    }

    public HTMLElement h2(Supplier<String> text) {
        return textComponent("h2", text);
    }

    public HTMLElement h3(Supplier<String> text) {
        return textComponent("h3", text);
    }

    public HTMLElement input(Signal<String> jsxText) {
        HTMLInputElement input = (HTMLInputElement) doc.createElement("input");
        input.listenKeyUp(e -> jsxText.set(((HTMLInputElement) e.getTarget()).getValue()));
        cx.createEffect(() -> {
            input.setValue(jsxText.get());
        });
        return input;
    }

    public HTMLElement generic(
            String tag,
            Map<String, String> staticAttributes,
            Map<String, Supplier<?>> dynamicAttributes,
            Map<String, Consumer<Event>> eventHandlers,
            List<Node> childNodes) {
        var element = doc.createElement(tag);
        System.out.println("create container "+tag);
        applyAttributes(staticAttributes, dynamicAttributes, eventHandlers, element);
        for (Node childNode : childNodes) {
            element.appendChild(childNode);
        }
        return element;
    }
    public HTMLElement generic(
            String tag,
            Map<String, String> staticAttributes,
            Map<String, Supplier<?>> dynamicAttributes,
            Map<String, Consumer<Event>> eventHandlers) {
        var element = doc.createElement(tag);
        applyAttributes(staticAttributes, dynamicAttributes, eventHandlers, element);
        return element;
    }

    public void applyAttributes(Map<String, String> staticAttributes, Map<String, Supplier<?>> dynamicAttributes, Map<String, Consumer<Event>> eventHandlers, HTMLElement element) {
        staticAttributes.forEach(element::setAttribute);
        eventHandlers.forEach((event, handler) -> element.addEventListener(event, handler::accept));
        for (Map.Entry<String, Supplier<?>> entry : dynamicAttributes.entrySet()) {
            String attr = entry.getKey();
            Supplier<?> value = entry.getValue();
            if(attr.equals("innerHtml")) {
                cx.createEffect(() -> element.setInnerHTML(value.get().toString()));
            }else {
                cx.createEffect(() -> element.setAttribute(attr, value.get().toString()));
            }
        }
    }

    public Text text(String txt){
        return doc.createTextNode(txt);
    }
    public Text text(Supplier<?> txt){
        var text = doc.createTextNode("");
        cx.createEffect(()->text.setTextContent(txt.get().toString()));
        return text;
    }

    public Node pre(String message) {
        return textComponent("pre", () -> message);
    }
}
