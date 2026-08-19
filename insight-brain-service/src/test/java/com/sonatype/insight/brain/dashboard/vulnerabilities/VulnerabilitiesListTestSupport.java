/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import com.sonatype.insight.brain.search.index.SearchIndexClient;

/** Shared index fixtures for the Vulnerabilities list integration tests (CLM-42216). */
final class VulnerabilitiesListTestSupport
{
  private static final Object INDEX_POPULATE_LOCK = new Object();

  private VulnerabilitiesListTestSupport() {
  }

  static void populateIndex(final SearchIndexClient searchIndexClient) {
    synchronized (INDEX_POPULATE_LOCK) {
      searchIndexClient.populateIndex();
    }
  }
}
