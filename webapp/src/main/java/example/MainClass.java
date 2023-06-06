package example;

import com.niton.jsx.JsxComponents;
import org.teavm.jso.dom.html.HTMLDocument;

public class MainClass {
    static HTMLDocument document = HTMLDocument.current();

    public static void main(String[] args)  {
        document.getBody().appendChild(JsxComponents.App());
    }
}
