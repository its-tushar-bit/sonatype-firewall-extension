/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.util.Set;

import com.sonatype.insight.brain.search.lucene.LuceneIndexWriterOwner;
import com.sonatype.insight.brain.search.session.ReadableContextAuthzCache;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.tenancy.TenantManaged;

import jakarta.inject.Inject;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Confirms Spring {@code @Bean} TenantManaged search components join the injected
 * {@code Set<TenantManaged>} used by tenant lifecycle initializers.
 */
public class SearchTenantManagedWiringTest
    extends AbstractComponentTest
{
  @Inject
  private Set<TenantManaged> tenantManagedBeans;

  @Test
  public void luceneWriterAndAuthzCacheAreInTenantManagedSet() {
    assertThat(tenantManagedBeans).anyMatch(LuceneIndexWriterOwner.class::isInstance);
    assertThat(tenantManagedBeans).anyMatch(ReadableContextAuthzCache.class::isInstance);
  }
}
