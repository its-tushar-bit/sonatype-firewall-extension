/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.index.VulnerabilityDescriptionFetcher;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics.SearchCount;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryCollector.TOTAL_SEARCHES;
import static com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryCollector.TOTAL_SEARCHES_BY_FIELD_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class AdvancedSearchTelemetryCollectorTest
    extends AbstractComponentTest
{
  @Inject
  private AdvancedSearchTelemetryCollector collector;

  @Inject
  private AdvancedSearchTelemetryMetrics metrics;

  @Inject
  private IndexService indexService;

  @Mock
  private VulnerabilityDescriptionFetcher vulnerabilityDescriptionFetcher;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(VulnerabilityDescriptionFetcher.class).toInstance(vulnerabilityDescriptionFetcher);
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    super.configure(binder);
  }

  @Test
  public void testCollectAllData_NoData() {
    assertThat(collector.collectAllData()).isEmpty();
  }

  @Test
  public void testCollectAllData_SearchData() {
    metrics.addSearch(new HashSet<>(Collections.singletonList("foo")));
    metrics.addSearch(new HashSet<>(Collections.singletonList("foo")));

    List<TelemetryData> allTelemetryData = collector.collectAllData();
    assertThat(allTelemetryData).hasSize(1);

    TelemetryData telemetryData = allTelemetryData.get(0);
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.ADVANCED_SEARCH);
    @SuppressWarnings("unchecked")
    List<SearchCount> actualSearchCounts =
        (List<SearchCount>) telemetryData.getAttributes().get(TOTAL_SEARCHES_BY_FIELD_NAME);
    List<SearchCount> expectedSearchCounts = Collections.singletonList(new SearchCount("foo", 2));

    assertThat(telemetryData.getAttributes()).containsOnlyKeys(TOTAL_SEARCHES_BY_FIELD_NAME, TOTAL_SEARCHES);
    assertThat(actualSearchCounts).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedSearchCounts);
    assertThat(telemetryData.getAttributes().get(TOTAL_SEARCHES)).isEqualTo(2L);
  }

  @Test
  public void testCollectAllData_IndexData() throws Exception {
    indexService.createSearchIndex();

    List<TelemetryData> allTelemetryData = collector.collectAllData();
    assertThat(allTelemetryData).hasSize(1);

    TelemetryData telemetryData = allTelemetryData.get(0);
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.ADVANCED_SEARCH_INDEXING);
    assertThat(telemetryData.getAttributes())
        .containsEntry(SearchIndexClient.SEARCH_INDEX_SIZE_BYTES, indexService.getIndexSize())
        .containsEntry(SearchIndexClient.SEARCH_INDEX_REINDEX, false);
  }

  @Test
  public void testIsClusterTelemetry() {
    assertThat(collector.isClusterTelemetry()).isFalse();
  }
}
