/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import javax.persistence.PersistenceException;

import com.sonatype.insight.brain.dataaccess.ComponentCategoryDAO;
import com.sonatype.insight.brain.db.MultiTenantGlobalSchemaProtection;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.component.ComponentCategory;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.brain.tenancy.MultiTenantDatabaseTestSupport;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantGlobalWriteProtectionTest
    extends MultiTenantDatabaseTestSupport
{
  private MultiTenantGlobalSchemaProtection multiTenantGlobalSchemaProtection;

  @Override
  @Before
  public void setup() {
    super.setup();
    OperationalDataStore operationalDataStore = multiTenantDatabaseTestRule.operationalDataStore;
    multiTenantGlobalSchemaProtection =
        new MultiTenantGlobalSchemaProtection(operationalDataStore);
  }

  @Test
  public void testGlobalSchema_HasWriteProtectionCreated() {
    multiTenantGlobalSchemaProtection.createWriteProtection();

    testAsGlobalTenant(global -> {
      Role globalRole = newRole(global.tenantSlug + "-role", "description", false);
      RolePermission globalRolePermission = new RolePermission(globalRole.getId(), Permission.ADD_APPLICATION);

      ComponentCategory componentCategory = new ComponentCategory("id", "test");

      // Global tenant can write to component_category table
      new ComponentCategoryDAO().insert(componentCategory);

      // Global tenant is prevented from writing to role and role_permission tables
      try {
        new RoleDAO().insert(globalRole);
        new RolePermissionDAO().insert(globalRolePermission);
      }
      catch (PersistenceException e) {
        Throwable rootCause = ExceptionUtils.getRootCause(e);
        assertThat(rootCause.getMessage()).contains("ERROR: global write protection");
      }
    });
  }

  @Test
  public void testGlobalSchema_HasWriteProtectionDisabled() {
    multiTenantGlobalSchemaProtection.createWriteProtection(); //Global schema should be protected first
    multiTenantGlobalSchemaProtection.disableWriteProtection();

    testAsGlobalTenant(global -> {
      Role globalRole = newRole(global.tenantSlug + "-role", "description", false);
      RolePermission globalRolePermission = new RolePermission(globalRole.getId(), Permission.ADD_APPLICATION);

      // role and role_permission tables do not have the write_protection and insertions can be executed
      new RoleDAO().insert(globalRole);
      new RolePermissionDAO().insert(globalRolePermission);
    });
  }

  @Test
  public void testGlobalSchema_HasWriteProtectionReEnabled() {
    multiTenantGlobalSchemaProtection.createWriteProtection(); //Global schema should be protected first
    multiTenantGlobalSchemaProtection.disableWriteProtection();

    testAsGlobalTenant(global -> {
      Role globalRole1 = newRole(global.tenantSlug + "-role1", "description1", false);
      RolePermission globalRolePermission1 = new RolePermission(globalRole1.getId(), Permission.ADD_APPLICATION);

      // role and role_permission tables do not have the write_protection and insertions can be executed
      new RoleDAO().insert(globalRole1);
      new RolePermissionDAO().insert(globalRolePermission1);

      multiTenantGlobalSchemaProtection.enableWriteProtection();

      Role globalRole2 = newRole(global.tenantSlug + "-role2", "description2", false);
      RolePermission globalRolePermission2 = new RolePermission(globalRole2.getId(), Permission.EVALUATE_APPLICATION);

      // Global tenant is again prevented from writing to role and role_permission tables
      try {
        new RoleDAO().insert(globalRole2);
        new RolePermissionDAO().insert(globalRolePermission2);
      }
      catch (PersistenceException e) {
        Throwable rootCause = ExceptionUtils.getRootCause(e);
        assertThat(rootCause.getMessage()).contains("ERROR: global write protection");
      }
    });
  }

  public Role newRole(String name, String description, boolean global) {
    Role role = new Role();
    role.setName(name);
    role.setDescription(description);
    role.setGlobal(global);
    role.setId("role-id");
    return role;
  }
}
