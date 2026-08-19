/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.search.LuceneSearchConfig;
import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.search.SearchConfig.AwsHttpOpenSearchConfig;
import com.sonatype.insight.brain.search.SearchConfig.HttpOpenSearchConfig;
import com.sonatype.insight.brain.search.SearchConfigSupplier;
import com.sonatype.insight.brain.search.SearchMode;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reports the configured search index backend (Lucene vs OpenSearch, and the
 * OpenSearch flavour when applicable). Mirrors the shape of
 * {@link DatabaseTelemetryCollector}: one record per cluster per cluster-telemetry
 * cycle, under a dedicated {@link TelemetryPurpose}.
 *
 * @since 1.207
 */
@Named
@Singleton
public class SearchBackendTelemetryCollector
    implements TelemetryCollector
{
  private static final Logger log = LoggerFactory.getLogger(SearchBackendTelemetryCollector.class);

  public static final String SEARCH_MODE = "search_mode";

  public static final String SEARCH_BACKEND_TYPE = "search_backend_type";

  private final SearchConfigSupplier searchConfigSupplier;

  @Inject
  public SearchBackendTelemetryCollector(final SearchConfigSupplier searchConfigSupplier) {
    this.searchConfigSupplier = searchConfigSupplier;
  }

  @Override
  public TelemetryData collectData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SEARCH_BACKEND);
    Map<String, Object> attributes = telemetryData.getAttributes();

    SearchConfig searchConfig = searchConfigSupplier.getSearchConfig();
    if (searchConfig == null) {
      // No search: block in config.yml -> runtime falls back to Lucene-only.
      attributes.put(SEARCH_MODE, SearchMode.LUCENE.name());
      attributes.put(SEARCH_BACKEND_TYPE, "none");
    }
    else {
      attributes.put(SEARCH_MODE, searchConfig.getMode().name());
      attributes.put(SEARCH_BACKEND_TYPE, backendTypeOf(searchConfig));
    }
    return telemetryData;
  }

  private String backendTypeOf(final SearchConfig searchConfig) {
    // AwsHttpOpenSearchConfig and HttpOpenSearchConfig are siblings (both extend
    // AbstractSearchConfig), not parent/child, so the check order below is not load-bearing
    // today — but please don't "tidy" it without confirming that's still the case.
    if (searchConfig instanceof AwsHttpOpenSearchConfig) {
      return "aws";
    }
    if (searchConfig instanceof HttpOpenSearchConfig) {
      return "http";
    }
    if (searchConfig instanceof LuceneSearchConfig) {
      return "none";
    }
    // Future-proofing: if a new SearchConfig subtype is added but this collector isn't updated,
    // we report a distinct "unknown" rather than "none" so it stands out in the data and
    // doesn't get silently bucketed with Lucene/no-config installations.
    log.warn("Unknown SearchConfig type for telemetry: {}; reporting backend type 'unknown'",
        searchConfig.getClass().getName());
    return "unknown";
  }

  @Override
  public boolean isClusterTelemetry() {
    return true;
  }
}
