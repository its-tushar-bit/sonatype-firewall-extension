/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.query;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryCollector;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics.SearchCount;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryCollector.TOTAL_SEARCHES;
import static com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryCollector.TOTAL_SEARCHES_BY_FIELD_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SearchServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SearchService searchService;

  @Inject
  private IndexService indexService;

  @Inject
  private InsightWork insightWork;

  @Inject
  private AdvancedSearchTelemetryCollector advancedSearchTelemetryCollector;

  @Test
  public void testSearchIndex_NoSearchIndexDirectory() {
    assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> searchService.searchIndex("query", 1, 1))
        .withMessageContaining("Index does not exist or is unreadable, please (re)create your index.");
  }

  @Test
  public void testSearchIndex_EmptySearchIndexDirectory() throws Exception {
    Files.createDirectories(insightWork.getSearchIndexDir().toPath());
    assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> searchService.searchIndex("query", 1, 1))
        .withMessageContaining("Index does not exist or is unreadable, please (re)create your index.");
  }

  private TelemetryData collectSearchTelemetry() {
    return advancedSearchTelemetryCollector.collectAllData().stream()
        .filter(telemetryData -> TelemetryPurpose.ADVANCED_SEARCH.equals(telemetryData.getPurpose())).findAny()
        .orElse(null);
  }

  @Test
  public void testSearchIndex_Telemetry() throws Exception {
    indexService.createSearchIndex();
    searchService.searchIndex("organizationName:org1 itemType:it2", 1, 0);
    searchService.searchIndex("itemType:it1", 1, 0);
    TelemetryData telemetryData = collectSearchTelemetry();

    @SuppressWarnings("unchecked")
    List<SearchCount> actualSearchCounts =
        (List<SearchCount>) telemetryData.getAttributes().get(TOTAL_SEARCHES_BY_FIELD_NAME);
    List<SearchCount> expectedSearchCounts =
        Arrays.asList(new SearchCount("organizationName", 1), new SearchCount("itemType", 2));

    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.ADVANCED_SEARCH);
    assertThat(telemetryData.getAttributes()).containsOnlyKeys(TOTAL_SEARCHES, TOTAL_SEARCHES_BY_FIELD_NAME);
    assertThat(actualSearchCounts).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedSearchCounts);
    assertThat(telemetryData.getAttributes().get(TOTAL_SEARCHES)).isEqualTo(2L);
  }

  @Test
  public void testSearchIndex_TelemetryNotAddedWhenPagingThroughResults() throws Exception {
    indexService.createSearchIndex();
    searchService.searchIndex("itemType:it1", 10, 1);
    TelemetryData telemetryData = collectSearchTelemetry();

    assertThat(telemetryData).isNull();
  }

  @Test
  public void testSearchIndex_TelemetryInvalidFieldNameCaptured() throws Exception {
    indexService.createSearchIndex();

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      searchService.searchIndex("invalidFieldName:value", 1, 0);
    });

    TelemetryData telemetryData = collectSearchTelemetry();

    @SuppressWarnings("unchecked")
    List<SearchCount> actualSearchCounts =
        (List<SearchCount>) telemetryData.getAttributes().get(TOTAL_SEARCHES_BY_FIELD_NAME);
    List<SearchCount> expectedSearchCounts =
        Arrays.asList(new SearchCount("invalidFieldName", 1));

    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.ADVANCED_SEARCH);
    assertThat(telemetryData.getAttributes()).containsOnlyKeys(TOTAL_SEARCHES, TOTAL_SEARCHES_BY_FIELD_NAME);
    assertThat(actualSearchCounts).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedSearchCounts);
    assertThat(telemetryData.getAttributes().get(TOTAL_SEARCHES)).isEqualTo(1L);
  }

  @Test
  public void testSearchIndex_TelemetryDuplicateFieldNamesInQueryAreIgnored() throws Exception {
    indexService.createSearchIndex();
    searchService.searchIndex("itemType:it1 itemType:it2", 1, 0);
    TelemetryData telemetryData = collectSearchTelemetry();

    @SuppressWarnings("unchecked")
    List<SearchCount> actualSearchCounts =
        (List<SearchCount>) telemetryData.getAttributes().get(TOTAL_SEARCHES_BY_FIELD_NAME);
    List<SearchCount> expectedSearchCounts = Arrays.asList(new SearchCount("itemType", 1));

    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.ADVANCED_SEARCH);
    assertThat(telemetryData.getAttributes()).containsOnlyKeys(TOTAL_SEARCHES, TOTAL_SEARCHES_BY_FIELD_NAME);
    assertThat(actualSearchCounts).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedSearchCounts);
    assertThat(telemetryData.getAttributes().get(TOTAL_SEARCHES)).isEqualTo(1L);
  }
}
