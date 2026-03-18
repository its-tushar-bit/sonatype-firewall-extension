/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.dataaccess.tenancy.DeletedTenantDAO;
import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tenancy.DeletedTenant;
import com.sonatype.insight.brain.search.SearchConfig.HttpOpenSearchConfig;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.tenancy.TenantDeregistrationJob;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;
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
  private DatabaseProvisioner databaseProvisioner;

  @Mock
  private TenantValidator tenantValidator;

  @Mock
  private TenantDeregistrationJob tenantDeregistrationJob;

  @Mock
  private DeletedTenantDAO deletedTenantDAO;

  @Mock
  private UserDAO userDAO;

  @Mock
  private IndexService indexService;

  private TenantUtil tenantUtil;

  private MultiTenantInsightConfig config;

  private TenantProvisioningService underTest;

  @Before
  public void setup() {
    tenantUtil = new TenantUtil();
    config = new MultiTenantInsightConfig();
    underTest = new TenantProvisioningService(databaseProvisioner, tenantUtil,
        tenantValidator, tenantDeregistrationJob, deletedTenantDAO, userDAO, indexService, config);
  }

  @Test
  public void testProvisionTenant_shouldProvisionNewTenant() {
    testAsNewTenant(tenant -> {
      User user = new User("ADMIN", "ADMIN", "ADMIN", "ADMIN",
          "admin@local.com");
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);
      when(userDAO.getById(any(String.class))).thenReturn(user);
      config.setDeleteBuiltInAdmin(false);
      config.setSearchConfig(new HttpOpenSearchConfig());

      underTest.provisionTenant(tenant.tenantSlug);

      verify(databaseProvisioner).initializeDatabaseWithMigration();
      verify(userDAO).getById("ADMIN");
      verify(userDAO, never()).delete(user);
      verify(indexService).createSearchIndex();
    });
  }

  @Test
  public void testProvisionTenant_shouldProvisionNewTenant_and_deleteBuiltInAdmin() {
    testAsNewTenant(tenant -> {
      User user = new User("ADMIN", "ADMIN", "ADMIN", "ADMIN",
          "admin@local.com");
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);
      when(userDAO.getById(any(String.class))).thenReturn(user);
      config.setSearchConfig(new HttpOpenSearchConfig());

      underTest.provisionTenant(tenant.tenantSlug);

      verify(databaseProvisioner).initializeDatabaseWithMigration();
      verify(userDAO).getById("ADMIN");
      verify(userDAO).delete(user);
      verify(indexService).createSearchIndex();
    });
  }

  @Test
  public void testProvisionTenant_shouldThrowRuntimeException_whenTenantAlreadyExists() {
    final String errorMessage = "Tenant already exists";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      assertThatThrownBy(() -> underTest.provisionTenant(tenant.tenantSlug))
          .withFailMessage(errorMessage)
          .isInstanceOf(ConflictException.class);
    });
  }

  @Test
  public void testProvisionTenant_shouldThrowRuntimeException_whenUsingGlobalTenant() {
    final String errorMessage = "Invalid tenant";

    testAsGlobalTenant(tenant -> {
      assertThatThrownBy(() -> underTest.provisionTenant(tenant.tenantSlug))
          .withFailMessage(errorMessage)
          .isInstanceOf(BadRequestException.class);
    });
  }

  @Test
  public void testProvisionTenant_shouldRemoveTenantMarkedForDeletion_whenProvisioningAgain() {
    testAsNewTenant(tenant -> {
      DeletedTenant deletedTenant = new DeletedTenant(tenant.tenantSlug);
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);
      // Tenant being provisioned was marked for deletion
      when(deletedTenantDAO.getTenantBySlug(tenant.tenantSlug)).thenReturn(deletedTenant);
      config.setSearchConfig(new HttpOpenSearchConfig());

      underTest.provisionTenant(tenant.tenantSlug);

      verify(deletedTenantDAO).delete(deletedTenant);
      // The provisioning process continues
      verify(databaseProvisioner).initializeDatabaseWithMigration();
      verify(indexService).createSearchIndex();
    });
  }
}
