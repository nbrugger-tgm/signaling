package com.niton.jsx.parsing;

import com.niton.parser.ast.TokenNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.token.TokenStream;
import com.niton.parser.token.Tokenizer;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

public class AnyTokenGrammar extends Grammar<GrammarMatcher<TokenNode>, TokenNode> {
    @Override
    protected GrammarMatcher<TokenNode> createExecutor() {
        return new GrammarMatcher<>() {
            @Override
            protected @NotNull TokenNode process(@NotNull TokenStream tokens, @NotNull GrammarReference reference) throws ParsingException {
                var remainingTokens = new LinkedList<Tokenizer.AssignedToken>();
                while (tokens.hasNext()) {
                    remainingTokens.add(tokens.next());
                }
                return new TokenNode(remainingTokens);
            }
        };
    }
}
