/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.io.File;
import java.io.IOException;

import com.sonatype.insight.brain.search.index.SearchIndexClient;

import org.apache.commons.io.FileUtils;

/** Shared index fixtures for CLM-42214 components list integration tests. */
final class ComponentsListTestSupport
{
  private static final Object INDEX_POPULATE_LOCK = new Object();

  private ComponentsListTestSupport() {
  }

  static void populateIndex(final SearchIndexClient searchIndexClient) {
    synchronized (INDEX_POPULATE_LOCK) {
      searchIndexClient.populateIndex();
    }
  }

  static void runWithoutSearchIndex(final File searchIndexDir, final Runnable action) throws IOException {
    synchronized (INDEX_POPULATE_LOCK) {
      FileUtils.deleteDirectory(searchIndexDir);
      action.run();
    }
  }
}
