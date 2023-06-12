package com.niton.jsx.parsing.model;

import com.niton.parser.ast.LocatableReducedNode;

import java.util.Map;

public record JsxHtmlTag(
        LocatableReducedNode ast,
        String name,
        boolean selfClosing,
        Map<String, String> staticAttributes,
        Map<String, String> dynamicAttributes,
        Map<String, String> eventHandlers,
        JsxElement[] children
) implements JsxElement, JsxTag {
}
