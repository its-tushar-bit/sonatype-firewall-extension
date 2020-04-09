/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.ClassicAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.Directory;

@Named
@Singleton
public class LuceneComponents
{
  private final NumberFormat numberFormat;

  @Inject
  public LuceneComponents() {
    numberFormat = NumberFormat.getNumberInstance(Locale.ROOT);
  }

  public Analyzer newAnalyzerForSearch() {
    Analyzer standardAnalyzer = new StandardAnalyzer();
    Map<String, Analyzer> fieldAnalyzers = new HashMap<>();
    fieldAnalyzers.put(FieldIdentifier.VULNERABILITY_DESCRIPTION.label, standardAnalyzer);
    fieldAnalyzers.put(FieldIdentifier.APPLICATION_CATEGORY_DESCRIPTION.label, standardAnalyzer);
    fieldAnalyzers.put(FieldIdentifier.COMPONENT_LABEL_DESCRIPTION.label, standardAnalyzer);
    return new PerFieldAnalyzerWrapper(new LowerCaseKeywordAnalyzer(), fieldAnalyzers);
  }

  Analyzer newAnalyzerForAutoCompletion() {
    return new ClassicAnalyzer(CharArraySet.EMPTY_SET);
  }

  public Function<String, Query> newQueryParser() {
    Map<String, PointsConfig> pointsConfigsByFieldName = new HashMap<>();
    pointsConfigsByFieldName.put(FieldIdentifier.POLICY_THREAT_LEVEL.label,
        new PointsConfig(numberFormat, Integer.class));
    pointsConfigsByFieldName.put(FieldIdentifier.VULNERABILITY_SEVERITY.label,
        new PointsConfig(numberFormat, Float.class));
    StandardQueryParser queryParser = new StandardQueryParser(newAnalyzerForSearch());
    queryParser.setPointsConfigMap(pointsConfigsByFieldName);
    queryParser.setAllowLeadingWildcard(true);
    return searchString -> {
      try {
        return queryParser.parse(searchString, FieldIdentifier.VULNERABILITY_ID.label);
      }
      catch (Exception e) {
        throw new BadRequestException("The search query is invalid: " + e.getMessage(), e);
      }
    };
  }

  public AnalyzingInfixSuggester newSuggester(Directory suggesterDirectory) throws IOException {
    return new AnalyzingInfixSuggester(suggesterDirectory, newAnalyzerForAutoCompletion());
  }
}
