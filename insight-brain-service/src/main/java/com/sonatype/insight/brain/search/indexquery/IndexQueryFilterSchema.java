/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import java.util.Map;

public final class IndexQueryFilterSchema
{
  public enum Kind
  {
    TEXT,
    TERMS,
    RANGE
  }

  public record FilterDef(String field, Kind kind)
  {
  }

  /** The {@code query} filter has no index field: it becomes the bare-token query, not a field chip. */
  public static final FilterDef FREE_TEXT_QUERY = new FilterDef(null, Kind.TEXT);

  private static final Map<IndexQueryType, Map<String, FilterDef>> SCHEMA = buildSchema();

  private IndexQueryFilterSchema() {
  }

  public static Map<String, FilterDef> forQueryType(final IndexQueryType queryType) {
    return SCHEMA.getOrDefault(queryType, Map.of());
  }

  private static Map<IndexQueryType, Map<String, FilterDef>> buildSchema() {
    return Map.of(
        // No policyThreatLevel/violationStates on APPLICATION: they are aggregations over the app's
        // violations, not indexed application attributes, so they cannot be honoured here.
        // No categories filter: applicationCategoryName is indexed only on APPLICATION_CATEGORY docs,
        // never on APPLICATION docs, so it would silently match nothing (same reason it's absent from
        // VIOLATION). policyEvaluationStage IS indexed on APPLICATION docs, so the stages filter works.
        IndexQueryType.APPLICATION, Map.of(
            "query", FREE_TEXT_QUERY,
            "organizations", new FilterDef("organizationName", Kind.TERMS),
            "applications", new FilterDef("applicationName", Kind.TERMS),
            "stages", new FilterDef("policyEvaluationStage", Kind.TERMS)),
        // No categories filter: applicationCategoryName is indexed only on
        // APPLICATION_CATEGORY docs, never on violation docs, so it would silently match nothing.
        IndexQueryType.VIOLATION, Map.of(
            "query", FREE_TEXT_QUERY,
            "organizations", new FilterDef("organizationName", Kind.TERMS),
            "applications", new FilterDef("applicationName", Kind.TERMS),
            "policyTypes", new FilterDef("policyViolationThreatCategory", Kind.TERMS),
            // policyViolationThreatLevel is set only on POLICY_VIOLATION docs; LEGAL_VIOLATION docs
            // carry no queryable threat-level field, so this range narrows policy violations only.
            "policyThreatLevel", new FilterDef("policyViolationThreatLevel", Kind.RANGE)),
        IndexQueryType.POLICY, Map.of(
            "query", FREE_TEXT_QUERY,
            "policyTypes", new FilterDef("policyThreatCategory", Kind.TERMS),
            "organizations", new FilterDef("organizationName", Kind.TERMS),
            "policyThreatLevel", new FilterDef("policyThreatLevel", Kind.RANGE)));
  }
}
