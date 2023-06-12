package com.niton.jsx.parsing.model;

import com.niton.parser.ast.LocatableReducedNode;
import com.niton.parser.ast.ReducedNode;

public sealed interface JsxElement permits JsxComponentTag, JsxExpressionNode, JsxHtmlTag, JsxTag, JsxTextNode {
    LocatableReducedNode ast();
}
