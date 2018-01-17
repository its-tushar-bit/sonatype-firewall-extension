/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.PermissionCategory;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

public class RoleServiceTest
    extends AbstractComponentTest
{
  @Inject
  private RoleService roleService;

  @Test
  public void testGetAllRoles() {
    List<RoleDTO> roles = roleService.getAllRoles();
    assertThat(roles.size(), greaterThan(0));
  }

  @Test
  public void updateRole_Permissions() {
    Role role = tempEntity.newRole(false, Permission.READ);

    RoleDTO roleDTO = roleService.getRoleById(role.getId());
    setAllowedPermissions(roleDTO, Arrays.asList(Permission.WRITE, Permission.EVALUATE_APPLICATION));

    roleService.updateRole(roleDTO);
    RoleDTO updatedRoleDTO = roleService.getRoleById(role.getId());
    assertAllowedPermissions(updatedRoleDTO, Permission.READ, Permission.WRITE, Permission.EVALUATE_APPLICATION);
  }

  @Test
  public void testGetRoleById_Builtin() {
    RoleDTO roleDTO = roleService.getRoleById(Role.SYSTEM_ADMIN_ROLE_ID);

    assertThat(roleDTO.id, is(Role.SYSTEM_ADMIN_ROLE_ID));
    assertThat(roleDTO.permissionCategories, hasSize(2));
    assertAllowedPermissions(roleDTO, Permission.CONFIGURE_SYSTEM, Permission.VIEW_ROLES);

    PermissionCategoryDTO category = roleDTO.permissionCategories.get(0);
    assertThat(category.displayName, is(PermissionCategory.ADMINISTRATOR.getDisplayName()));
    assertListedPermissions(category, Permission.CONFIGURE_SYSTEM, Permission.EDIT_ROLES, Permission.VIEW_ROLES);

    category = roleDTO.permissionCategories.get(1);
    assertThat(category.displayName, is(PermissionCategory.IQ.getDisplayName()));
    assertListedPermissions(category, Permission.MANAGE_PROPRIETARY, Permission.CLAIM_COMPONENT, Permission.WRITE,
        Permission.READ, Permission.EVALUATE_APPLICATION, Permission.EVALUATE_COMPONENT, Permission.ADD_APPLICATION,
        Permission.MANAGE_AUTOMATIC_APPLICATION_CREATION);
  }

  @Test
  public void testGetRoleById_Custom() {
    Role expectedRole = tempEntity.newRole(false, Permission.WRITE);

    RoleDTO roleDTO = roleService.getRoleById(expectedRole.getId());
    assertThat(roleDTO.id, is(expectedRole.getId()));
    assertThat(roleDTO.name, is(expectedRole.getName()));
    assertThat(roleDTO.description, is(expectedRole.getDescription()));

    assertThat(roleDTO.permissionCategories, hasSize(2));
    assertAllowedPermissions(roleDTO, Permission.WRITE);

    PermissionCategoryDTO category = roleDTO.permissionCategories.get(0);
    assertThat(category.displayName, is(PermissionCategory.ADMINISTRATOR.getDisplayName()));
    assertListedPermissions(category, Permission.VIEW_ROLES);

    category = roleDTO.permissionCategories.get(1);
    assertThat(category.displayName, is(PermissionCategory.IQ.getDisplayName()));
    assertListedPermissions(category, Permission.MANAGE_PROPRIETARY, Permission.CLAIM_COMPONENT, Permission.WRITE,
        Permission.READ, Permission.EVALUATE_APPLICATION, Permission.EVALUATE_COMPONENT, Permission.ADD_APPLICATION,
        Permission.MANAGE_AUTOMATIC_APPLICATION_CREATION);
  }

  @Test
  public void testGetTemplateForNewRole() {
    RoleDTO roleDTO = roleService.getTemplateForNewRole();
    assertThat(roleDTO.id, nullValue());
    assertThat(roleDTO.name, nullValue());
    assertThat(roleDTO.description, nullValue());

    assertThat(roleDTO.permissionCategories, hasSize(2));
    assertAllowedPermissions(roleDTO);

    PermissionCategoryDTO category = roleDTO.permissionCategories.get(0);
    assertThat(category.displayName, is(PermissionCategory.ADMINISTRATOR.getDisplayName()));
    assertListedPermissions(category, Permission.VIEW_ROLES);

    category = roleDTO.permissionCategories.get(1);
    assertThat(category.displayName, is(PermissionCategory.IQ.getDisplayName()));
    assertListedPermissions(category, Permission.MANAGE_PROPRIETARY, Permission.CLAIM_COMPONENT, Permission.WRITE,
        Permission.READ, Permission.EVALUATE_APPLICATION, Permission.EVALUATE_COMPONENT, Permission.ADD_APPLICATION,
        Permission.MANAGE_AUTOMATIC_APPLICATION_CREATION);
  }

  private void setAllowedPermissions(final RoleDTO roleDTO, final List<Permission> permissions) {
    for (PermissionCategoryDTO categoryDTO : roleDTO.permissionCategories) {
      for (PermissionDTO permissionDTO : categoryDTO.permissions) {
        if (permissions.contains(permissionDTO.id)) {
          permissionDTO.allowed = true;
        }
      }
    }
  }

  private void assertListedPermissions(final PermissionCategoryDTO actual, final Permission... permissions) {
    assertThat(actual.permissions, hasSize(permissions.length));
    for (int i = 0; i < permissions.length; i++) {
      assertThat(actual.permissions.get(i).id, is(permissions[i]));
      assertThat(actual.permissions.get(i).displayName, is(permissions[i].getDisplayName()));
      assertThat(actual.permissions.get(i).description, is(permissions[i].getDescription()));
    }
  }

  private void assertAllowedPermissions(final RoleDTO actual, final Permission... permissions) {
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
}
