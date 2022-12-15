/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.tenancy.MultiTenantTest;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.dataaccess.AbstractDAO.Query;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.Tenant.SINGLE_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.setTenant;
import static com.sonatype.insight.brain.tenancy.TenantUtil.isGlobalTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SystemConfigurationPropertyDAOMultiTenantTest
    extends MultiTenantTest
{
  static final String PROPERTY_NAME = "name";

  static final String PROPERTY_VALUE = "value";

  static final String GLOBAL_TENANT_PROPERTY_VALUE = "global-tenant-value";

  static final Tenant TENANT = new Tenant("individual-tenant");

  @Mock
  Query query;

  SystemConfigurationProperty property;

  SystemConfigurationProperty globalTenantProperty;

  MockSystemConfigurationPropertyDAO underTest;

  @Before
  @Override
  public void setup() {
    super.setup();

    this.underTest = new MockSystemConfigurationPropertyDAO();

    this.property = new SystemConfigurationProperty(PROPERTY_NAME, PROPERTY_VALUE);
    this.globalTenantProperty = new SystemConfigurationProperty(PROPERTY_NAME, GLOBAL_TENANT_PROPERTY_VALUE);

    when(query.setLockModeType(any())).thenReturn(query);

    when(query.get(any())).thenAnswer(invocationOnMock -> {
      if (isGlobalTenant()) {
        return globalTenantProperty;
      }
      else {
        return property;
      }
    });
  }

  @Test
  public void shouldReturnProperty_whenExists() {
    setTenant(TENANT);

    String value = underTest.get(mock(TransactionContext.class), "query");

    assertThat(value).isEqualTo(PROPERTY_VALUE);

    assertThat(underTest.usedTenants.size()).isEqualTo(1);
    assertThat(underTest.usedTenants.get(0)).isEqualTo(TENANT);
  }

  @Test
  public void shouldReturnValue_whenSingleTenant() {
    setTenant(SINGLE_TENANT);

    String value = underTest.get(mock(TransactionContext.class), "query");

    assertThat(value).isEqualTo(PROPERTY_VALUE);

    assertThat(underTest.usedTenants.size()).isEqualTo(1);
    assertThat(underTest.usedTenants.get(0)).isEqualTo(SINGLE_TENANT);
  }

  @Test
  public void shouldReturnNull_whenSingleTenant_andValueNull() {
    setTenant(SINGLE_TENANT);
    when(query.get(any())).thenReturn(null);

    String value = underTest.get(mock(TransactionContext.class), "query");

    assertThat(value).isEqualTo(null);

    assertThat(underTest.usedTenants.size()).isEqualTo(1);
    assertThat(underTest.usedTenants.get(0)).isEqualTo(SINGLE_TENANT);
  }

  @Test
  public void shouldReturnValue_whenGlobalTenant() {
    setTenant(GLOBAL_TENANT);

    String value = underTest.get(mock(TransactionContext.class), "query");

    assertThat(value).isEqualTo(GLOBAL_TENANT_PROPERTY_VALUE);

    assertThat(underTest.usedTenants.size()).isEqualTo(1);
    assertThat(underTest.usedTenants.get(0)).isEqualTo(GLOBAL_TENANT);
  }

  @Test
  public void shouldFallBackToGlobalConfig_whenPerTenantConfigNull() {
    setTenant(TENANT);
    when(query.get(any())).thenReturn(null).thenReturn(globalTenantProperty);

    String value = underTest.get(mock(TransactionContext.class), "query");

    assertThat(value).isEqualTo(GLOBAL_TENANT_PROPERTY_VALUE);

    assertThat(underTest.usedTenants.size()).isEqualTo(2);
    assertThat(underTest.usedTenants.get(0)).isEqualTo(TENANT);
    assertThat(underTest.usedTenants.get(1)).isEqualTo(GLOBAL_TENANT);
  }

  private class MockSystemConfigurationPropertyDAO
      extends SystemConfigurationPropertyDAO
  {
    private List<Tenant> usedTenants = new ArrayList<>();

    // This method is overridden to prevent having to mock all the transaction internals
    @Override
    public Query<SystemConfigurationProperty> createQuery(
        String sQuery,
        Object... parameters)
    {
      this.usedTenants.add(TenantThreadLocal.getTenant());

      return query;
    }
  }
}
