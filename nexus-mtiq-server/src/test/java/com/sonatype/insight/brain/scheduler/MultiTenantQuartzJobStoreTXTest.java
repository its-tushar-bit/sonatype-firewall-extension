/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobPersistenceException;
import org.quartz.impl.jdbcjobstore.InvalidConfigurationException;
import org.quartz.utils.ConnectionProvider;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@RunWith(MockitoJUnitRunner.class)
public class MultiTenantQuartzJobStoreTXTest
    extends AbstractMultiTenantTest
{
  @Mock
  private ProductLicense productLicense;

  @Mock
  private InsightConfig insightConfig;

  @Mock
  private OperationalDataStore operationalDataStore;

  @Mock
  private ClusterLockManager clusterLockManager;

  private TestMultiTenantQuartzJobStoreTX underTest;

  @Before
  public void setup() {
    try {
      underTest = new TestMultiTenantQuartzJobStoreTX(productLicense,
          insightConfig,
          operationalDataStore,
          clusterLockManager);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void shouldUseMultiTenantConnectionProvider() {
    ConnectionProvider connectionProvider = underTest.buildQuartzConnectionProvider();

    assertThat(connectionProvider).isExactlyInstanceOf(MultiTenantQuartzConnectionProvider.class);
  }

  @Test
  public void shouldUseGlobalTenant_whenDoCheckIn() throws Exception {
    testAsNewTenant(t -> {
      underTest.doCheckin();

      assertThat(underTest.lastUsedTenant).isEqualTo(GLOBAL_TENANT);
    });
  }

  @Test
  public void schemaMigrationShouldNotExitOnCheckIn() {
    // pretend that a schema migration is in progress
    // Note `lenient` is used as the code is never actually hit due to the override
    lenient().when(clusterLockManager.lockExists(ClusterLockManager.getLockIdForSchemaMigrationInProgress()))
        .thenReturn(true);

    // the MTIQ implementation still always returns false
    assertThat(underTest.shouldExitDueToSchemaMigration()).isFalse();
  }

  private static class TestMultiTenantQuartzJobStoreTX
      extends MultiTenantQuartzJobStoreTX
  {
    Tenant lastUsedTenant;

    public TestMultiTenantQuartzJobStoreTX(
        ProductLicense productLicense,
        InsightConfig insightConfig,
        OperationalDataStore operationalDataStore,
        ClusterLockManager clusterLockManager) throws InvalidConfigurationException
    {
      super(productLicense, insightConfig, operationalDataStore, new TenantUtil(), clusterLockManager);
    }

    @Override
    void initialize() throws InvalidConfigurationException {
      // no-op to prevent calling of static methods. Parent code is tested separately.
    }

    @Override
    protected boolean doSuperCheckIn() throws JobPersistenceException {
      lastUsedTenant = TenantThreadLocal.getTenant();

      return false;
    }
  }
}
