/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

public class IndexConfig
{
  private String indexName;

  private IndexMapping indexMapping = new IndexMapping();

  public String getIndexName() {
    return indexName;
  }

  public void setIndexName(final String indexName) {
    this.indexName = indexName;
  }

  public IndexMapping getIndexMapping() {
    return indexMapping;
  }

  public void setIndexMapping(final IndexMapping indexMapping) {
    this.indexMapping = indexMapping;
  }
}
