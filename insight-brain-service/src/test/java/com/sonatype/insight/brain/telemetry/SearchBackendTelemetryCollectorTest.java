/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.search.LuceneSearchConfig;
import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.search.SearchConfig.AwsHttpOpenSearchConfig;
import com.sonatype.insight.brain.search.SearchConfig.HttpOpenSearchConfig;
import com.sonatype.insight.brain.search.SearchConfigSupplier;
import com.sonatype.insight.brain.search.SearchMode;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

public class SearchBackendTelemetryCollectorTest
{
  @Test
  public void testCollectData_TelemetryPurpose() {
    TelemetryData data = collectorFor(null).collectData();

    assertThat(data.getPurpose()).isEqualTo(TelemetryPurpose.SEARCH_BACKEND);
  }

  @Test
  public void testIsClusterTelemetry() {
    assertThat(collectorFor(null).isClusterTelemetry()).isTrue();
  }

  @Test
  public void testCollectData_NoSearchConfig_ReportsLuceneAndNone() {
    TelemetryData data = collectorFor(null).collectData();

    assertThat(data.getAttributes())
        .containsEntry(SearchBackendTelemetryCollector.SEARCH_MODE, SearchMode.LUCENE.name())
        .containsEntry(SearchBackendTelemetryCollector.SEARCH_BACKEND_TYPE, "none");
  }

  @Test
  public void testCollectData_LuceneSearchConfig_ExplicitMode_ReportsLuceneAndNone() {
    LuceneSearchConfig config = new LuceneSearchConfig();
    config.setMode(SearchMode.LUCENE);

    TelemetryData data = collectorFor(config).collectData();

    assertThat(data.getAttributes())
        .containsEntry(SearchBackendTelemetryCollector.SEARCH_MODE, SearchMode.LUCENE.name())
        .containsEntry(SearchBackendTelemetryCollector.SEARCH_BACKEND_TYPE, "none");
  }

  /**
   * The more common real-world Lucene config: {@code search: {type: lucene}} in config.yml with no explicit
   * {@code mode:} entry, exercising the {@link LuceneSearchConfig#getMode()} null-safe fallback to {@code LUCENE}.
   * Mirrors the {@code DefaultMode} / {@code ExplicitOpenSearchMode} pair used for HTTP and AWS.
   */
  @Test
  public void testCollectData_LuceneSearchConfig_DefaultMode_ReportsLuceneAndNone() {
    LuceneSearchConfig config = new LuceneSearchConfig();
    // mode intentionally not set -> LuceneSearchConfig.getMode() returns SearchMode.LUCENE by default

    TelemetryData data = collectorFor(config).collectData();

    assertThat(data.getAttributes())
        .containsEntry(SearchBackendTelemetryCollector.SEARCH_MODE, SearchMode.LUCENE.name())
        .containsEntry(SearchBackendTelemetryCollector.SEARCH_BACKEND_TYPE, "none");
  }

  @Test
  public void testCollectData_HttpOpenSearch_DefaultMode_ReportsHybridAndHttp() {
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    // mode unset -> defaults to HYBRID at runtime

    TelemetryData data = collectorFor(config).collectData();

    assertThat(data.getAttributes())
        .containsEntry(SearchBackendTelemetryCollector.SEARCH_MODE, SearchMode.HYBRID.name())
        .containsEntry(SearchBackendTelemetryCollector.SEARCH_BACKEND_TYPE, "http");
  }

  @Test
  public void testCollectData_HttpOpenSearch_ExplicitOpenSearchMode_ReportsOpenSearchAndHttp() {
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setMode(SearchMode.OPENSEARCH);

    TelemetryData data = collectorFor(config).collectData();

    assertThat(data.getAttributes())
        .containsEntry(SearchBackendTelemetryCollector.SEARCH_MODE, SearchMode.OPENSEARCH.name())
        .containsEntry(SearchBackendTelemetryCollector.SEARCH_BACKEND_TYPE, "http");
  }

  @Test
  public void testCollectData_HttpOpenSearch_ExplicitLuceneMode_ReportsLuceneAndHttp() {
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setMode(SearchMode.LUCENE);

    TelemetryData data = collectorFor(config).collectData();

    assertThat(data.getAttributes())
        .containsEntry(SearchBackendTelemetryCollector.SEARCH_MODE, SearchMode.LUCENE.name())
        .containsEntry(SearchBackendTelemetryCollector.SEARCH_BACKEND_TYPE, "http");
  }

  @Test
  public void testCollectData_AwsOpenSearch_DefaultMode_ReportsHybridAndAws() {
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    // mode unset -> defaults to HYBRID at runtime

    TelemetryData data = collectorFor(config).collectData();

    assertThat(data.getAttributes())
        .containsEntry(SearchBackendTelemetryCollector.SEARCH_MODE, SearchMode.HYBRID.name())
        .containsEntry(SearchBackendTelemetryCollector.SEARCH_BACKEND_TYPE, "aws");
  }

  @Test
  public void testCollectData_AwsOpenSearch_ExplicitOpenSearchMode_ReportsOpenSearchAndAws() {
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setMode(SearchMode.OPENSEARCH);

    TelemetryData data = collectorFor(config).collectData();

    assertThat(data.getAttributes())
        .containsEntry(SearchBackendTelemetryCollector.SEARCH_MODE, SearchMode.OPENSEARCH.name())
        .containsEntry(SearchBackendTelemetryCollector.SEARCH_BACKEND_TYPE, "aws");
  }

  @Test
  public void testCollectData_AwsOpenSearch_ExplicitLuceneMode_ReportsLuceneAndAws() {
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setMode(SearchMode.LUCENE);

    TelemetryData data = collectorFor(config).collectData();

    assertThat(data.getAttributes())
        .containsEntry(SearchBackendTelemetryCollector.SEARCH_MODE, SearchMode.LUCENE.name())
        .containsEntry(SearchBackendTelemetryCollector.SEARCH_BACKEND_TYPE, "aws");
  }

  @Test
  public void testCollectData_UnknownSearchConfigSubtype_ReportsUnknownBackendType() {
    // Simulate a hypothetical future SearchConfig subtype that this collector hasn't been
    // updated to recognise. The fallback should report "unknown" rather than "none" so the
    // gap stands out in downstream data.
    SearchConfig unknownConfig = new SearchConfig()
    {
      @Override
      public SearchMode getMode() {
        return SearchMode.HYBRID;
      }

      @Override
      public void validate() {
        // no-op for test
      }
    };

    TelemetryData data = collectorFor(unknownConfig).collectData();

    assertThat(data.getAttributes())
        .containsEntry(SearchBackendTelemetryCollector.SEARCH_MODE, SearchMode.HYBRID.name())
        .containsEntry(SearchBackendTelemetryCollector.SEARCH_BACKEND_TYPE, "unknown");
  }

  private static SearchBackendTelemetryCollector collectorFor(final SearchConfig searchConfig) {
    SearchConfigSupplier supplier = () -> searchConfig;
    return new SearchBackendTelemetryCollector(supplier);
  }
}
