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
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.Tenant.SINGLE_TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests multi-tenant fallback behavior for MailConfigurationDAO.
 *
 * <p>
 * This test verifies that:
 * <ul>
 * <li>In multi-tenant mode, tenant-specific config is returned when it exists</li>
 * <li>In multi-tenant mode, global config is returned as fallback when tenant config is null</li>
 * <li>In single-tenant mode, configuration is returned without fallback logic</li>
 * <li>When running as global tenant, global config is returned directly</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
public class MailConfigurationDAOMultiTenantTest
    extends MultiTenantTestSupport
{
  @Mock
  OperationalDataStore operationalDataStore;

  MockMailConfigurationDAO underTest;

  MailConfiguration tenantMailConfiguration;

  MailConfiguration globalMailConfiguration;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();

    this.underTest = new MockMailConfigurationDAO(operationalDataStore);

    this.tenantMailConfiguration = new MailConfiguration();
    tenantMailConfiguration.setId("tenant");
    this.globalMailConfiguration = new MailConfiguration();
    globalMailConfiguration.setId("global");
  }

  @Test
  public void shouldReturnTenantConfiguration_whenExists() {
    testAsNewTenant(t -> {
      underTest.setTenantConfig(tenantMailConfiguration);
      underTest.setGlobalConfig(globalMailConfiguration);

      MailConfiguration value = underTest.get();

      assertThat(value.getId()).isEqualTo("tenant");

      assertThat(underTest.usedTenants.size()).isEqualTo(1);
      assertThat(underTest.usedTenants).containsOnly(t);
    });
  }

  @Test
  public void shouldReturnValue_whenSingleTenant() {
    testAsSingleTenant(s -> {
      underTest.setTenantConfig(tenantMailConfiguration);
      underTest.setGlobalConfig(globalMailConfiguration);

      MailConfiguration value = underTest.get();

      assertThat(value.getId()).isEqualTo("tenant");

      assertThat(underTest.usedTenants.size()).isEqualTo(1);
      assertThat(underTest.usedTenants).containsOnly(SINGLE_TENANT);
    });
  }

  @Test
  public void shouldReturnValue_whenGlobalTenant() {
    testAsGlobalTenant(g -> {
      underTest.setTenantConfig(globalMailConfiguration);
      underTest.setGlobalConfig(globalMailConfiguration);

      MailConfiguration value = underTest.get();

      assertThat(value.getId()).isEqualTo("global");

      assertThat(underTest.usedTenants.size()).isEqualTo(1);
      assertThat(underTest.usedTenants).containsOnly(GLOBAL_TENANT);
    });
  }

  @Test
  public void shouldFallBackToGlobalConfig_whenPerTenantConfigNull() {
    testAsNewTenant(t -> {
      underTest.setTenantConfig(null);
      underTest.setGlobalConfig(globalMailConfiguration);

      MailConfiguration value = underTest.get();

      assertThat(value.getId()).isEqualTo("global");

      assertThat(underTest.usedTenants.size()).isEqualTo(2);
      assertThat(underTest.usedTenants).containsExactlyInAnyOrder(t, GLOBAL_TENANT);
    });
  }

  @Test
  public void shouldNotFallBackToGlobal_whenUsingGetWithoutFallback() {
    testAsNewTenant(t -> {
      underTest.setTenantConfig(null);
      underTest.setGlobalConfig(globalMailConfiguration);

      MailConfiguration value = underTest.getWithoutFallback();

      assertThat(value).isNull();

      // Should only query the tenant, not fall back to global
      assertThat(underTest.usedTenants.size()).isEqualTo(1);
      assertThat(underTest.usedTenants).containsOnly(t);
    });
  }

  /**
   * Mock implementation that intercepts jOOQ calls and returns pre-configured values
   * based on the current tenant context.
   */
  private class MockMailConfigurationDAO
      extends MailConfigurationDAO
  {
    private MailConfiguration tenantConfig;

    private MailConfiguration globalConfig;

    MockMailConfigurationDAO(OperationalDataStore operationalDataStore) {
      super(operationalDataStore);
    }

    private final HashSet<Tenant> usedTenants = new HashSet<>();

    void setTenantConfig(MailConfiguration config) {
      this.tenantConfig = config;
    }

    void setGlobalConfig(MailConfiguration config) {
      this.globalConfig = config;
    }

    @Override
    public TransactionContext createTransactionContext() {
      return mock(TransactionContext.class);
    }

    /**
     * Override fetchMailConfiguration to track tenant usage and return appropriate config.
     * This method is called by getByIdJooq for both the primary and fallback queries.
     */
    @Override
    protected MailConfiguration fetchMailConfiguration(TransactionContext tx) {
      Tenant currentTenant = TenantThreadLocal.getTenant();
      usedTenants.add(currentTenant);

      TenantUtil tenantUtil = new TenantUtil();
      if (tenantUtil.isGlobalTenant()) {
        return globalConfig;
      }
      else {
        return tenantConfig;
      }
    }
  }
}
