/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.Collection;
import java.util.Collections;
import javax.inject.Provider;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.brain.service.TenantLifecycle;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantManager;
import com.sonatype.insight.brain.test.MultiTenantDatabaseTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import static com.sonatype.insight.brain.tenancy.TenantManagerTestHelper.setTestTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.createTenant;
import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantRolePermissionDAOTest
{
  private final RolePermissionDAO permDAO = new RolePermissionDAO();

  @Rule
  public MultiTenantDatabaseTestRule multiTenantDatabaseTestRule = new MultiTenantDatabaseTestRule();

  @Test
  public void testRolePermissionDao_doesNotLeakDataBetweenTenants() {
    Collection<TenantManaged> tenantManagedBeans = Collections.emptyList();
    Provider<TenantLifecycle> tenantLifecycleProvider = () -> Mockito.mock(TenantLifecycle.class);

    TenantManager tenantManager =
        new TenantManager(tenantManagedBeans, multiTenantDatabaseTestRule.insightConfig, tenantLifecycleProvider,
            multiTenantDatabaseTestRule.databaseProvisionUtils, multiTenantDatabaseTestRule.databaseConfigProvider);

    Tenant tenant1 = createTenant("tenant1");
    Tenant tenant2 = createTenant("tenant2");

    setTestTenant(tenantManager, tenant1);

    Role tenant1Role = newRole(tenant1.tenantSlug + "-role", "description", false);
    RolePermission tenant1RolePermission = new RolePermission(tenant1Role.getId(), Permission.ADD_APPLICATION);
    permDAO.insert(tenant1RolePermission);

    assertThat(permDAO.getRoleIdsByPermission(tenant1RolePermission.getPermission()))
        .contains(tenant1RolePermission.getRoleId());

    setTestTenant(tenantManager, tenant2);

    Role tenant2Role = newRole(tenant2.tenantSlug + "-role", "description", false);
    RolePermission tenant2RolePermission = new RolePermission(tenant2Role.getId(), Permission.ADD_APPLICATION);
    permDAO.insert(tenant2RolePermission);

    assertThat(permDAO.getRoleIdsByPermission(tenant2RolePermission.getPermission()))
        .contains(tenant2RolePermission.getRoleId())
        .doesNotContain(tenant1RolePermission.getRoleId());

    setTestTenant(tenantManager, tenant1);

    assertThat(permDAO.getRoleIdsByPermission(tenant1RolePermission.getPermission()))
        .contains(tenant1RolePermission.getRoleId())
        .doesNotContain(tenant2RolePermission.getRoleId());
  }

  public Role newRole(String name, String description, boolean global) {
    Role role = new Role();
    role.setName(name);
    role.setDescription(description);
    role.setGlobal(global);

    new RoleDAO().insert(role);

    return role;
  }
}
