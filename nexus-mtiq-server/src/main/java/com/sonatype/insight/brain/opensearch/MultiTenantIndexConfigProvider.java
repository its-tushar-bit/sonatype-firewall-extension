/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.opensearch;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.search.opensearch.IndexConfig;
import com.sonatype.insight.brain.search.opensearch.IndexConfigProvider;
import com.sonatype.insight.brain.tenancy.TenantManager;
import org.springframework.context.annotation.Primary;

@Named
@Singleton
@Primary
public class MultiTenantIndexConfigProvider
    implements IndexConfigProvider
{
  private final TenantManager tenantManager;

  @Inject
  public MultiTenantIndexConfigProvider(final TenantManager tenantManager) {
    this.tenantManager = tenantManager;
  }

  @Override
  public IndexConfig getIndexConfig() {
    IndexConfig indexConfig = new IndexConfig();
    String tenantSlug = tenantManager.getTenant().tenantSlug;

    indexConfig.setIndexName(tenantSlug + "-mtiq-index");

    return indexConfig;
  }
}
