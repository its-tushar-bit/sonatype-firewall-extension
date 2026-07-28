/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;

import com.sonatype.insight.brain.search.catalog.CatalogLocalRequestBuilder.LocalQuery;

import org.junit.Test;

public class CatalogLocalRequestBuilderTest
{
  @Test
  public void firstSeenWindow_30d_emitsLowerBoundedEpochRangeClause() {
    long before = Instant.now().minus(Duration.ofDays(30)).toEpochMilli();
    LocalQuery result = CatalogLocalRequestBuilder.build(
        CatalogEntityType.VULNERABILITY, Map.of("firstSeenWindow", "30d"));
    long after = Instant.now().minus(Duration.ofDays(30)).toEpochMilli();

    assertThat(result.fieldClauses()).hasSize(1);
    String clause = result.fieldClauses().get(0);
    assertThat(clause).startsWith("vulnerabilityFirstSeenEpochMs:[");
    assertThat(clause).endsWith(" TO *]");
    // The lower bound is now-30d, resolved at build time; bracket it between two clock reads.
    long start = Long.parseLong(
        clause.substring(clause.indexOf('[') + 1, clause.indexOf(" TO ")));
    assertThat(start).isBetween(before, after);
  }

  @Test
  public void firstSeenWindow_windows_mapToExpectedDayOffsets() {
    assertLowerBoundDays("90d", 90);
    assertLowerBoundDays("1y", 365);
    assertLowerBoundDays("2y", 730);
  }

  @Test
  public void firstSeenWindow_all_any_blank_emitNoClause() {
    for (String token : new String[]{"all", "any", "", "ALL"}) {
      LocalQuery result = CatalogLocalRequestBuilder.build(
          CatalogEntityType.VULNERABILITY, Map.of("firstSeenWindow", token));
      assertThat(result.fieldClauses())
          .as("token '%s' must not emit a first-seen clause", token)
          .noneMatch(c -> c.contains("vulnerabilityFirstSeenEpochMs"));
    }
  }

  @Test
  public void firstSeenWindow_unknownToken_is400() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> CatalogLocalRequestBuilder.build(CatalogEntityType.VULNERABILITY, Map.of("firstSeenWindow", "5w")));
  }

  @Test
  public void firstSeenWindow_isLowerBoundedSoRowsWithNoFirstSeenAreExcluded() {
    // A [start TO *] range on a LongPoint matches only docs carrying the field, so a non-violating
    // vuln (no vulnerabilityFirstSeenEpochMs) is excluded by any window. Assert the clause is lower-
    // bounded (not open-lower "* TO *"), which is what produces that exclusion at query time.
    LocalQuery result = CatalogLocalRequestBuilder.build(
        CatalogEntityType.VULNERABILITY, Map.of("firstSeenWindow", "1y"));
    assertThat(result.fieldClauses().get(0)).doesNotContain("[* TO");
  }

  private static void assertLowerBoundDays(final String token, final int days) {
    long before = Instant.now().minus(Duration.ofDays(days)).toEpochMilli();
    LocalQuery result = CatalogLocalRequestBuilder.build(
        CatalogEntityType.VULNERABILITY, Map.of("firstSeenWindow", token));
    long after = Instant.now().minus(Duration.ofDays(days)).toEpochMilli();
    String clause = result.fieldClauses().get(0);
    long start = Long.parseLong(clause.substring(clause.indexOf('[') + 1, clause.indexOf(" TO ")));
    assertThat(start).as("window '%s'", token).isBetween(before, after);
  }
}
