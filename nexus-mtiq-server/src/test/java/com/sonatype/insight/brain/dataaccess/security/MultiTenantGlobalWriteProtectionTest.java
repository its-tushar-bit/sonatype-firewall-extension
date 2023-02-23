/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import javax.persistence.RollbackException;

import com.sonatype.insight.brain.dataaccess.ComponentCategoryDAO;
import com.sonatype.insight.brain.db.MultiTenantGlobalSchemaProtection;
import com.sonatype.insight.brain.model.component.ComponentCategory;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.brain.tenancy.MultiTenantDatabaseTestSupport;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

public class MultiTenantGlobalWriteProtectionTest
    extends MultiTenantDatabaseTestSupport
{
  @Override
  @Before
  public void setUp() {
    super.setUp();
    MultiTenantGlobalSchemaProtection multiTenantGlobalSchemaProtection = new MultiTenantGlobalSchemaProtection();
    multiTenantGlobalSchemaProtection.enableWriteProtection();
  }

  @Test
  public void testGlobalSchema_HasWriteProtectionEnabled() {
    testAsNewTenant(tenant -> {
      Role tenant1Role = newRole(tenant.tenantSlug + "-role", "description", false);
      RolePermission tenant1RolePermission = new RolePermission(tenant1Role.getId(), Permission.ADD_APPLICATION);

      ComponentCategory componentCategory = new ComponentCategory("id", "test");

      // Global tenant can write to component_category table
      new ComponentCategoryDAO().insert(componentCategory);

      // Global tenant is prevented from writing to role_permission table
      try {
        new RolePermissionDAO().insert(tenant1RolePermission);
        fail();
      }
      catch (RollbackException e) {
        // no-op
      }
    });
  }

  public Role newRole(String name, String description, boolean global) {
    Role role = new Role();
    role.setName(name);
    role.setDescription(description);
    role.setGlobal(global);
    return role;
  }
}
