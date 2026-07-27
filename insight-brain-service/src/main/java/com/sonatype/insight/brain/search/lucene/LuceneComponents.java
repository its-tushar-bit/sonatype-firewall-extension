/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

@Named
@Singleton
public class LuceneComponents
{
  private final NumberFormat numberFormat;

  private final InsightWork insightWork;

  @Inject
  public LuceneComponents(InsightWork insightWork) {
    this.insightWork = insightWork;
    numberFormat = NumberFormat.getNumberInstance(Locale.ROOT);
  }

  public Directory openSearchIndex(boolean readOnly) throws IOException {
    Path searchIndexDirectory = insightWork.getSearchIndexDir().toPath();
    if (readOnly && !Files.exists(searchIndexDirectory)) {
      return null;
    }
    return FSDirectory.open(searchIndexDirectory);
  }

  public Analyzer newAnalyzerForSearch() {
    Analyzer standardAnalyzer = new StandardAnalyzer();
    Map<String, Analyzer> fieldAnalyzers = new HashMap<>();
    fieldAnalyzers.put(FieldIdentifier.VULNERABILITY_DESCRIPTION.label, standardAnalyzer);
    fieldAnalyzers.put(FieldIdentifier.APPLICATION_CATEGORY_DESCRIPTION.label, standardAnalyzer);
    fieldAnalyzers.put(FieldIdentifier.COMPONENT_LABEL_DESCRIPTION.label, standardAnalyzer);
    // Match the OpenSearch `text` mapping for waiver reason/comment so both backends tokenize alike.
    fieldAnalyzers.put(FieldIdentifier.POLICY_WAIVER_REASON.label, standardAnalyzer);
    fieldAnalyzers.put(FieldIdentifier.POLICY_WAIVER_COMMENT.label, standardAnalyzer);
    return new PerFieldAnalyzerWrapper(new LowerCaseKeywordAnalyzer(), fieldAnalyzers);
  }

  public Function<String, Query> newQueryParser() {
    Map<String, PointsConfig> pointsConfigsByFieldName = new HashMap<>();
    pointsConfigsByFieldName.put(FieldIdentifier.POLICY_THREAT_LEVEL.label,
        new PointsConfig(numberFormat, Integer.class));
    pointsConfigsByFieldName.put(FieldIdentifier.VULNERABILITY_SEVERITY.label,
        new PointsConfig(numberFormat, Float.class));
    pointsConfigsByFieldName.put(FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label,
        new PointsConfig(numberFormat, Integer.class));
    pointsConfigsByFieldName.put(FieldIdentifier.COMPONENT_LICENSE_THREAT_LEVEL.label,
        new PointsConfig(numberFormat, Integer.class));
    pointsConfigsByFieldName.put(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label,
        new PointsConfig(numberFormat, Integer.class));
    pointsConfigsByFieldName.put(FieldIdentifier.POLICY_WAIVER_EXPIRES_AT_EPOCH_MS.label,
        new PointsConfig(numberFormat, Long.class));
    // Epoch-millis long backing the applications "latest evaluation" range filter/sort.
    pointsConfigsByFieldName.put(FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label,
        new PointsConfig(numberFormat, Long.class));
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
}
