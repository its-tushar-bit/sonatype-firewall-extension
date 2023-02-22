/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.brain.tenancy.MultiTenantDatabaseTestSupport;

import org.junit.Test;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAs;
import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantRolePermissionDAOTest
    extends MultiTenantDatabaseTestSupport
{
  private final RolePermissionDAO permDAO = new RolePermissionDAO();

  @Test
  public void testRolePermissionDao_doesNotLeakDataBetweenTenants() {
    testAsNewTenant(t1 -> {
      Role tenant1Role = newRole(t1.tenantSlug, "description", false);
      RolePermission tenant1RolePermission = new RolePermission(tenant1Role.getId(), Permission.ADD_APPLICATION);
      permDAO.insert(tenant1RolePermission);

      assertThat(permDAO.getRoleIdsByPermission(tenant1RolePermission.getPermission()))
          .contains(tenant1RolePermission.getRoleId());

      testAsNewTenant(t2 -> {

        Role tenant2Role = newRole(t2.tenantSlug, "description", false);
        RolePermission tenant2RolePermission = new RolePermission(tenant2Role.getId(), Permission.ADD_APPLICATION);
        permDAO.insert(tenant2RolePermission);

        assertThat(permDAO.getRoleIdsByPermission(tenant2RolePermission.getPermission()))
            .contains(tenant2RolePermission.getRoleId())
            .doesNotContain(tenant1RolePermission.getRoleId());

        testAs(t1, t1Again -> {
          assertThat(permDAO.getRoleIdsByPermission(tenant1RolePermission.getPermission()))
              .contains(tenant1RolePermission.getRoleId())
              .doesNotContain(tenant2RolePermission.getRoleId());
        });
      });
    });
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
