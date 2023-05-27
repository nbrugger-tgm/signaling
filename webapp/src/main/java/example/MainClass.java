package example;

import org.teavm.jso.dom.html.HTMLDocument;

import static com.niton.jsx.JsxComponents.App;

public class MainClass {
    static HTMLDocument document = HTMLDocument.current();

    public static void main(String[] args)  {
        document.getBody().appendChild(App());
    }
}
