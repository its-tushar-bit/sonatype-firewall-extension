/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.dataaccess.tenancy.DeletedTenantDAO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.tenancy.TenantDeregistrationJob;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantProvisioningServiceTest
    extends AbstractMultiTenantTest
{
  @Mock
  private InsightConfig insightConfig;

  @Mock
  private DatabaseProvisionUtils databaseProvisionUtils;

  @Mock
  private TenantValidator tenantValidator;

  @Mock
  private TenantDeregistrationJob tenantDeregistrationJob;

  @Mock
  private DeletedTenantDAO deletedTenantDAO;

  @Mock
  private UserDAO userDAO;

  @Mock
  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private TenantUtil tenantUtil;

  private MultiTenantInsightConfig config;

  private TenantProvisioningService underTest;

  @Before
  public void setup() {
    tenantUtil = new TenantUtil();
    config = new MultiTenantInsightConfig();
    underTest = new TenantProvisioningService(insightConfig, databaseProvisionUtils, tenantUtil,
        tenantValidator, tenantDeregistrationJob, deletedTenantDAO, userDAO, systemConfigurationPropertyDAO, config);
  }

  @Test
  public void shouldProvisionNewTenant() {
    testAsNewTenant(tenant -> {
      User user = new User("ADMIN", "ADMIN", "ADMIN", "ADMIN",
          "admin@local.com");
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);
      when(userDAO.getById(any(String.class))).thenReturn(user);
      config.setDeleteBuiltInAdmin(false);

      underTest.provisionTenant(tenant.tenantSlug);

      verify(databaseProvisionUtils).initializeDatabasesWithMigration(any(InsightConfig.class));
      verify(userDAO).getById("ADMIN");
      verify(userDAO, never()).delete(user);
    });
  }

  @Test
  public void shouldProvisionNewTenant_and_deleteBuiltInAdmin() {
    testAsNewTenant(tenant -> {
      User user = new User("ADMIN", "ADMIN", "ADMIN", "ADMIN",
          "admin@local.com");
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);
      when(userDAO.getById(any(String.class))).thenReturn(user);

      underTest.provisionTenant(tenant.tenantSlug);

      verify(databaseProvisionUtils).initializeDatabasesWithMigration(any(InsightConfig.class));
      verify(userDAO).getById("ADMIN");
      verify(userDAO).delete(user);
    });
  }

  @Test
  public void shouldProvisionNewTenant_and_disableAdvancedSearch() {
    testAsNewTenant(tenant -> {
      User user = new User("ADMIN", "ADMIN", "ADMIN", "ADMIN",
          "admin@local.com");
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);
      when(userDAO.getById(any(String.class))).thenReturn(user);

      underTest.provisionTenant(tenant.tenantSlug);

      verify(databaseProvisionUtils).initializeDatabasesWithMigration(any(InsightConfig.class));
      verify(systemConfigurationPropertyDAO).set("ADVANCED_SEARCH_ENABLED", "false");
    });
  }

  @Test
  public void shouldThrowRuntimeException_whenTenantAlreadyExists() {
    final String errorMessage = "Tenant already exists";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      assertThatThrownBy(() -> underTest.provisionTenant(tenant.tenantSlug))
          .withFailMessage(errorMessage)
          .isInstanceOf(ConflictException.class);
    });
  }

  @Test
  public void shouldThrowRuntimeException_whenUsingGlobalTenant() {
    final String errorMessage = "Invalid tenant";

    testAsGlobalTenant(tenant -> {
      assertThatThrownBy(() -> underTest.provisionTenant(tenant.tenantSlug))
          .withFailMessage(errorMessage)
          .isInstanceOf(BadRequestException.class);
    });
  }
}
