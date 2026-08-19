/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import javax.sql.DataSource;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.assertTenantSet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MultiTenantQuartzConnectionProviderTest
    extends AbstractMultiTenantTest
{
  @Mock
  private OperationalDataStore operationalDataStore;

  @Mock
  private DataSource dataSource;

  @Test
  public void shouldUseGlobalTenantToGetConnection() throws Exception {
    when(operationalDataStore.getDataSource()).thenReturn(dataSource);
    when(dataSource.getConnection()).thenAnswer(invocationOnMock -> {
      assertTenantSet(GLOBAL_TENANT);
      return null;
    });

    MultiTenantQuartzConnectionProvider connectionProvider =
        new MultiTenantQuartzConnectionProvider(operationalDataStore);
    connectionProvider.getConnection();

    verify(dataSource).getConnection();
  }
}
