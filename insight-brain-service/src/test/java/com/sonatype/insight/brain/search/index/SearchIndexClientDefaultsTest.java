/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.search.global.GlobalSearchRequest;
import com.sonatype.insight.brain.search.results.SearchResultDTO;

import org.junit.Test;

/**
 * Pins the {@link SearchIndexClient} default methods that form the Global Search security backbone:
 * a minimal implementation (only the truly-abstract methods) must fail closed
 * ({@link SearchIndexClient#isGlobalSearchEnabled()} is {@code false}) and fail loud
 * ({@code searchGlobal}, {@code backendId}, {@code checkGlobalSearchMode},
 * {@code getCurrentUserContextIdsWithReadPermission} all throw {@link UnsupportedOperationException})
 * so a future edit cannot silently flip a default to an unsafe value.
 */
public class SearchIndexClientDefaultsTest
{
  /** Implements only the abstract methods, inheriting every default verbatim. */
  private static final class MinimalSearchIndexClient
      implements SearchIndexClient
  {
    @Override
    public void populateIndex() {
    }

    @Override
    public void updateIndex(final List<SearchIndexChange> changes, final Consumer<SearchIndexChange> deletion) {
    }

    @Override
    public void deleteSearchIndexChange(final SearchIndexChange change) {
    }

    @Override
    public Long getLastIndexTime() {
      return null;
    }

    @Override
    public long getIndexSize() {
      return 0L;
    }

    @Override
    public SearchResultDTO searchIndex(
        final String searchQuery,
        final int pageSize,
        final int page,
        final boolean allComponents,
        final boolean isSbomManagerMode,
        final List<String> searchAfter)
    {
      return null;
    }

    @Override
    public List<SearchIndexChange> getSearchIndexChanges() {
      return List.of();
    }

    @Override
    public long count(final String metricQuery) {
      return 0L;
    }

    @Override
    public MetricAggregationResult aggregateCountByField(
        final String metricQuery,
        final String bucketField,
        final Map<String, int[]> ranges)
    {
      return null;
    }

    @Override
    public MetricAggregationResult aggregateCountByFloatField(
        final String metricQuery,
        final String bucketField,
        final Map<String, float[]> ranges,
        final String distinctField)
    {
      return null;
    }

    @Override
    public long countDistinct(final String metricQuery, final List<String> compositeKeyFields) {
      return 0L;
    }

    @Override
    public Map<String, Long> countDistinctGroupedBy(
        final String metricQuery,
        final String groupField,
        final String distinctField,
        final Collection<String> groupValues)
    {
      return Map.of();
    }

    @Override
    public Map<String, Map<String, Long>> countDistinctGroupedByBands(
        final String metricQuery,
        final String groupField,
        final String distinctField,
        final Collection<String> groupValues,
        final String bandField,
        final Map<String, int[]> bands)
    {
      return Map.of();
    }
  }

  private final SearchIndexClient client = new MinimalSearchIndexClient();

  @Test
  public void isGlobalSearchEnabled_defaultsToFalse_failClosed() {
    assertThat(client.isGlobalSearchEnabled()).isFalse();
  }

  @Test
  public void searchGlobal_defaultThrows_failLoud() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> client.searchGlobal((GlobalSearchRequest) null));
  }

  @Test
  public void backendId_defaultThrows_failLoud() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(client::backendId);
  }

  @Test
  public void checkGlobalSearchMode_defaultThrows_failLoud() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> client.checkGlobalSearchMode(false));
  }

  @Test
  public void getCurrentUserContextIdsWithReadPermission_defaultThrows_failLoud() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(client::getCurrentUserContextIdsWithReadPermission);
  }
}
