/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics.SearchCount;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

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

  @Test
  public void testCollectData_TelemetryPurpose() {
    metrics.addSearch(new HashSet<>(Arrays.asList("foo")));
    TelemetryData telemetryData = collector.collectData();

    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.ADVANCED_SEARCH);
  }

  @Test
  public void testCollectData() {
    metrics.addSearch(new HashSet<>(Arrays.asList("foo")));
    metrics.addSearch(new HashSet<>(Arrays.asList("foo")));
    TelemetryData telemetryData = collector.collectData();

    @SuppressWarnings("unchecked")
    List<SearchCount> actualSearchCounts =
        (List<SearchCount>) telemetryData.getAttributes().get(TOTAL_SEARCHES_BY_FIELD_NAME);
    List<SearchCount> expectedSearchCounts = Arrays.asList(new SearchCount("foo", 2));

    assertThat(telemetryData.getAttributes()).containsOnlyKeys(TOTAL_SEARCHES_BY_FIELD_NAME, TOTAL_SEARCHES);
    assertThat(actualSearchCounts).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedSearchCounts);
    assertThat(telemetryData.getAttributes().get(TOTAL_SEARCHES)).isEqualTo(2L);
  }

  @Test
  public void testCollectData_NoData() {
    assertThat(collector.collectData()).isNull();
  }
}
