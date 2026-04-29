/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

/**
 * Search configuration for pure Lucene mode. No external search service connection is needed.
 * This is the default when no {@code type} discriminator is present in the search config.
 */
public class LuceneSearchConfig
    implements SearchConfig
{
  private SearchMode mode;

  @Override
  public SearchMode getMode() {
    return mode != null ? mode : SearchMode.LUCENE;
  }

  public void setMode(final SearchMode mode) {
    this.mode = mode;
  }

  @Override
  public void validate() {
    if (mode != null && mode != SearchMode.LUCENE) {
      throw new SearchConfigurationException(
          "Search mode '" + mode + "' requires a search type to be configured (e.g. http or aws)");
    }
  }
}
