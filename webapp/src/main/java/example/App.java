package example;

import com.niton.jsx.Component;
import com.niton.jsx.JsxComponent;
import eu.nitonfx.signaling.api.Context;
import eu.nitonfx.signaling.api.Signal;

@JsxComponent("""
    <div style="bg-blue">
        <span>You clicked {count} times!</span>
        <button onClick={e -> count.update(c -> c + 1)}>Click me</button>
        <JsxParserComponent/>
        
    </div>
""")//<Dropdown/>
public class App implements Component {
    public Signal<Integer> count;

    @Override
    public void initialize(Context cx) {
        count = cx.createSignal(0);
        cx.createEffect(()->{
            System.out.println("Count: " + count.get());
        });
    }
}
