/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.search.global.FilterValidationException;
import com.sonatype.insight.brain.search.indexquery.IndexQueryFilterCompiler.CompiledQuery;

import org.junit.Test;

public class IndexQueryFilterCompilerTest
{
  private static final long FIXED_NOW_MS = 1_700_000_000_000L;

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.ofEpochMilli(FIXED_NOW_MS), ZoneOffset.UTC);

  @Test
  public void expiryExpired_compilesToExpiredRangeAtFixedNow() {
    CompiledQuery compiled = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.WAIVER, Map.of("expiry", "expired", "includeAutoWaivers", true), FIXED_CLOCK);

    assertThat(compiled.fieldClauses())
        .contains("policyWaiverExpiresAtEpochMs:[* TO " + FIXED_NOW_MS + "]");
  }

  @Test
  public void expiryActive_compilesToNegatedExpiredRangeAtFixedNow() {
    CompiledQuery compiled = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.WAIVER, Map.of("expiry", "active", "includeAutoWaivers", true), FIXED_CLOCK);

    assertThat(compiled.fieldClauses())
        .contains("NOT policyWaiverExpiresAtEpochMs:[* TO " + FIXED_NOW_MS + "]");
  }

  // -- Applications aggregate filters (A1/A2/A3/A4 + age) ------------------------------------

  @Test
  public void applicationStages_singleTerm_compilesToKeywordChip() {
    CompiledQuery compiled = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.APPLICATION, Map.of("stages", List.of("build")));
    assertThat(compiled.fieldClauses()).contains("applicationViolationStage:\"build\"");
  }

  @Test
  public void applicationStages_multiTerm_orsWithinFilter() {
    // OR-within a single TERMS filter: any of the stages matches.
    CompiledQuery compiled = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.APPLICATION, Map.of("stages", List.of("build", "release")));
    assertThat(compiled.fieldClauses())
        .contains("(applicationViolationStage:\"build\" OR applicationViolationStage:\"release\")");
  }

  @Test
  public void applicationPolicyTypesAndStates_distinctFiltersAndNarrow() {
    // AND-across distinct filters: policyTypes AND violationStates both appear as separate chips.
    CompiledQuery compiled = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.APPLICATION,
        Map.of("policyTypes", List.of("security"), "violationStates", List.of("waived")));
    assertThat(compiled.fieldClauses())
        .contains("applicationViolationPolicyType:\"security\"", "applicationViolationState:\"waived\"");
  }

  @Test
  public void applicationPolicyThreatLevel_compilesToRangeOnMaxThreat() {
    CompiledQuery compiled = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.APPLICATION, Map.of("policyThreatLevel", List.of(7, 10)));
    assertThat(compiled.fieldClauses()).contains("applicationMaxPolicyThreatLevel:[7 TO 10]");
  }

  @Test
  public void applicationAge_compilesToRangeOnLastEvaluationEpochMs() {
    // age is a plain RANGE over the existing epoch field; the caller resolves the window to bounds.
    long from = FIXED_NOW_MS - 7L * 24 * 60 * 60 * 1000;
    CompiledQuery compiled = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.APPLICATION, Map.of("age", List.of(from, FIXED_NOW_MS)));
    assertThat(compiled.fieldClauses())
        .contains("applicationLastEvaluationTimeEpochMs:[" + from + " TO " + FIXED_NOW_MS + "]");
  }

  @Test
  public void applicationUnknownFilterKey_rejected() {
    assertThatExceptionOfType(FilterValidationException.class)
        .isThrownBy(() -> IndexQueryFilterCompiler.compileWithClauses(
            IndexQueryType.APPLICATION, Map.of("notARealFilter", List.of("x"))));
  }
}
