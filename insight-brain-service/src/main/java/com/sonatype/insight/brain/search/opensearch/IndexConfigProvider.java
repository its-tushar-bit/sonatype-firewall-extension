/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

public interface IndexConfigProvider
{
  /**
   * Provides the configuration for the OpenSearch index.
   *
   * @return IndexConfig containing the index configuration details.
   */
  IndexConfig getIndexConfig();
}
