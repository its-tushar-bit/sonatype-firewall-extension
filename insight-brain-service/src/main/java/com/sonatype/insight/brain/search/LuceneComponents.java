/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

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

@Named
@Singleton
public class LuceneComponents
{
  private final Map<String, PointsConfig> pointsConfigsByFieldName;

  @Inject
  public LuceneComponents() {
    pointsConfigsByFieldName = new HashMap<>();
    pointsConfigsByFieldName.put(FieldIdentifier.POLICY_THREAT_LEVEL.label,
        new PointsConfig(NumberFormat.getIntegerInstance(Locale.ROOT), Integer.class));
  }

  public Analyzer newAnalyzerForSearch() {
    Analyzer standardAnalyzer = new StandardAnalyzer();
    Map<String, Analyzer> fieldAnalyzers = new HashMap<>();
    fieldAnalyzers.put(FieldIdentifier.VULNERABILITY_DESCRIPTION.label, standardAnalyzer);
    fieldAnalyzers.put(FieldIdentifier.APPLICATION_CATEGORY_DESCRIPTION.label, standardAnalyzer);
    fieldAnalyzers.put(FieldIdentifier.COMPONENT_LABEL_DESCRIPTION.label, standardAnalyzer);
    return new PerFieldAnalyzerWrapper(new LowerCaseKeywordAnalyzer(), fieldAnalyzers);
  }

  public Analyzer newAnalyzerForAutoCompletion() {
    return new ClassicAnalyzer(CharArraySet.EMPTY_SET);
  }

  public Function<String, Query> newQueryParser() {
    StandardQueryParser queryParser = new StandardQueryParser(newAnalyzerForSearch());
    queryParser.setPointsConfigMap(pointsConfigsByFieldName);
    return searchString -> {
      try {
        return queryParser.parse(searchString, FieldIdentifier.VULNERABILITY_ID.label);
      }
      catch (Exception e) {
        throw new BadRequestException("The search query is invalid: " + e.getMessage(), e);
      }
    };
  }
}
