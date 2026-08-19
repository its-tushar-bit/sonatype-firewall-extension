/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.session.ReadableContextAuthzCache;
import com.sonatype.insight.brain.security.CurrentUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AbstractSearchIndexClientReadableContextsTest
{
  @Mock
  private ReadableContextAuthzCache readableContextAuthzCache;

  @Mock
  private CurrentUser currentUser;

  @Mock
  private UserPrincipal principal;

  private TestClient client;

  @BeforeEach
  public void setUp() {
    when(currentUser.getUserPrincipal()).thenReturn(principal);
    client = new TestClient(currentUser, readableContextAuthzCache);
  }

  @Test
  public void getCurrentUserContextIdsWithReadPermission_unrestricted_returnsGlobalSentinel() {
    when(readableContextAuthzCache.resolveReadableContexts(principal)).thenReturn(Optional.empty());

    assertThat(client.getCurrentUserContextIdsWithReadPermission())
        .containsExactly(MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void getCurrentUserContextIdsWithReadPermission_restricted_returnsContextKeys() {
    when(readableContextAuthzCache.resolveReadableContexts(principal))
        .thenReturn(Optional.of(Map.of(
            "org-1", OwnerType.ORGANIZATION,
            "app-1", OwnerType.APPLICATION)));

    assertThat(client.getCurrentUserContextIdsWithReadPermission())
        .containsExactlyInAnyOrder("org-1", "app-1");
  }

  @Test
  public void getCurrentUserContextIdsWithReadPermission_none_returnsEmpty() {
    when(readableContextAuthzCache.resolveReadableContexts(principal))
        .thenReturn(Optional.of(Map.of()));

    assertThat(client.getCurrentUserContextIdsWithReadPermission()).isEmpty();
  }

  private static final class TestClient
      extends AbstractSearchIndexClient
  {
    private TestClient(final CurrentUser currentUser, final ReadableContextAuthzCache cache) {
      super(null, null, null, null, null, null, null, null, null, null, null, null, null, null,
          null, null, null, null, currentUser, null, null, cache);
    }

    @Override
    protected void updateMaxQueryClauseCount() {
    }

    @Override
    protected void updateIndex(SearchIndexChange change, IndexingContext indexingContext) {
    }

    @Override
    public SearchResultDTO searchIndex(
        String q,
        int pageSize,
        int page,
        boolean allComponents,
        boolean isSbomManagerMode,
        List<String> searchAfter)
    {
      return null;
    }

    @Override
    public void populateIndex() {
    }

    @Override
    public void updateIndex(List<SearchIndexChange> changes, Consumer<SearchIndexChange> cb) {
    }

    @Override
    public void updateIndex() {
    }

    @Override
    public Long getLastIndexTime() {
      return null;
    }

    @Override
    public long getIndexSize() {
      return 0;
    }

    @Override
    protected boolean isChangeSpecificError(Exception e) {
      return false;
    }

    @Override
    protected boolean isSystemicError(Exception e) {
      return false;
    }

    @Override
    public long countDistinct(String metricQuery, List<String> compositeKeyFields) {
      return 0;
    }

    @Override
    public Map<String, Long> countDistinctGroupedBy(
        String metricQuery,
        String groupField,
        String distinctField,
        Collection<String> groupValues)
    {
      return Map.of();
    }

    @Override
    public Map<String, Map<String, Long>> countDistinctGroupedByBands(
        String metricQuery,
        String groupField,
        String distinctField,
        Collection<String> groupValues,
        String bandField,
        Map<String, int[]> bands)
    {
      return Map.of();
    }

    @Override
    public long count(String metricQuery) {
      return 0;
    }

    @Override
    public MetricAggregationResult aggregateCountByField(
        String metricQuery,
        String bucketField,
        Map<String, int[]> ranges)
    {
      return null;
    }

    @Override
    public MetricAggregationResult aggregateCountByFloatField(
        String metricQuery,
        String bucketField,
        Map<String, float[]> ranges,
        String distinctField)
    {
      return null;
    }
  }
}
