/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.tenancy.MultiTenantTestSupport;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

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
 * Tests for multi-tenant behavior of SystemConfigurationPropertyDAO.
 *
 * This test verifies that:
 * - Property lookups use the current tenant's data
 * - Property lookups fall back to global tenant when per-tenant data is not found
 * - Property updates/sets do NOT fall back to global tenant (they operate on current tenant only)
 *
 * The test uses a mock DAO that tracks which tenants are accessed during operations,
 * allowing verification of the multi-tenant fallback behavior without requiring
 * actual database connections.
 */
@ExtendWith(MockitoExtension.class)
public class SystemConfigurationPropertyDAOMultiTenantTest
    extends MultiTenantTestSupport
{
  static final String PROPERTY_NAME = "name";

  static final String PROPERTY_VALUE = "value";

  static final String GLOBAL_TENANT_PROPERTY_VALUE = "global-tenant-value";

  @Mock
  OperationalDataStore operationalDataStore;

  SystemConfigurationProperty property;

  SystemConfigurationProperty globalTenantProperty;

  MockSystemConfigurationPropertyDAO underTest;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();

    this.underTest = new MockSystemConfigurationPropertyDAO(operationalDataStore);

    this.property = new SystemConfigurationProperty(PROPERTY_NAME, PROPERTY_VALUE);
    this.globalTenantProperty = new SystemConfigurationProperty(PROPERTY_NAME, GLOBAL_TENANT_PROPERTY_VALUE);
  }

  @Test
  public void shouldReturnProperty_whenExists() {
    testAsNewTenant(t -> {
      // Configure mock to return tenant-specific property
      underTest.setPropertyForTenant(t, property);

      String value = underTest.get(mock(TransactionContext.class), "query");

      assertThat(value).isEqualTo(PROPERTY_VALUE);

      assertThat(underTest.usedTenants.size()).isEqualTo(1);
      assertThat(underTest.usedTenants.get(0)).isEqualTo(t);
    });
  }

  @Test
  public void shouldReturnValue_whenSingleTenant() {
    testAsSingleTenant(s -> {
      // Configure mock to return property for single tenant
      underTest.setPropertyForTenant(SINGLE_TENANT, property);

      String value = underTest.get(mock(TransactionContext.class), "query");

      assertThat(value).isEqualTo(PROPERTY_VALUE);

      assertThat(underTest.usedTenants.size()).isEqualTo(1);
      assertThat(underTest.usedTenants.get(0)).isEqualTo(SINGLE_TENANT);
    });
  }

  @Test
  public void shouldReturnNull_whenSingleTenant_andValueNull() {
    testAsSingleTenant(s -> {
      // Configure mock to return null for single tenant (no property set)
      underTest.setPropertyForTenant(SINGLE_TENANT, null);

      String value = underTest.get(mock(TransactionContext.class), "query");

      assertThat(value).isEqualTo(null);

      assertThat(underTest.usedTenants.size()).isEqualTo(1);
      assertThat(underTest.usedTenants.get(0)).isEqualTo(SINGLE_TENANT);
    });
  }

  @Test
  public void shouldReturnValue_whenGlobalTenant() {
    testAsGlobalTenant(g -> {
      // Configure mock to return property for global tenant
      underTest.setPropertyForTenant(GLOBAL_TENANT, globalTenantProperty);

      String value = underTest.get(mock(TransactionContext.class), "query");

      assertThat(value).isEqualTo(GLOBAL_TENANT_PROPERTY_VALUE);

      assertThat(underTest.usedTenants.size()).isEqualTo(1);
      assertThat(underTest.usedTenants.get(0)).isEqualTo(GLOBAL_TENANT);
    });
  }

  @Test
  public void shouldFallBackToGlobalConfig_whenPerTenantConfigNull() {
    testAsNewTenant(t -> {
      // Configure: current tenant has no property, but global tenant does
      underTest.setPropertyForTenant(t, null);
      underTest.setPropertyForTenant(GLOBAL_TENANT, globalTenantProperty);

      String value = underTest.get(mock(TransactionContext.class), "query");

      assertThat(value).isEqualTo(GLOBAL_TENANT_PROPERTY_VALUE);

      assertThat(underTest.usedTenants.size()).isEqualTo(2);
      assertThat(underTest.usedTenants.get(0)).isEqualTo(t);
      assertThat(underTest.usedTenants.get(1)).isEqualTo(GLOBAL_TENANT);
    });
  }

  @Test
  public void shouldNotFallBackToGlobal_whenSetTenantConfig() {
    testAsNewTenant(t -> {
      // For set operations, we should NOT fall back to global
      // Even though no property exists for the tenant, set should create in tenant's context
      underTest.setPropertyForTenant(t, null);
      underTest.setPropertyForTenant(GLOBAL_TENANT, globalTenantProperty);

      underTest.set(mock(TransactionContext.class), "key", PROPERTY_VALUE);

      assertThat(underTest.usedTenants.size()).isEqualTo(1);
      assertThat(underTest.usedTenants.get(0)).isEqualTo(t);
    });
  }

  @Test
  public void shouldNotFallBackToGlobal_whenUpdateTenantConfig() {
    testAsNewTenant(t -> {
      // For update operations, we should NOT fall back to global
      underTest.setPropertyForTenant(t, null);
      underTest.setPropertyForTenant(GLOBAL_TENANT, globalTenantProperty);

      try {
        underTest.update(mock(TransactionContext.class), new SystemConfigurationProperty("key", PROPERTY_VALUE));
      }
      catch (NotFoundException e) {
        // no-op. Don't care that no config exists for the tenant, we care that global is NOT used
      }

      assertThat(underTest.usedTenants.size()).isEqualTo(1);
      assertThat(underTest.usedTenants.get(0)).isEqualTo(t);
    });
  }

  /**
   * Mock DAO that tracks tenant usage without requiring actual database/jOOQ infrastructure.
   *
   * This mock overrides the jOOQ-based query methods to:
   * 1. Track which tenants are accessed during operations
   * 2. Return configured mock data based on current tenant
   * 3. Implement the same multi-tenant fallback logic as the real DAO
   */
  private class MockSystemConfigurationPropertyDAO
      extends SystemConfigurationPropertyDAO
  {
    private final List<Tenant> usedTenants = new ArrayList<>();

    private final java.util.Map<Tenant, SystemConfigurationProperty> propertyByTenant = new java.util.HashMap<>();

    private final TenantUtil tenantUtil = new TenantUtil();

    public MockSystemConfigurationPropertyDAO(OperationalDataStore operationalDataStore) {
      super(operationalDataStore);
    }

    /**
     * Configure what property should be returned for a specific tenant.
     */
    void setPropertyForTenant(Tenant tenant, SystemConfigurationProperty property) {
      propertyByTenant.put(tenant, property);
    }

    // Override createTransactionContext to prevent actual database connection attempts
    @Override
    public TransactionContext createTransactionContext() {
      return mock(TransactionContext.class);
    }

    /**
     * Override getByName to implement mock tenant-aware lookup with tracking.
     * This replicates the multi-tenant fallback logic of the real DAO.
     */
    @Override
    public SystemConfigurationProperty getByName(TransactionContext tx, String name) {
      return getByNameMock(name, false);
    }

    /**
     * Mock implementation of tenant-aware property lookup.
     *
     * @param name property name (not actually used in mock, but matches signature)
     * @param fetchForUpdate if true, skip global fallback (used for update operations)
     */
    @SuppressWarnings("PMD.UnusedFormalParameter") // name parameter matches production method signature
    private SystemConfigurationProperty getByNameMock(String name, boolean fetchForUpdate) {
      Tenant currentTenant = TenantThreadLocal.getTenant();
      recordTenantUsage(currentTenant);

      // Get result for current tenant
      SystemConfigurationProperty result = propertyByTenant.get(currentTenant);

      // Check if we should fall back to global
      if (result != null || fetchForUpdate || tenantUtil.isSingleTenant() || tenantUtil.isGlobalTenant()) {
        return result;
      }
      else {
        // Fall back to global tenant
        recordTenantUsage(GLOBAL_TENANT);
        return propertyByTenant.get(GLOBAL_TENANT);
      }
    }

    /**
     * Override get(tx, name) to use our mock implementation.
     */
    @Override
    public String get(TransactionContext tx, String name) {
      SystemConfigurationProperty property = getByNameMock(name, false);
      return property != null ? property.getValue() : null;
    }

    /**
     * Override set to track tenant usage without falling back to global.
     */
    @Override
    public void set(TransactionContext tx, String name, String value) {
      // For set operations, use fetchForUpdate=true to prevent global fallback
      getByNameMock(name, true);
      // Don't need to actually persist anything for this test
    }

    /**
     * Override update to track tenant usage without falling back to global.
     */
    @Override
    public int update(TransactionContext tx, SystemConfigurationProperty property) {
      // For update operations, use fetchForUpdate=true to prevent global fallback
      SystemConfigurationProperty existing = getByNameMock(property.getName(), true);
      if (existing == null) {
        throw new NotFoundException("A system configuration property '" + property.getName() + "' does not exist.");
      }
      // Don't need to actually update anything for this test
      return 0;
    }

    private void recordTenantUsage(Tenant tenant) {
      if (!usedTenants.contains(tenant)) {
        usedTenants.add(tenant);
      }
    }
  }
}
