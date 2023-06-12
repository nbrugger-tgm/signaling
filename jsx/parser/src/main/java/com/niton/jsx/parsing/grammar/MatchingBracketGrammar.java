package com.niton.jsx.parsing.grammar;

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

public class MatchingBracketGrammar<T extends AstNode> extends Grammar<T> {
    private final Grammar<?> openingGrammar;
    private final Grammar<?> closingGrammar;
    private final Grammar<T> inner;

    public MatchingBracketGrammar(String openingGrammar, String closingGrammar, Grammar<T> inner) {
        this.openingGrammar = Grammar.tokenReference(openingGrammar);
        this.closingGrammar = Grammar.tokenReference(closingGrammar);
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
                throw new ParsingException(getOriginGrammarName(), String.format("Unexpected end of file. Expected '%s' with character '%s'", getName(), openingGrammar), tokens);
            }
            var start = openingGrammar.parse(tokens, reference);
            var startIndex = tokens.index();//start is inclusive
            var counter = 1;
            ParsingException lastClosingException = null;
            while (tokens.hasNext()) {
                var next = tokens.next();
                try{
                    openingGrammar.parse(tokens, reference);
                    counter ++;
                }catch (ParsingException e){
                    try {
                        closingGrammar.parse(tokens, reference);
                        counter --;
                    } catch (ParsingException e1) {
                        lastClosingException = e1;
                    }
                }
                if (counter == 0) {
                    var endIndex = tokens.index() - 1;//end is exclusive
                    var substream = tokens.subStream(startIndex, endIndex);
                    var innerNode = inner.parse(substream, reference);
                    verifyNoMoreTokens(substream);
                    return innerNode;
                }
            }
            throw new ParsingException(getOriginGrammarName(), String.format("Unexpected end of file. Block '%s' (at %s) was opened with '%s' but never closed (with '%s')", getName(),start.getLocation().toString(), closingGrammar, closingGrammar), lastClosingException);
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
