package com.niton.jsx.parsing.model;

import java.util.Map;

public sealed interface JsxTag extends JsxElement permits JsxHtmlTag, JsxComponentTag {
    String name();
    Map<String, String> staticAttributes();
    Map<String, String> dynamicAttributes();
}
