/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.cache;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.datacache.QueryKey;
import org.apache.openjpa.datacache.QueryResult;
import org.apache.openjpa.event.RemoteCommitEventManager;
import org.apache.openjpa.persistence.DataCache;
import org.apache.openjpa.util.CacheMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.apache.commons.lang3.NotImplementedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MultiTenantQueryCacheTest
{
  private MultiTenantQueryCache underTest;

  @Mock
  QueryKey queryKey;

  @Mock
  OpenJPAConfiguration config;

  @Mock
  RemoteCommitEventManager remoteCommitMgr;

  @Before
  public void setUp() throws Exception {
    when(config.getRemoteCommitEventManager()).thenReturn(remoteCommitMgr);

    underTest = new TestMultiTenantQueryCache();
    underTest.setConfiguration(config);
    underTest.initialize(null);
  }

  @Test
  public void test_MultiTenantQueryCachePut_UnknownClassReturnNull() {
    when(queryKey.getCandidateTypeName()).thenReturn("foo");

    assertThat(underTest.put(queryKey, null)).isNull();
  }

  @Test
  public void test_MultiTenantQueryCachePut_ClassNotAnnotatedWithDataCache_ReturnsNull() {
    when(queryKey.getCandidateTypeName()).thenReturn(EntityWithoutCache.class.getName());

    assertThat(underTest.put(queryKey, null)).isNull();
  }

  @Test
  public void test_MultiTenantQueryCachePut_ClassAnnotatedWithDataCacheDisabled_ReturnsNull() {
    when(queryKey.getCandidateTypeName()).thenReturn(EntityWithCacheButDisabled.class.getName());

    assertThat(underTest.put(queryKey, null)).isNull();
  }

  @Test
  public void test_MultiTenantQueryCachePut_ClassAnnotatedWithDataCache_IsPutInToCache() {
    when(queryKey.getCandidateTypeName()).thenReturn(EntityWithCache.class.getName());

    assertThatThrownBy(() -> underTest.put(queryKey, null))
        .isInstanceOf(NotImplementedException.class)
        .hasMessageContaining("Not implemented for testing");
  }

  private static class TestMultiTenantQueryCache
      extends MultiTenantQueryCache
  {
    @Override
    protected CacheMap newCacheMap() {
      return new CacheMap();
    }

    @Override
    protected QueryResult putInternal(final QueryKey qk, final QueryResult result) {
      throw new NotImplementedException("Not implemented for testing");
    }
  }

  @DataCache
  private static class EntityWithCache
  {
  }

  @DataCache(enabled = false)
  private static class EntityWithCacheButDisabled
  {
  }

  private static class EntityWithoutCache
  {
  }
}
