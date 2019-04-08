package com.tesshu.jpsonic.service.search.analysis;

import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.util.AttributeFactory;

/**
 * A tokenizer that divides artist text at devide characters defined by id3, or
 * whitespace and comma.
 */
public class Id3ArtistTokenizer extends CharTokenizer {

    /**
     * Construct a new Id3ArtistTokenizer.
     */
    public Id3ArtistTokenizer() {
    }

    /**
     * Construct a new Id3ArtistTokenizer using a given
     * {@link org.apache.lucene.util.AttributeFactory}.
     *
     * @param factory the attribute factory to use for this {@link Tokenizer}
     */
    public Id3ArtistTokenizer(AttributeFactory factory) {
        super(factory);
    }

    /**
     * Construct a new Id3ArtistTokenizer using a given
     * {@link org.apache.lucene.util.AttributeFactory}.
     *
     * @param factory     the attribute factory to use for this {@link Tokenizer}
     * @param maxTokenLen maximum token length the tokenizer will emit. Must be
     *                    greater than 0 and less than MAX_TOKEN_LENGTH_LIMIT
     *                    (1024*1024)
     * @throws IllegalArgumentException if maxTokenLen is invalid.
     */
    public Id3ArtistTokenizer(AttributeFactory factory, int maxTokenLen) {
        super(factory, maxTokenLen);
    }

    /**
     * Collects only characters which do not satisfy
     */
    @Override
    protected boolean isTokenChar(int c) {

        /*
         * see http://id3.org/
         * ; v2.2 
         * / v2.3
         * \0 v2.4 (Unnecessary)
         */
        int id3delim = ';' | '/';

        if (id3delim == c) {
            return false;
        }

        if (Character.SPACE_SEPARATOR == Character.getType(c)) {
            return false;
        }

        if (',' == c) {
            return false;
        }

        return true;
    }

}
