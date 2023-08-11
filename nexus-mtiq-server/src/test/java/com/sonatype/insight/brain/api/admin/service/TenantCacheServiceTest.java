/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.tenancy.MultiTenantTestSupport;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.openjpa.datacache.CacheStatistics;
import org.apache.openjpa.datacache.CacheStatisticsImpl;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.StoreCache;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantCacheServiceTest
    extends MultiTenantTestSupport
{
  @Mock
  TenantUtil tenantUtil;

  @Mock
  TenantValidator tenantValidator;

  @Mock
  OperationalDataStore operationalDataStore;

  @Mock
  OpenJPAEntityManagerFactory openJpaEntityManagerFactory;

  @Mock
  StoreCache storeCache;

  TenantCacheService underTest;

  @Before
  @Override
  public void setup() {
    underTest = new TenantCacheService(tenantUtil, tenantValidator);

    when(tenantValidator.validateTenantExists(anyString())).thenReturn(true);

    OperationalDataStoreProvider.setInstance(operationalDataStore);

    when(operationalDataStore.getJPAEntityManagerFactory()).thenReturn(openJpaEntityManagerFactory);
    when(openJpaEntityManagerFactory.getStoreCache()).thenReturn(storeCache);

    when(storeCache.getStatistics()).thenReturn(new CacheStatisticsImpl());
  }

  @After
  public void tearDown() {
    OperationalDataStoreProvider.setInstance(null);
  }

  @Test
  public void testGetsStatisticsFromStoreCache() {
    testAsNewTenant(t -> {
      String result = underTest.getCache(t.tenantSlug);

      JSONAssert.assertEquals(JsonUtils.format(new CacheStatisticsImpl()), result, true);
    });
  }

  @Test
  public void testThrowsExceptionWhenGlobalTenant() {
    when(tenantUtil.isGlobalTenant()).thenReturn(true);

    assertThatThrownBy(() -> underTest.getCache(GLOBAL_TENANT.tenantSlug))
        .hasMessage("Invalid tenant")
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void testThrowsExceptionWhenTenantDoesNotExist() {
    when(tenantValidator.validateTenantExists(anyString())).thenReturn(false);

    assertThatThrownBy(() -> underTest.getCache(GLOBAL_TENANT.tenantSlug))
        .hasMessage("Tenant doesn't exist")
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void testThrowsExceptionWhenJsonParseFails() {
    // A mock is not serializable so will trigger a JsonProcessingException
    when(storeCache.getStatistics()).thenReturn(mock(CacheStatistics.class));

    testAsNewTenant(t -> assertThatThrownBy(() -> underTest.getCache(t.tenantSlug))
        .hasMessage("Unable to parse CacheStatistics")
        .isInstanceOf(RuntimeException.class));
  }
}
