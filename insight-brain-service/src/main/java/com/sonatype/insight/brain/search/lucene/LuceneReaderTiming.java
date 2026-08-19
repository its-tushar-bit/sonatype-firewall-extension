/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Request-scoped Lucene list timing broken down into acquisition, query build/parse, search, and
 * document grouping phases.
 */
public final class LuceneReaderTiming
{
  private static final Logger log = LoggerFactory.getLogger(LuceneReaderTiming.class);

  private static final ThreadLocal<TimingState> active = new ThreadLocal<>();

  private LuceneReaderTiming() {
  }

  public static void reset() {
    active.remove();
  }

  public static void startAcquisition() {
    reset();
    state().acquisitionStart = System.nanoTime();
  }

  public static void abort() {
    reset();
  }

  public static void endAcquisition() {
    TimingState state = state();
    state.lastAcquisitionNanos = System.nanoTime() - state.acquisitionStart;
  }

  public static void startExecution() {
    state().executionStart = System.nanoTime();
  }

  /** Query string assembly (including RBAC append) before parse. */
  public static void startQueryBuild() {
    state().queryBuildStart = System.nanoTime();
  }

  public static void endQueryBuild(final String finalQuery, final int rbacContextCount) {
    TimingState state = state();
    state.lastQueryBuildNanos = System.nanoTime() - state.queryBuildStart;
    state.lastFinalQueryChars = finalQuery == null ? 0 : finalQuery.length();
    state.lastFinalQueryOrClauses = finalQuery == null ? 0 : countOccurrences(finalQuery, " OR ");
    state.lastRbacContextCount = rbacContextCount;
  }

  /** {@code stringToQuery} / QueryParser. */
  public static void startQueryParse() {
    state().queryParseStart = System.nanoTime();
  }

  public static void endQueryParse() {
    TimingState state = state();
    state.lastQueryParseNanos = System.nanoTime() - state.queryParseStart;
  }

  private static int countOccurrences(final String haystack, final String needle) {
    int count = 0;
    int from = 0;
    while ((from = haystack.indexOf(needle, from)) >= 0) {
      count++;
      from += needle.length();
    }
    return count;
  }

  public static void startSearch() {
    state().searchStart = System.nanoTime();
  }

  public static void endSearch(final int maxDoc, final int scoreDocsLength, final long totalHits) {
    TimingState state = state();
    state.lastSearchNanos = System.nanoTime() - state.searchStart;
    state.lastMaxDoc = maxDoc;
    state.lastScoreDocsLength = scoreDocsLength;
    state.lastTotalHits = totalHits;
  }

  public static void startGroupDocuments() {
    state().groupStart = System.nanoTime();
  }

  public static void endGroupDocuments() {
    TimingState state = state();
    state.lastGroupNanos = System.nanoTime() - state.groupStart;
  }

  public static void recordStoredFieldDocumentLoad() {
    state().storedFieldDocumentLoads++;
  }

  public static TimingState endExecution() {
    TimingState state = state();
    state.lastExecutionNanos = System.nanoTime() - state.executionStart;
    TimingState result = state.snapshot();
    active.remove();
    try {
      log.debug("DASHBOARD_BENCHMARK metric=lucene_reader phase=acquisition durationMs={}",
          result.lastAcquisitionNanos / 1_000_000L);
      log.debug("DASHBOARD_BENCHMARK metric=lucene_reader phase=execution durationMs={}",
          result.lastExecutionNanos / 1_000_000L);
      log.debug(
          "DASHBOARD_BENCHMARK metric=lucene_reader phase=queryBuild durationMs={} finalQueryChars={} orClauses={} rbacContexts={}",
          result.lastQueryBuildNanos / 1_000_000L, result.lastFinalQueryChars, result.lastFinalQueryOrClauses,
          result.lastRbacContextCount);
      log.debug("DASHBOARD_BENCHMARK metric=lucene_reader phase=queryParse durationMs={}",
          result.lastQueryParseNanos / 1_000_000L);
      log.debug(
          "DASHBOARD_BENCHMARK metric=lucene_reader phase=search durationMs={} maxDoc={} scoreDocs={} totalHits={}",
          result.lastSearchNanos / 1_000_000L, result.lastMaxDoc, result.lastScoreDocsLength, result.lastTotalHits);
      log.debug(
          "DASHBOARD_BENCHMARK metric=lucene_reader phase=groupDocuments durationMs={} storedFieldLoads={}",
          result.lastGroupNanos / 1_000_000L, result.storedFieldDocumentLoads);
    }
    catch (RuntimeException ignored) {
      // logging must never fail the request
    }
    return result;
  }

  public static long lastAcquisitionNanos() {
    return measurements().lastAcquisitionNanos;
  }

  public static long lastExecutionNanos() {
    return measurements().lastExecutionNanos;
  }

  public static long lastQueryBuildNanos() {
    return measurements().lastQueryBuildNanos;
  }

  public static long lastQueryParseNanos() {
    return measurements().lastQueryParseNanos;
  }

  public static int lastFinalQueryChars() {
    return measurements().lastFinalQueryChars;
  }

  public static int lastFinalQueryOrClauses() {
    return measurements().lastFinalQueryOrClauses;
  }

  public static int lastRbacContextCount() {
    return measurements().lastRbacContextCount;
  }

  public static long lastSearchNanos() {
    return measurements().lastSearchNanos;
  }

  public static long lastGroupNanos() {
    return measurements().lastGroupNanos;
  }

  public static int storedFieldDocumentLoads() {
    return measurements().storedFieldDocumentLoads;
  }

  private static TimingState state() {
    TimingState state = active.get();
    if (state == null) {
      state = new TimingState();
      active.set(state);
    }
    return state;
  }

  private static TimingState measurements() {
    TimingState state = active.get();
    return state == null ? new TimingState() : state;
  }

  public static final class TimingState
  {
    private long acquisitionStart;

    private long executionStart;

    private long queryBuildStart;

    private long queryParseStart;

    private long searchStart;

    private long groupStart;

    private long lastAcquisitionNanos;

    private long lastExecutionNanos;

    private long lastQueryBuildNanos;

    private long lastQueryParseNanos;

    private int lastFinalQueryChars;

    private int lastFinalQueryOrClauses;

    /** -1 = unrestricted/global (no RBAC clause); else number of readable context ids. */
    private int lastRbacContextCount = -1;

    private long lastSearchNanos;

    private long lastGroupNanos;

    private int lastMaxDoc;

    private int lastScoreDocsLength;

    private long lastTotalHits;

    private int storedFieldDocumentLoads;

    private TimingState snapshot() {
      TimingState snapshot = new TimingState();
      snapshot.lastAcquisitionNanos = lastAcquisitionNanos;
      snapshot.lastExecutionNanos = lastExecutionNanos;
      snapshot.lastQueryBuildNanos = lastQueryBuildNanos;
      snapshot.lastQueryParseNanos = lastQueryParseNanos;
      snapshot.lastFinalQueryChars = lastFinalQueryChars;
      snapshot.lastFinalQueryOrClauses = lastFinalQueryOrClauses;
      snapshot.lastRbacContextCount = lastRbacContextCount;
      snapshot.lastSearchNanos = lastSearchNanos;
      snapshot.lastGroupNanos = lastGroupNanos;
      snapshot.lastMaxDoc = lastMaxDoc;
      snapshot.lastScoreDocsLength = lastScoreDocsLength;
      snapshot.lastTotalHits = lastTotalHits;
      snapshot.storedFieldDocumentLoads = storedFieldDocumentLoads;
      return snapshot;
    }

    public long lastAcquisitionNanos() {
      return lastAcquisitionNanos;
    }

    public long lastExecutionNanos() {
      return lastExecutionNanos;
    }

    public long lastQueryBuildNanos() {
      return lastQueryBuildNanos;
    }

    public long lastQueryParseNanos() {
      return lastQueryParseNanos;
    }

    public int lastFinalQueryChars() {
      return lastFinalQueryChars;
    }

    public int lastFinalQueryOrClauses() {
      return lastFinalQueryOrClauses;
    }

    public int lastRbacContextCount() {
      return lastRbacContextCount;
    }

    public long lastSearchNanos() {
      return lastSearchNanos;
    }

    public long lastGroupNanos() {
      return lastGroupNanos;
    }

    public int storedFieldDocumentLoads() {
      return storedFieldDocumentLoads;
    }
  }
}
