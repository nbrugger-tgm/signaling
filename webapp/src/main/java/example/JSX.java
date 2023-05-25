package example;

import eu.nitonfx.signaling.api.Context;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

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

    private HTMLElement textComponent(String tag, Supplier<?> text) {
        var span = doc.createElement(tag);
        cx.createEffect(
                ()->span.setInnerText(text.get().toString())
        );
        return span;
    }

    public HTMLElement button(Supplier<?> count, Consumer<? super Event> listener) {
        var btn = doc.createElement("button");
        cx.createEffect(()->{
            btn.setInnerText(count.get().toString());
        });
        btn.listenClick(listener::accept);
        return btn;
    }

    public HTMLElement h1(String text) {
        return h1(()->text);
    }

    public HTMLElement h2(String text) {
        return h2(()->text);
    }

    public HTMLElement h3(String text) {
        return h3(()->text);
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
}
