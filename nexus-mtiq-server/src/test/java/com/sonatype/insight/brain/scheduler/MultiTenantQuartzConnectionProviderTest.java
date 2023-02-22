/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import javax.sql.DataSource;

import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.tenancy.MultiTenantTestSupport;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.assertTenantSet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MultiTenantQuartzConnectionProviderTest
    extends MultiTenantTestSupport
{
  @Mock
  OperationalDataStore operationalDataStore;

  @Mock
  DataSource dataSource;

  @Test
  public void shouldUseGlobalTenantToGetConnection() throws Exception {
    OperationalDataStore previousInstance = OperationalDataStoreProvider.getInstance();

    try {
      OperationalDataStoreProvider.setInstance(operationalDataStore);
      when(operationalDataStore.getDataSource()).thenReturn(dataSource);
      when(dataSource.getConnection()).thenAnswer(invocationOnMock -> {
        assertTenantSet(GLOBAL_TENANT);
        return null;
      });

      MultiTenantQuartzConnectionProvider connectionProvider = new MultiTenantQuartzConnectionProvider();
      connectionProvider.getConnection();

      verify(dataSource).getConnection();
    }
    finally {
      OperationalDataStoreProvider.setInstance(previousInstance);
    }
  }
}
