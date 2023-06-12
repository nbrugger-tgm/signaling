package example;

import com.niton.jsx.Component;
import com.niton.jsx.JsxComponent;
import com.niton.jsx.parsing.JsxParser;
import com.niton.parser.ast.AstNode;
import com.niton.parser.exceptions.InterpretationException;
import com.niton.parser.exceptions.ParsingException;
import eu.nitonfx.signaling.api.Context;
import eu.nitonfx.signaling.api.Signal;
import java.util.function.Supplier;

@JsxComponent("""
            <div>
                <input onKeyup={(e) -> jsxText.set((( org.teavm.jso.dom.html.HTMLInputElement)((org.teavm.jso.dom.events.KeyboardEvent)e).getTarget()).getValue())} value={jsxText}/>
                <div innerHtml={jsxTree}/>
                <pre>{parseError}</pre>
            </div>
        """)
public class JsxParserComponent implements Component {

    public Signal<String> jsxText;
    public Supplier<String> jsxTree;
    public Signal<String> parseError;

    @Override
    public void initialize(Context cx) {
        jsxText = cx.createSignal("<div>JSX</div>");
        parseError = cx.createSignal("");
        System.out.println("Parsing2: " + jsxText);
        jsxTree = ()->{
            try {
                System.out.println("Parsing: " + jsxText.get());
                var parsed = new JsxParser().parse(jsxText.get());
                parseError.set("");
                return parsed.ast().formatHtml();
            }catch (ParsingException e) {
                var probable = e.getMostProminentDeepException();
                var location = AstNode.Location.oneChar(probable.getLine(), probable.getColumn());
                parseError.set(location.markInText(jsxText.get(), 2, probable.getMessage()));
                return "Syntax Error";
            }catch (InterpretationException e){
                parseError.set(e.getSyntaxErrorMessage(jsxText.get()));
                return "Syntax Error";
            }
        };
    }
}
