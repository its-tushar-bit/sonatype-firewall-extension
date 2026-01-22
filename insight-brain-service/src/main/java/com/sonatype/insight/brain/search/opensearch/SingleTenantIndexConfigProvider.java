/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Named
@Singleton
public class SingleTenantIndexConfigProvider
    implements IndexConfigProvider
{
  @Override
  public IndexConfig getIndexConfig() {
    IndexConfig indexConfig = new IndexConfig();
    indexConfig.setIndexName("iq-index");

    return indexConfig;
  }
}
