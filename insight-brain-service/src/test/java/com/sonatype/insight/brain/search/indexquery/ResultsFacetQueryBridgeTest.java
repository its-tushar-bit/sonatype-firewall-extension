/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.sonatype.insight.brain.search.indexquery.IndexQueryFilterCompiler.CompiledQuery;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@code /results} {@code q=} → facet-count {@link CompiledQuery} bridge. The
 * bridge lifts the top-level {@code field:value} chips out of the parsed AST and rebuilds them as the
 * Lucene clause strings the shared facet-count base expects, using the same FieldMap field resolution
 * as the index-query path. Free text is intentionally dropped from the count base.
 */
public class ResultsFacetQueryBridgeTest
{
  @Test
  public void freeTextOnly_yieldsNoStructuredClauses() {
    // The key guard: a q with no field:value chips must still produce a valid (empty-clause) CompiledQuery
    // so the facet counts fall back to the whole-corpus item-type base rather than erroring.
    CompiledQuery compiled = ResultsFacetQueryBridge.compile(IndexQueryType.VIOLATION, "log4j apache");
    assertThat(compiled.fieldClauses()).isEmpty();
    assertThat(compiled.waiverStatusClauses()).isEmpty();
    assertThat(compiled.autoWaiverRestrictionClause()).isNull();
  }

  @Test
  public void blankQuery_yieldsNoStructuredClauses() {
    assertThat(ResultsFacetQueryBridge.compile(IndexQueryType.APPLICATION, "").fieldClauses()).isEmpty();
    assertThat(ResultsFacetQueryBridge.compile(IndexQueryType.APPLICATION, null).fieldClauses()).isEmpty();
    assertThat(ResultsFacetQueryBridge.compile(IndexQueryType.APPLICATION, "   ").fieldClauses()).isEmpty();
  }

  @Test
  public void clausesAreKeyedByIndexField_soAFacetCanSubtractItsOwnDimension() {
    // The facet engine looks a facet's own clauses up by its index field. Two chips on one field must
    // both land under that field, or the facet subtracts only part of its own selection and collapses to
    // the value the user picked last.
    CompiledQuery compiled = ResultsFacetQueryBridge.compile(
        IndexQueryType.VIOLATION,
        "policyViolationThreatCategory:security policyViolationThreatCategory:license policyEvaluationStage:build");
    assertThat(compiled.clausesByField())
        .containsEntry(
            "policyViolationThreatCategory",
            List.of("policyViolationThreatCategory:\"security\"", "policyViolationThreatCategory:\"license\""))
        .containsEntry("policyEvaluationStage", List.of("policyEvaluationStage:\"build\""));
  }

  @Test
  public void keywordChip_resolvesToIndexFieldClause_lowercased() {
    // policyViolationThreatCategory is both the /results query-language field key AND the index field
    // label; the value is lowercased to match the keyword analyzer / OpenSearch lowercase normalizer
    // applied by count(String).
    CompiledQuery compiled =
        ResultsFacetQueryBridge.compile(IndexQueryType.VIOLATION, "policyViolationThreatCategory:SECURITY");
    assertThat(compiled.fieldClauses()).containsExactly("policyViolationThreatCategory:\"security\"");
  }

  @Test
  public void stageChip_resolvesToPolicyEvaluationStage() {
    CompiledQuery compiled =
        ResultsFacetQueryBridge.compile(IndexQueryType.VIOLATION, "policyEvaluationStage:build");
    assertThat(compiled.fieldClauses()).containsExactly("policyEvaluationStage:\"build\"");
  }

  @Test
  public void multipleChips_allLifted_freeTextIgnored() {
    CompiledQuery compiled = ResultsFacetQueryBridge.compile(
        IndexQueryType.VIOLATION, "log4j policyViolationThreatCategory:license policyEvaluationStage:release");
    assertThat(compiled.fieldClauses())
        .containsExactlyInAnyOrder("policyViolationThreatCategory:\"license\"", "policyEvaluationStage:\"release\"");
  }

  @Test
  public void numericRangeChip_resolvesToRangeClause() {
    // policyViolationThreatLevel is a numeric field on VIOLATION docs; a range chip becomes a [lo TO hi].
    CompiledQuery compiled =
        ResultsFacetQueryBridge.compile(IndexQueryType.VIOLATION, "policyViolationThreatLevel:[7 TO 10]");
    assertThat(compiled.fieldClauses()).containsExactly("policyViolationThreatLevel:[7 TO 10]");
  }

  @Test
  public void numericExactChip_resolvesToDegenerateRange() {
    CompiledQuery compiled =
        ResultsFacetQueryBridge.compile(IndexQueryType.VIOLATION, "policyViolationThreatLevel:9");
    assertThat(compiled.fieldClauses()).containsExactly("policyViolationThreatLevel:[9 TO 9]");
  }

  @Test
  public void openEndedRangeChip_usesStarBound() {
    CompiledQuery compiled =
        ResultsFacetQueryBridge.compile(IndexQueryType.VIOLATION, "policyViolationThreatLevel:[7 TO *]");
    assertThat(compiled.fieldClauses()).containsExactly("policyViolationThreatLevel:[7 TO *]");
  }

  @Test
  public void unknownField_dropped() {
    CompiledQuery compiled = ResultsFacetQueryBridge.compile(
        IndexQueryType.VIOLATION, "notARealField:foo policyViolationThreatCategory:security");
    assertThat(compiled.fieldClauses()).containsExactly("policyViolationThreatCategory:\"security\"");
  }

  @Test
  public void fieldNotAllowedOnEntityType_dropped() {
    // vulnerabilitySeverity is a VULNERABILITY field; it is not indexed on APPLICATION docs, so it is
    // not a structured filter for the Applications tab.
    CompiledQuery compiled =
        ResultsFacetQueryBridge.compile(IndexQueryType.APPLICATION, "vulnerabilitySeverity:7.5");
    assertThat(compiled.fieldClauses()).isEmpty();
  }

  @Test
  public void violationWaiverStatusChip_trackedForFixedFacetBase() {
    // The state facet base subtracts the user's own waiver-status clause so the fixed OPEN/WAIVED counts
    // stay whole-corpus. The bridge must track it in waiverStatusClauses.
    CompiledQuery compiled =
        ResultsFacetQueryBridge.compile(IndexQueryType.VIOLATION, "policyViolationWaiverStatus:Waived");
    assertThat(compiled.waiverStatusClauses()).containsExactly("policyViolationWaiverStatus:\"waived\"");
    assertThat(compiled.fieldClauses()).contains("policyViolationWaiverStatus:\"waived\"");
  }

  @Test
  public void manualOnlyAutoRestriction_tracked() {
    // policyWaiverAuto:false narrows to manual waivers; the auto/manual facet base drops it so both
    // true/false buckets count whole-corpus.
    CompiledQuery compiled =
        ResultsFacetQueryBridge.compile(IndexQueryType.WAIVER, "policyWaiverAuto:false");
    assertThat(compiled.autoWaiverRestrictionClause()).isEqualTo("policyWaiverAuto:\"false\"");
    assertThat(compiled.fieldClauses()).contains("policyWaiverAuto:\"false\"");
  }

  @Test
  public void autoTrue_notTrackedAsRestriction() {
    CompiledQuery compiled =
        ResultsFacetQueryBridge.compile(IndexQueryType.WAIVER, "policyWaiverAuto:true");
    assertThat(compiled.autoWaiverRestrictionClause()).isNull();
    assertThat(compiled.fieldClauses()).containsExactly("policyWaiverAuto:\"true\"");
  }

  @Test
  public void nonNumericRangeBounds_areDropped_notRebuiltIntoMalformedRange() {
    // A malformed range must not reach the count query: rebuilding label:[abc TO xyz] makes count() throw,
    // so includeFacets would turn a request that succeeds without it into a 400/500. The page path fails
    // open, so the facet base does too.
    CompiledQuery compiled = ResultsFacetQueryBridge.compile(
        IndexQueryType.VIOLATION, "policyViolationThreatLevel:[abc TO xyz]");

    assertThat(compiled.fieldClauses()).isEmpty();
  }

  @Test
  public void partiallyNonNumericRangeBound_isDropped() {
    CompiledQuery compiled = ResultsFacetQueryBridge.compile(
        IndexQueryType.VIOLATION, "policyViolationThreatLevel:[1 TO xyz]");

    assertThat(compiled.fieldClauses()).isEmpty();
  }

  @Test
  public void numericRangeBounds_areKept_includingOpenBounds() {
    // The guard must not reject legitimate ranges, including the open '*' bound.
    assertThat(ResultsFacetQueryBridge.compile(
        IndexQueryType.VIOLATION, "policyViolationThreatLevel:[1 TO 5]").fieldClauses())
            .containsExactly("policyViolationThreatLevel:[1 TO 5]");
    assertThat(ResultsFacetQueryBridge.compile(
        IndexQueryType.VIOLATION, "policyViolationThreatLevel:[* TO 5]").fieldClauses())
            .containsExactly("policyViolationThreatLevel:[* TO 5]");
  }

  @Test
  public void nonFiniteNumericValues_areDropped() {
    // Double.parseDouble accepts NaN/Infinity without throwing, and either builds a range the numeric
    // point field cannot parse -- the same 400/500-under-includeFacets failure a non-numeric bound caused.
    for (String value : new String[]{"NaN", "Infinity", "-Infinity"}) {
      assertThat(ResultsFacetQueryBridge.compile(
          IndexQueryType.VIOLATION, "policyViolationThreatLevel:" + value).fieldClauses())
              .as("exact %s", value)
              .isEmpty();
      assertThat(ResultsFacetQueryBridge.compile(
          IndexQueryType.VIOLATION, "policyViolationThreatLevel:[" + value + " TO 5]").fieldClauses())
              .as("range low %s", value)
              .isEmpty();
    }
  }

  @Test
  public void prefixChip_isDropped_ratherThanCountedAsExactMatch() {
    // The page query compiles a prefix chip to a real PrefixQuery (QueryCompiler#boundedPrefixQuery), so
    // counting it as an exact phrase would undercount every bucket. Dropping it keeps the facet base a
    // superset of the page filter.
    CompiledQuery compiled =
        ResultsFacetQueryBridge.compile(IndexQueryType.VIOLATION, "policyViolationThreatCategory:secur*");

    assertThat(compiled.fieldClauses()).isEmpty();
  }

  @Test
  public void exactChipAlongsidePrefixChip_keepsOnlyTheExactClause() {
    CompiledQuery compiled = ResultsFacetQueryBridge.compile(
        IndexQueryType.VIOLATION,
        "policyViolationThreatCategory:security policyViolationWaiverStatus:Waive*");

    assertThat(compiled.fieldClauses()).containsExactly("policyViolationThreatCategory:\"security\"");
  }
}
