/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import javax.persistence.RollbackException;

import com.sonatype.insight.brain.dataaccess.ComponentCategoryDAO;
import com.sonatype.insight.brain.model.component.ComponentCategory;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.brain.service.AbstractMultiTenantBrainServiceTest;
import com.sonatype.insight.brain.tenancy.Tenant;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.createTenant;
import static org.junit.Assert.fail;

public class MultiTenantGlobalWriteProtectionTest
    extends AbstractMultiTenantBrainServiceTest
{
  private Tenant tenant1;

  @Before
  public void setUp() {
    tenant1 = createTenant("tenant1");
  }

  @Override
  public void setUpTestLicenseThreatGroups() {
    // no-op because the default creates LicenseThreatGroups under global which is now write protected
  }

  @Test
  public void testGlobalSchema_HasWriteProtectionEnabled() {
    Role tenant1Role = newRole(tenant1.tenantSlug + "-role", "description", false);
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
  }

  public Role newRole(String name, String description, boolean global) {
    Role role = new Role();
    role.setName(name);
    role.setDescription(description);
    role.setGlobal(global);
    return role;
  }
}
