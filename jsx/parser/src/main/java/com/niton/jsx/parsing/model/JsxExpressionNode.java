package com.niton.jsx.parsing.model;

import com.niton.parser.ast.LocatableReducedNode;
import com.niton.parser.ast.ReducedNode;

public record JsxExpressionNode(
        LocatableReducedNode ast,
        String expression
) implements JsxElement{
}
