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
import java.time.temporal.ChronoUnit;
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

  @Test
  public void includeAutoWaiversFalse_compilesManualOnlyChip() {
    CompiledQuery compiled = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.WAIVER, Map.of("includeAutoWaivers", false));
    assertThat(compiled.q()).contains("policyWaiverAuto:\"false\"");
  }

  @Test
  public void includeAutoWaiversTrue_isNoOpIncludeBoth() {
    CompiledQuery compiled = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.WAIVER, Map.of("includeAutoWaivers", true));
    assertThat(compiled.q()).doesNotContain("policyWaiverAuto");
    assertThat(compiled.fieldClauses()).isEmpty();
  }

  @Test
  public void includeAutoWaiversAbsent_includesBothKinds() {
    CompiledQuery compiled = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.WAIVER, Map.of());
    assertThat(compiled.q()).doesNotContain("policyWaiverAuto");
  }

  @Test
  public void isAutoTrue_compilesAutoOnlyChip() {
    CompiledQuery compiled = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.WAIVER, Map.of("isAuto", List.of("true")));
    assertThat(compiled.q()).isEqualTo("policyWaiverIsAuto:\"true\"");
  }

  @Test
  public void isAutoInvalidValue_rejectedWith400() {
    assertThatExceptionOfType(FilterValidationException.class)
        .isThrownBy(() -> IndexQueryFilterCompiler.compileWithClauses(
            IndexQueryType.WAIVER, Map.of("isAuto", List.of("banana"))))
        .satisfies(e -> assertThat(e.getCode()).isEqualTo(FilterValidationException.Code.INVALID_FILTER));
  }

  @Test
  public void expiryStatus_canonicalizesCaseInsensitiveValues() {
    CompiledQuery compiled = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.WAIVER, Map.of("expiryStatus", List.of("Active", "NEVER")));
    // active expands to (active OR never); Never remains an exact never clause.
    assertThat(compiled.q()).contains("policyWaiverExpiryStatus:\"active\"");
    assertThat(compiled.q()).contains("policyWaiverExpiryStatus:\"never\"");
  }

  @Test
  public void expiryStatusActive_includesNeverExpiring() {
    CompiledQuery compiled = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.WAIVER, Map.of("expiryStatus", List.of("active")));
    assertThat(compiled.q()).isEqualTo(
        "(policyWaiverExpiryStatus:\"active\" OR policyWaiverExpiryStatus:\"never\")");
  }

  @Test
  public void expiryStatus_invalidValue_rejectedWith400() {
    assertThatExceptionOfType(FilterValidationException.class)
        .isThrownBy(() -> IndexQueryFilterCompiler.compileWithClauses(
            IndexQueryType.WAIVER, Map.of("expiryStatus", List.of("InvalidStatus"))))
        .satisfies(e -> assertThat(e.getCode()).isEqualTo(FilterValidationException.Code.INVALID_FILTER));
  }

  @Test
  public void lifecycleStatus_reusesWaiverStatusClauses() {
    assertThat(IndexQueryService.STATUS_EXPIRING_WINDOW_DAYS).isEqualTo(7);
    long windowEnd = classicExpiringWindowEnd(FIXED_CLOCK);

    CompiledQuery compiled = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.WAIVER, Map.of("lifecycleStatus", List.of("expiring", "auto-waived")), FIXED_CLOCK);

    assertThat(compiled.q()).isEqualTo(
        "((itemType:policy_waiver AND policyWaiverExpiresAtEpochMs:{" + FIXED_NOW_MS + " TO " + windowEnd + "])"
            + " OR policyWaiverAuto:\"true\")");
  }

  @Test
  public void lifecycleStatus_invalidValue_rejectedWith400() {
    assertThatExceptionOfType(FilterValidationException.class)
        .isThrownBy(() -> IndexQueryFilterCompiler.compileWithClauses(
            IndexQueryType.WAIVER, Map.of("lifecycleStatus", List.of("future-ish")), FIXED_CLOCK))
        .satisfies(e -> assertThat(e.getCode()).isEqualTo(FilterValidationException.Code.INVALID_FILTER));
  }

  private static long classicExpiringWindowEnd(final Clock clock) {
    return clock.instant()
        .truncatedTo(ChronoUnit.DAYS)
        .plus(IndexQueryService.STATUS_EXPIRING_WINDOW_DAYS, ChronoUnit.DAYS)
        .plus(1, ChronoUnit.DAYS)
        .toEpochMilli();
  }

  @Test
  public void policyIds_andPoliciesAlias_compileToSameField() {
    CompiledQuery byIds = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.WAIVER, Map.of("policyIds", List.of("pol-1")));
    CompiledQuery byAlias = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.WAIVER, Map.of("policies", List.of("pol-1")));
    assertThat(byIds.q()).isEqualTo("policyWaiverPolicyId:\"pol-1\"");
    assertThat(byAlias.q()).isEqualTo("policyWaiverPolicyId:\"pol-1\"");
  }
}
