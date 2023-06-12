package com.niton.jsx.parsing.model;

import com.niton.parser.ast.LocatableReducedNode;

public record JsxTextNode(
        LocatableReducedNode ast,
        String text
) implements JsxElement{}
