/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2019 (C) tesshu.com
 */
package com.tesshu.jpsonic.service.search;

import com.tesshu.jpsonic.service.search.IndexType.FieldNames;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKWidthFilterFactory;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.ja.JapanesePartOfSpeechStopFilterFactory;
import org.apache.lucene.analysis.ja.JapaneseTokenizerFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Analyzer provider. This class is a division of what was once part of
 * SearchService and added functionality.
 * 
 * Analyzer can be closed but is a reuse premise. It is held in this class.
 * 
 * Some of the query parses performed in legacy services are defined in
 * QueryAnalyzer. Most of the actual query parsing is done with QueryFactory.
 */
public final class AnalyzerFactory {

  private static AnalyzerFactory instance;

  private static final Logger LOG = LoggerFactory.getLogger(AnalyzerFactory.class);

  /**
   * Returns an instance of AnalyzerFactory.
   * 
   * @return AnalyzerFactory instance
   */
  public static AnalyzerFactory getInstance() {
    if (null == instance) {
      synchronized (AnalyzerFactory.class) {
        if (instance == null) {
          instance = new AnalyzerFactory();
        }
      }
    }
    return instance;
  }

  private Analyzer analyzer;

  private final String stopTags = "org/apache/lucene/analysis/ja/stoptags.txt";

  private final String stopWords = "com/tesshu/jpsonic/service/stopwords.txt";

  private AnalyzerFactory() {
  }

  private CustomAnalyzer.Builder filters(CustomAnalyzer.Builder builder, boolean isIgnoreCase) {
    try {
      builder = builder
        .addTokenFilter(JapanesePartOfSpeechStopFilterFactory.class, "tags", stopTags);
      if(isIgnoreCase) {
        builder.addTokenFilter(LowerCaseFilterFactory.class);
      }
      builder
        .addTokenFilter(CJKWidthFilterFactory.class) // before // StopFilter
        .addTokenFilter(StopFilterFactory.class, "words", stopWords, "ignoreCase", "true")
        .addTokenFilter(ASCIIFoldingFilterFactory.class, "preserveOriginal", "false");
    } catch (IOException e) {
      LOG.error("Error when initializing analyzer", e);
    }
    return builder;
  }

  /**
   * Return analyzer.
   * 
   * @return analyzer for index
   */
  public Analyzer getAnalyzer() {
    if (null == this.analyzer) {
      try {
        
        CustomAnalyzer.Builder builder
        = CustomAnalyzer.builder().withTokenizer(JapaneseTokenizerFactory.class);
        Analyzer analyzer = filters(builder, true).build();

        Analyzer keywordAnalyzer =
            filters(CustomAnalyzer.builder()
            .withTokenizer(KeywordTokenizerFactory.class), true)
            .build();
        Analyzer folderAnalyzer =
            filters(CustomAnalyzer.builder().withTokenizer(KeywordTokenizerFactory.class), false)
            .build();

        Map<String, Analyzer> analyzerMap = new HashMap<String, Analyzer>();
        analyzerMap.put(FieldNames.GENRE, keywordAnalyzer);
        analyzerMap.put(FieldNames.FOLDER, folderAnalyzer);
        analyzerMap.put(FieldNames.MEDIA_TYPE, keywordAnalyzer);
        analyzerMap.put(FieldNames.ARTIST_FULL, keywordAnalyzer);
        analyzerMap.put(FieldNames.ARTIST_READING_HIRAGANA, keywordAnalyzer);
        analyzerMap.put(FieldNames.ALBUM_FULL, keywordAnalyzer);
   
        this.analyzer = new PerFieldAnalyzerWrapper(analyzer, analyzerMap);

      } catch (IOException e) {
        LOG.error("Error when initializing Analyzer", e);
      }
    }
    return this.analyzer;
  }

}
