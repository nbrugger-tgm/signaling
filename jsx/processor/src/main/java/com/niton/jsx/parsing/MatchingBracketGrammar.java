package com.niton.jsx.parsing;

import com.niton.parser.ast.AstNode;
import com.niton.parser.ast.TokenNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.token.TokenStream;
import com.niton.parser.token.Tokenable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class MatchingBracketGrammar<T extends AstNode> extends Grammar<GrammarMatcher<T>, T> {
    private final String openingToken;
    private final String closingToken;
    private final Grammar<?, T> inner;

    public MatchingBracketGrammar(String openingToken, String closingToken, Grammar<?, T> inner) {
        this.openingToken = openingToken;
        this.closingToken = closingToken;
        this.inner = inner;
    }

    public static MatchingBracketGrammar<TokenNode> from(String openingToken, String closingToken) {
        return new MatchingBracketGrammar<>(openingToken, closingToken, new AnyTokenGrammar());
    }

    public static MatchingBracketGrammar<TokenNode> from(Tokenable defaultToken, Tokenable defaultToken1) {
        return from(defaultToken.name(), defaultToken1.name());
    }

    @Override
    protected GrammarMatcher<T> createExecutor() {
        return new Matcher();
    }

    private class Matcher extends GrammarMatcher<T> {

        private Matcher() {
        }

        @Override
        @NotNull
        protected T process(@NotNull TokenStream tokens, @NotNull GrammarReference reference) throws ParsingException {
            if (!tokens.hasNext()) {
                throw new ParsingException(getOriginGrammarName(), String.format("Unexpected end of file. Expected '%s' with character '%s'", getName(), openingToken), tokens);
            }
            var token = tokens.next();
            if (!token.getName().equals(openingToken)) {
                throw new ParsingException(getOriginGrammarName(), String.format("Unexpected token '%s' with character '%s'. Expected '%s'", token.getName(), token.getValue(), openingToken), tokens);
            }
            var startIndex = tokens.index();//start is inclusive
            var counter = 1;
            while (tokens.hasNext()) {
                var next = tokens.next();
                if (next.getName().equals(openingToken)) {
                    counter++;
                } else if (next.getName().equals(closingToken)) {
                    counter--;
                }
                if (counter == 0) {
                    var endIndex = tokens.index() - 1;//end is exclusive
                    var substream = tokens.subStream(startIndex, endIndex);
                    var innerNode = inner.parse(substream, reference);
                    verifyNoMoreTokens(substream);
                    return innerNode;
                }
            }
            throw new ParsingException(getOriginGrammarName(), String.format("Unexpected end of file. Block '%s' was opened with '%s' but never closed (with '%s')", getName(), closingToken, closingToken), tokens);
        }

        private void verifyNoMoreTokens(TokenStream substream) throws ParsingException {
            if (substream.hasNext()) {
                var remaining = new ArrayList<>();
                while (substream.hasNext()) {
                    remaining.add(substream.next());
                }
                throw new ParsingException(getOriginGrammarName(), String.format("Content in the block '%s' was not fully parsed! This is a bug in the grammar! Unparsed content: %s", getName(), remaining), substream);
            }
        }
    }
}
