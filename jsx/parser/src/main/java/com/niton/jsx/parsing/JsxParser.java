package com.niton.jsx.parsing;

import com.niton.jsx.parsing.grammar.MatchingBracketGrammar;
import com.niton.jsx.parsing.model.*;
import com.niton.parser.Parser;
import com.niton.parser.ast.AstNode;
import com.niton.parser.ast.LocatableReducedNode;
import com.niton.parser.exceptions.InterpretationException;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarName;
import com.niton.parser.grammar.api.GrammarReferenceMap;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.niton.jsx.parsing.JsxParser.NodeType.*;
import static com.niton.parser.grammar.api.Grammar.*;
import static com.niton.parser.token.DefaultToken.*;

public class JsxParser extends Parser<JsxElement> {
    private static final Grammar<?> staticParameterValue = build(ParameterType.STATIC_VALUE)
            .token(DOUBLEQUOTE).add()
            .tokens(DOUBLEQUOTE, NEW_LINE).anyExcept().add("content")
            .token(DOUBLEQUOTE).add()
            .get();
    private static final Grammar<?> expressionParameterValue = MatchingBracketGrammar.from(ROUND_BRACKET_OPEN, ROUND_BRACKET_CLOSED).named(ParameterType.EXPRESSION_VALUE);
    private static final Grammar<?> parameter = build("parameter")
            .token(WHITESPACE).ignore().add()
            .token(LETTERS).add("name")
            .token(EQUAL).add()
            .grammars(staticParameterValue, expressionParameterValue).add("value")
            .get();
    private static final Grammar<?> selfClosingTag = build(SELF_CLOSING_TAG)
            .token(SMALLER).add()
            .tokens(LETTERS, NUMBER).repeat().add("name")
            .token(WHITESPACE).ignore().add()
            .grammar(parameter).repeat().add("parameters")
            .token(WHITESPACE).ignore().add()
            .tokens(SLASH).add()
            .token(BIGGER).add()
            .get();//<test name="test" dynamic={(var) => {var++}}/>
    private static final Grammar<?> tagWithChildren = build(CONTAINER_TAG)
            .token(SMALLER).add()
            .tokens(LETTERS, NUMBER).repeat().add("name")
            .grammar(parameter).repeat().add("parameters")
            .token(WHITESPACE).ignore().add("after-parameter whitespace")
            .token(BIGGER).add()
            .token(WHITESPACE).ignore().add("pre-content whitespace")
            .grammars(SELF_CLOSING_TAG, CONTAINER_TAG, EXPRESSION_NODE, TEXT_NODE).repeat().add("children")
            .token(SMALLER).add()
            .token(SLASH).add()
            .tokens(LETTERS, NUMBER).repeat().add("closingName")
            .token(BIGGER).add()
            .get();//<div name="test"><test name="test"/> mixed {expression} with text</div>
    private static final Grammar<?> anyTag = build("tag")
            .grammars(selfClosingTag.getName(), tagWithChildren.getName()).add()
            .get();
    private static final Grammar<?> textNode = anyExcept(anyOf(tokenReference(SMALLER), tokenReference(ROUND_BRACKET_OPEN))).named(TEXT_NODE);
    private static final Grammar<?> expressionNode = MatchingBracketGrammar.from(ROUND_BRACKET_OPEN, ROUND_BRACKET_CLOSED).named(EXPRESSION_NODE);

    public JsxParser() {
        super(
                new GrammarReferenceMap()
                        .map(staticParameterValue)
                        .map(expressionParameterValue)
                        .map(parameter)
                        .map(textNode)
                        .map(expressionNode)
                        .map(selfClosingTag)
                        .map(tagWithChildren)
                        .map(anyTag),
                anyTag.getName()
        );
    }

    @Override
    public @NotNull JsxElement convert(@NotNull AstNode o) throws ParsingException {
        var ast = o.reduce("jsx").orElseThrow();
        return transformNode(ast).orElseThrow();
    }

    private @NotNull Optional<JsxElement> transformNode(LocatableReducedNode jsxAst) {
        var nodeType = jsxAst.getSubNode("type").orElseThrow().getValue();
        var nodeValue = jsxAst.getSubNode("value").orElseThrow();
        return switch (JsxParser.NodeType.valueOf(nodeType)) {
            case TEXT_NODE -> transformTextNode(nodeValue);
            case EXPRESSION_NODE -> Optional.of(transformExpressionNode(nodeValue));
            case CONTAINER_TAG, SELF_CLOSING_TAG -> Optional.of(transformTagNode(nodeValue));
        };
    }

    private JsxElement transformTagNode(LocatableReducedNode nodeValue) {
        var parameters = nodeValue.getSubNode("parameters").orElseThrow().getChildren();
        var tagName = nodeValue.getSubNode("name").orElseThrow().join();
        nodeValue.getSubNode("closingName").ifPresent(closingName -> {
            if (!Objects.equals(closingName.join(), tagName))
                throw new InterpretationException(String.format("Closing tag name does not match opening tag name (%s)",tagName), closingName, 1);
        });
        var staticParameters = new HashMap<String, String>(parameters.size());
        var dynamicParameters = new HashMap<String, String>(parameters.size());
        var eventListeners = new HashMap<String, String>(parameters.size());
        transformParameters(parameters, staticParameters, dynamicParameters, eventListeners);

        if (Character.isUpperCase(tagName.charAt(0))) {
            return transformComponentNode(nodeValue, tagName, staticParameters, dynamicParameters, eventListeners);
        }
        var children = nodeValue.getSubNode("children");
        var childNodes = children.stream().flatMap(childrenNode -> childrenNode.getChildren().stream().map(this::transformNode).flatMap(Optional::stream));
        return new JsxHtmlTag(
                nodeValue, tagName,
                nodeValue.getSubNode("closingName").isEmpty(),
                staticParameters, dynamicParameters, eventListeners,
                childNodes.toArray(JsxElement[]::new)
        );
    }

    private JsxElement transformComponentNode(
            LocatableReducedNode ast,
            String tagName,
            HashMap<String, String> staticParameters,
            HashMap<String, String> dynamicParameters,
            HashMap<String, String> eventListeners
    ) {
        Map<String, String> allDynamic = new HashMap<>(dynamicParameters.size() + eventListeners.size());
        allDynamic.putAll(dynamicParameters);
        allDynamic.putAll(eventListeners);
        return new JsxComponentTag(ast, tagName, new JsxElement[0], staticParameters, allDynamic);
    }

    private void transformParameters(
            List<LocatableReducedNode> parameters,
            Map<String, String> staticParamers,
            Map<String, String> dynamicParameters,
            Map<String, String> eventListeners
    ) {
        for (LocatableReducedNode parameter : parameters) {
            var name = parameter.getSubNode("name").orElseThrow().getValue();
            var value = parameter.getSubNode("value").orElseThrow();
            var valueType = JsxParser.ParameterType.valueOf(value.getSubNode("type").orElseThrow().getValue());
            value = value.getSubNode("value").orElseThrow();
            switch (valueType) {
                case STATIC_VALUE -> staticParamers.put(name, value.getSubNode("content").orElseThrow().getValue());
                case EXPRESSION_VALUE -> {
                    var expression = value.getValue();
                    if (name.startsWith("on")) {
                        var withoutOn = name.substring(2);
                        withoutOn = Character.toLowerCase(withoutOn.charAt(0)) + withoutOn.substring(1);
                        eventListeners.put(withoutOn, expression);
                    } else {
                        dynamicParameters.put(name, "()->" + expression);
                    }
                }
            }
        }
    }

    private JsxElement transformExpressionNode(LocatableReducedNode nodeValueAst) {
        return new JsxExpressionNode(nodeValueAst, nodeValueAst.getValue());
    }

    private Optional<JsxElement> transformTextNode(LocatableReducedNode textNodeAst) {
        var value = textNodeAst.getValue().replace("\n", "").replace("\r", "");
        if (value.trim().isEmpty()) return Optional.empty();
        return Optional.of(new JsxTextNode(textNodeAst, value));
    }

    enum NodeType implements GrammarName {
        TEXT_NODE,
        EXPRESSION_NODE,
        CONTAINER_TAG,
        SELF_CLOSING_TAG;

        @Override
        public String getName() {
            return name();
        }
    }

    private enum ParameterType implements GrammarName {
        STATIC_VALUE,
        EXPRESSION_VALUE;

        @Override
        public String getName() {
            return name();
        }
    }
}
