/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.HashSet;

import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.tenancy.MultiTenantTestSupport;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.dataaccess.AbstractDAO.Query;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.Tenant.SINGLE_TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MailConfigurationDAOMultiTenantTest
    extends MultiTenantTestSupport
{
  @Mock
  Query query;

  @Mock
  OperationalDataStore operationalDataStore;

  MockMailConfigurationDAO underTest;

  MailConfiguration mailConfiguration;

  MailConfiguration globalMailConfiguration;

  @Before
  @Override
  public void setup() {
    super.setup();

    this.underTest = new MockMailConfigurationDAO(operationalDataStore);

    this.mailConfiguration = new MailConfiguration();
    mailConfiguration.setId("tenant");
    this.globalMailConfiguration = new MailConfiguration();
    globalMailConfiguration.setId("global");

    when(query.setLockModeType(any())).thenReturn(query);

    when(query.get(any())).thenAnswer(invocationOnMock -> {
      if (new TenantUtil().isGlobalTenant()) {
        return globalMailConfiguration;
      }
      else {
        return mailConfiguration;
      }
    });
  }

  @Test
  public void shouldReturnTenantConfiguration_whenExists() {
    testAsNewTenant(t -> {
      MailConfiguration value = underTest.get(mock(TransactionContext.class), "query");

      assertThat(value.getId()).isEqualTo("tenant");

      assertThat(underTest.usedTenants.size()).isEqualTo(1);
      assertThat(underTest.usedTenants).containsOnly(t);
    });
  }

  @Test
  public void shouldReturnValue_whenSingleTenant() {
    testAsSingleTenant(s -> {
      MailConfiguration value = underTest.get(mock(TransactionContext.class), "query");

      assertThat(value.getId()).isEqualTo("tenant");

      assertThat(underTest.usedTenants.size()).isEqualTo(1);
      assertThat(underTest.usedTenants).containsOnly(SINGLE_TENANT);
    });
  }

  @Test
  public void shouldReturnValue_whenGlobalTenant() {
    testAsGlobalTenant(g -> {
      MailConfiguration value = underTest.get(mock(TransactionContext.class), "query");

      assertThat(value.getId()).isEqualTo("global");

      assertThat(underTest.usedTenants.size()).isEqualTo(1);
      assertThat(underTest.usedTenants).containsOnly(GLOBAL_TENANT);
    });
  }

  @Test
  public void shouldFallBackToGlobalConfig_whenPerTenantConfigNull() {
    testAsNewTenant(t -> {
      when(query.get(any())).thenReturn(null).thenReturn(globalMailConfiguration);

      MailConfiguration value = underTest.get(mock(TransactionContext.class), "query");

      assertThat(value.getId()).isEqualTo("global");

      assertThat(underTest.usedTenants.size()).isEqualTo(2);
      assertThat(underTest.usedTenants).containsExactlyInAnyOrder(t, GLOBAL_TENANT);
    });
  }

  private class MockMailConfigurationDAO
      extends MailConfigurationDAO
  {
    MockMailConfigurationDAO(OperationalDataStore operationalDataStore) {
      super(operationalDataStore);
    }

    private final HashSet<Tenant> usedTenants = new HashSet<>();

    // This method is overridden to prevent having to mock all the transaction internals
    @Override
    public TransactionContext createTransactionContext() {
      return mock(TransactionContext.class);
    }

    // This method is overridden to prevent having to mock all the transaction internals
    @Override
    public Query<MailConfiguration> createQuery(
        String sQuery,
        Object... parameters)
    {
      if (!usedTenants.contains(TenantThreadLocal.getTenant())) {
        this.usedTenants.add(TenantThreadLocal.getTenant());
      }

      return query;
    }
  }
}
