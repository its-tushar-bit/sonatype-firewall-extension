/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.search.ConversionHelper;

import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Test;

/**
 * Focused contract test for the {@link IndexingContext} load-on-miss memoization used by the
 * evaluation and violation rollups. The contract is: load an app on first request, load a
 * later app that was not in the first request (load-on-miss, NOT freeze-after-first-load), and cache
 * an app absent so a never-evaluated app is not re-queried.
 */
public class IndexingContextTest
{
  private static IndexingContext newContext() {
    return new IndexingContext(mock(OwnerDAO.class), mock(ConversionHelper.class))
    {
      @Override
      public void deleteDocuments(final String query) {
      }

      @Override
      public void addDocuments(final List<Document> documents) {
      }
    };
  }

  @Test
  public void latestEvaluation_loadsOnFirstRequest() {
    IndexingContext context = newContext();
    AtomicInteger loads = new AtomicInteger();
    Function<Set<String>, Map<String, Long>> loader = ids -> {
      loads.incrementAndGet();
      return Map.of("appA", 100L);
    };

    Map<String, Long> result = context.getLatestEvaluationEpochMsByApp(Set.of("appA"), loader);

    assertThat(result).containsEntry("appA", 100L);
    assertThat(loads.get()).isEqualTo(1);
  }

  @Test
  public void latestEvaluation_loadsOnMiss_forAppNotInFirstRequest() {
    // The Critical fix's contract: a later request with a DIFFERENT app id set loads that app too,
    // rather than freezing on the first caller's id set. Each call returns only the requested subset,
    // so appB's request loads appB on miss and appA's already-cached value is still returned for appA.
    IndexingContext context = newContext();
    AtomicInteger loads = new AtomicInteger();
    Function<Set<String>, Map<String, Long>> loader = ids -> {
      loads.incrementAndGet();
      // Return a row only for the requested (missing) ids so the batch mirrors the per-app load.
      return ids.contains("appB") ? Map.of("appB", 200L) : Map.of("appA", 100L);
    };

    Map<String, Long> first = context.getLatestEvaluationEpochMsByApp(Set.of("appA"), loader);
    Map<String, Long> second = context.getLatestEvaluationEpochMsByApp(Set.of("appB"), loader);

    // appB was not in the first request but still loads on miss (loader ran a second time for it).
    assertThat(first).containsEntry("appA", 100L);
    assertThat(second).containsEntry("appB", 200L);
    assertThat(loads.get()).isEqualTo(2);
    // appA stays cached: re-requesting it returns the memoized value without another load.
    assertThat(context.getLatestEvaluationEpochMsByApp(Set.of("appA"), loader)).containsEntry("appA", 100L);
    assertThat(loads.get()).isEqualTo(2);
  }

  @Test
  public void latestEvaluation_neverEvaluatedApp_isCachedAbsent_andNotRequeried() {
    IndexingContext context = newContext();
    AtomicInteger loads = new AtomicInteger();
    // The loader returns no row for appC (never evaluated).
    Function<Set<String>, Map<String, Long>> loader = ids -> {
      loads.incrementAndGet();
      return Map.of();
    };

    Map<String, Long> first = context.getLatestEvaluationEpochMsByApp(Set.of("appC"), loader);
    Map<String, Long> second = context.getLatestEvaluationEpochMsByApp(Set.of("appC"), loader);

    assertThat(first).doesNotContainKey("appC");
    assertThat(second).doesNotContainKey("appC");
    // appC is cached as "loaded, absent" after the first call, so the loader runs exactly once.
    assertThat(loads.get()).isEqualTo(1);
  }

  @Test
  public void violationRollup_loadsOnMiss_forAppNotInFirstRequest() {
    IndexingContext context = newContext();
    AtomicInteger loads = new AtomicInteger();
    IndexingContext.ViolationRollup rollupA = new IndexingContext.ViolationRollup(
        List.of("build:low:1"), null, Set.of(), Set.of(), Set.of(), null);
    IndexingContext.ViolationRollup rollupB = new IndexingContext.ViolationRollup(
        List.of("build:high:2"), null, Set.of(), Set.of(), Set.of(), null);
    Function<Set<String>, Map<String, IndexingContext.ViolationRollup>> loader = ids -> {
      loads.incrementAndGet();
      return ids.contains("appB") ? Map.of("appB", rollupB) : Map.of("appA", rollupA);
    };

    Map<String, IndexingContext.ViolationRollup> first = context.getViolationRollupByApp(Set.of("appA"), loader);
    Map<String, IndexingContext.ViolationRollup> second = context.getViolationRollupByApp(Set.of("appB"), loader);

    // appB was not in the first request but still loads on miss (loader ran a second time for it).
    assertThat(first).containsEntry("appA", rollupA);
    assertThat(second).containsEntry("appB", rollupB);
    assertThat(loads.get()).isEqualTo(2);
    // appA stays cached: re-requesting it returns the memoized value without another load.
    assertThat(context.getViolationRollupByApp(Set.of("appA"), loader)).containsEntry("appA", rollupA);
    assertThat(loads.get()).isEqualTo(2);
  }

  @Test
  public void violationRollup_appWithNoViolations_isCachedAbsent_andNotRequeried() {
    IndexingContext context = newContext();
    AtomicInteger loads = new AtomicInteger();
    Function<Set<String>, Map<String, IndexingContext.ViolationRollup>> loader = ids -> {
      loads.incrementAndGet();
      return Map.of();
    };

    context.getViolationRollupByApp(Set.of("appC"), loader);
    context.getViolationRollupByApp(Set.of("appC"), loader);

    assertThat(loads.get()).isEqualTo(1);
  }
}
