/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

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
}
