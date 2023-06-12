package com.niton.jsx.parsing.model;

import com.niton.parser.ast.LocatableReducedNode;
import com.niton.parser.ast.ReducedNode;

import java.util.Map;

public record JsxComponentTag(
        LocatableReducedNode ast,
        String componentName,
        JsxElement[] children,

        Map<String, String> staticAttributes,
        Map<String, String> dynamicAttributes
) implements JsxElement, JsxTag {
    @Override
    public String name() {
        return componentName;
    }
}
