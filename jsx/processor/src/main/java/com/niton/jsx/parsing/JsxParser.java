package com.niton.jsx.parsing;

import com.niton.parser.DefaultParser;
import com.niton.parser.grammar.GrammarReferenceMap;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarName;
import com.niton.parser.grammar.types.ChainGrammar;
import com.niton.parser.token.DefaultToken;
import com.niton.parser.token.Tokenable;

import static com.niton.jsx.parsing.JsxParser.NodeType.*;
import static com.niton.parser.grammar.api.Grammar.*;
import static com.niton.parser.token.DefaultToken.*;

public class JsxParser {
    public enum NodeType implements GrammarName {
        TEXT_NODE,
        EXPRESSION_NODE,
        CONTAINER_TAG,
        SELF_CLOSING_TAG;

        @Override
        public String getName() {
            return name();
        }
    }
    public enum ParameterType implements GrammarName {
        STATIC_VALUE,
        EXPRESSION_VALUE;

        @Override
        public String getName() {
            return name();
        }
    }
    private static DefaultParser parser;
    public static DefaultParser get() {
        if(parser != null)
           return parser;
        var staticParameterValue = ChainGrammar.build(ParameterType.STATIC_VALUE)
                .token(DefaultToken.DOUBLEQUOTE).add()
                .token(DefaultToken.DOUBLEQUOTE).anyExcept().add("content")
                .token(DefaultToken.DOUBLEQUOTE).add()
                .get();
        var expressionParameterValue = MatchingBracketGrammar.from(DefaultToken.ROUND_BRACKET_OPEN, DefaultToken.ROUND_BRACKET_CLOSED).named(ParameterType.EXPRESSION_VALUE);
        var parameter = ChainGrammar.build("parameter")
                .token(WHITESPACE).ignore().add()
                .token(LETTERS).add("name")
                .token(DefaultToken.EQUAL).add()
                .grammars(staticParameterValue, expressionParameterValue).add("value")
                .get();
        var textNode = anyExcept(anyOf(tokenReference(SMALLER), tokenReference(DefaultToken.ROUND_BRACKET_OPEN))).named(TEXT_NODE);
        var expressionNode = MatchingBracketGrammar.from(DefaultToken.ROUND_BRACKET_OPEN, DefaultToken.ROUND_BRACKET_CLOSED).named(EXPRESSION_NODE);
        var selfClosingTag = ChainGrammar.build(SELF_CLOSING_TAG)
                .token(SMALLER).add()
                .token(LETTERS).add("name")
                .token(WHITESPACE).ignore().add()
                .grammar(parameter).repeat().add("parameters")
                .token(WHITESPACE).ignore().add()
                .tokens(SLASH).add()
                .token(BIGGER).add()
                .get();
        var tagWithChildren = ChainGrammar.build(CONTAINER_TAG)
                .token(SMALLER).add()
                .token(LETTERS).add("name")
                .token(WHITESPACE).ignore().add()
                .grammar(parameter).repeat().add("parameters")
                .token(WHITESPACE).ignore().add()
                .token(BIGGER).add()
                .token(WHITESPACE).ignore().add()
                .grammars(SELF_CLOSING_TAG, CONTAINER_TAG, EXPRESSION_NODE, TEXT_NODE).repeat().add("children")
                .token(SMALLER).add()
                .token(SLASH).add()
                .token(LETTERS).add("closingName")
                .token(BIGGER).add()
                .get();
        var anyTag = ChainGrammar.build("tag")
                .grammars(selfClosingTag.getName(), tagWithChildren.getName()).add()
                .get();
        return parser = new DefaultParser(
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
}
