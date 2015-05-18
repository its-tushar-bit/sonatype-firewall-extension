/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.EnumSet;
import java.util.Set;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.PermissionCategory;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Inject;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

public class RolePermissionServiceTest
    extends AbstractComponentTest
{
  @Inject
  private RolePermissionService rolePermissionService;

  private void assertListedPermissions(PermissionCategoryDTO actual, Permission... permissions) {
    assertThat(actual.permissions, hasSize(permissions.length));
    for (int i = 0; i < permissions.length; i++) {
      assertThat(actual.permissions.get(i).id, is(permissions[i]));
      assertThat(actual.permissions.get(i).displayName, is(permissions[i].getDisplayName()));
      assertThat(actual.permissions.get(i).description, is(permissions[i].getDescription()));
    }
  }

  private void assertAllowdPermissions(RolePermissionDTO actual, Permission... permissions) {
    Set<Permission> allowed = EnumSet.noneOf(Permission.class);
    for (PermissionCategoryDTO category : actual.permissionCategories) {
      for (PermissionDTO permission : category.permissions) {
        if (permission.allowed) {
          allowed.add(permission.id);
        }
      }
    }
    assertThat(allowed, containsInAnyOrder(permissions));
  }

  @Test
  public void testGetPermissionsForRole_BuiltInRole() {
    RolePermissionDTO rolePermissions = rolePermissionService.getPermissionsForRole(Role.SYSTEM_ADMIN_ROLE_ID);

    assertThat(rolePermissions.roleId, is(Role.SYSTEM_ADMIN_ROLE_ID));
    assertThat(rolePermissions.permissionCategories, hasSize(2));
    assertAllowdPermissions(rolePermissions, Permission.CONFIGURE_SYSTEM, Permission.VIEW_ROLES);

    PermissionCategoryDTO category = rolePermissions.permissionCategories.get(0);
    assertThat(category.displayName, is(PermissionCategory.ADMINISTRATOR.getDisplayName()));
    assertListedPermissions(category, Permission.CONFIGURE_SYSTEM, Permission.EDIT_ROLES, Permission.VIEW_ROLES,
        Permission.MANAGE_PROPRIETARY);

    category = rolePermissions.permissionCategories.get(1);
    assertThat(category.displayName, is(PermissionCategory.CLM.getDisplayName()));
    assertListedPermissions(category, Permission.CLAIM_COMPONENT, Permission.WRITE, Permission.READ,
        Permission.EVALUATE_APPLICATION, Permission.EVALUATE_COMPONENT);
  }

  @Test
  public void testGetPermissionsForRole_CustomRole() {
    String roleId = tempEntity.newRole(false, Permission.WRITE).getId();
    RolePermissionDTO rolePermissions = rolePermissionService.getPermissionsForRole(roleId);

    assertThat(rolePermissions.roleId, is(roleId));
    assertThat(rolePermissions.permissionCategories, hasSize(2));
    assertAllowdPermissions(rolePermissions, Permission.WRITE);

    PermissionCategoryDTO category = rolePermissions.permissionCategories.get(0);
    assertThat(category.displayName, is(PermissionCategory.ADMINISTRATOR.getDisplayName()));
    assertListedPermissions(category, Permission.VIEW_ROLES, Permission.MANAGE_PROPRIETARY);

    category = rolePermissions.permissionCategories.get(1);
    assertThat(category.displayName, is(PermissionCategory.CLM.getDisplayName()));
    assertListedPermissions(category, Permission.CLAIM_COMPONENT, Permission.WRITE, Permission.READ,
        Permission.EVALUATE_APPLICATION, Permission.EVALUATE_COMPONENT);
  }

  @Test
  public void testGetPermissionsForNewCustomRole() {
    RolePermissionDTO rolePermissions = rolePermissionService.getPermissionsForNewCustomRole();

    assertThat(rolePermissions.roleId, is(nullValue()));
    assertThat(rolePermissions.permissionCategories, hasSize(2));
    assertAllowdPermissions(rolePermissions);

    PermissionCategoryDTO category = rolePermissions.permissionCategories.get(0);
    assertThat(category.displayName, is(PermissionCategory.ADMINISTRATOR.getDisplayName()));
    assertListedPermissions(category, Permission.VIEW_ROLES, Permission.MANAGE_PROPRIETARY);

    category = rolePermissions.permissionCategories.get(1);
    assertThat(category.displayName, is(PermissionCategory.CLM.getDisplayName()));
    assertListedPermissions(category, Permission.CLAIM_COMPONENT, Permission.WRITE, Permission.READ,
        Permission.EVALUATE_APPLICATION, Permission.EVALUATE_COMPONENT);
  }
}
