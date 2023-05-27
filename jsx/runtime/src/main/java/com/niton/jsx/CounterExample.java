package com.niton.jsx;

import org.teavm.jso.dom.html.HTMLElement;

import java.util.List;
import java.util.Map;

public class CounterExample {
    public static HTMLElement render() {
        var jsx = new JSX(null,null);
        return jsx.generic("div", Map.of("style", "bg-blue"),Map.of(),Map.of(), List.of(
                jsx.generic("span", Map.of(),Map.of(),Map.of(), List.of(
                        jsx.text("Hello World")
                )),
                jsx.generic("button", Map.of(),Map.of(),Map.of(), List.of(
                        jsx.text("Click me")
                ))
        ));
    }
}
