package com.tesshu.jpsonic.service.search.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.util.Map;

public class PunctuationStemFilterFactory extends TokenFilterFactory {

    public PunctuationStemFilterFactory(Map<String, String> args) {
        super(args);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public PunctuationStemFilter create(TokenStream input) {
        return new PunctuationStemFilter(input);
    }

}
