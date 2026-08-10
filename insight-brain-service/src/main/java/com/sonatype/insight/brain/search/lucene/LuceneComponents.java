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
import org.apache.lucene.analysis.core.KeywordAnalyzer;
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

  private final LuceneIndexGenerations indexGenerations;

  @Inject
  public LuceneComponents(InsightWork insightWork) {
    this.insightWork = insightWork;
    this.indexGenerations = new LuceneIndexGenerations(insightWork);
    numberFormat = NumberFormat.getNumberInstance(Locale.ROOT);
  }

  /**
   * Opens the serving (blue) Lucene index directory under {@code search/index}.
   */
  public Directory openSearchIndex(boolean readOnly) throws IOException {
    return openSearchIndexAt(insightWork.getSearchIndexDir().toPath(), readOnly);
  }

  public Directory openSearchIndexAt(final Path searchIndexDirectory, final boolean readOnly) throws IOException {
    if (!Files.exists(searchIndexDirectory)) {
      if (readOnly) {
        return null;
      }
      Files.createDirectories(searchIndexDirectory);
    }
    return FSDirectory.open(searchIndexDirectory);
  }

  public Path createBuildingGenerationDirectory() throws IOException {
    return indexGenerations.createBuildingGenerationDirectory();
  }

  public Path cutoverBuildingGeneration(final Path greenPath) throws IOException {
    return indexGenerations.cutover(greenPath);
  }

  public void deleteIndexGeneration(final Path generationPath) throws IOException {
    LuceneIndexGenerations.deleteRecursively(generationPath);
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
    // Waiver-request free-text fields tokenize the same way so fielded word-level search matches.
    fieldAnalyzers.put(FieldIdentifier.REJECTION_REASON.label, standardAnalyzer);
    fieldAnalyzers.put(FieldIdentifier.NOTE_TO_REVIEWER.label, standardAnalyzer);
    // Keyword (no lowercasing) for the Ana expiryStatus field. Indexed values are already lowercase
    // ({@code active}/{@code expired}/{@code never}); keeping KeywordAnalyzer matches the OpenSearch
    // keyword mapping and avoids accidental analyzer drift if vocabulary casing ever changes.
    fieldAnalyzers.put(FieldIdentifier.POLICY_WAIVER_EXPIRY_STATUS.label, new KeywordAnalyzer());
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
    // Int twin backing the Components tab policyThreatLevel range filter; without a PointsConfig the
    // range fragment fails to parse as an IntPoint range and the Lucene backend returns zero results.
    pointsConfigsByFieldName.put(FieldIdentifier.COMPONENT_MAX_POLICY_THREAT_LEVEL.label,
        new PointsConfig(numberFormat, Integer.class));
    pointsConfigsByFieldName.put(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label,
        new PointsConfig(numberFormat, Integer.class));
    pointsConfigsByFieldName.put(FieldIdentifier.POLICY_WAIVER_EXPIRES_AT_EPOCH_MS.label,
        new PointsConfig(numberFormat, Long.class));
    // Epoch-millis long backing the applications "latest evaluation" range filter/sort.
    pointsConfigsByFieldName.put(FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label,
        new PointsConfig(numberFormat, Long.class));
    // Epoch-millis long backing the local vulnerabilities "first seen (within ...)" window range filter.
    pointsConfigsByFieldName.put(FieldIdentifier.VULNERABILITY_FIRST_SEEN_EPOCH_MS.label,
        new PointsConfig(numberFormat, Long.class));
    // IntPoint backing the applications policyThreatLevel range filter/facet (max threat per app).
    pointsConfigsByFieldName.put(FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL.label,
        new PointsConfig(numberFormat, Integer.class));
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
