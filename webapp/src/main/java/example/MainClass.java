package example;

import eu.nitonfx.signaling.api.Context;
import eu.nitonfx.signaling.api.Signal;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

public class MainClass {
    static Context cx = Context.create();
    static HTMLDocument document = HTMLDocument.current();
    static JSX jsx = new JSX(document, cx);
    public static void main(String[] args) {

        var count = cx.createSignal(0);
        var div = jsx.div(
                jsx.h1("Java TeaVM Counter"),
                Counter(count),
                jsx.span(()->" -> "+count.get())
        );

        document.getBody().appendChild(div);
    }

    private static HTMLElement Counter(Signal<Integer> count) {
        System.out.println("Create counter");
        return jsx.button(
                count,
                e -> count.update(c -> c+1)
        );
    }
}
