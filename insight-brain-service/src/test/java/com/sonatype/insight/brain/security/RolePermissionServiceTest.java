/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.inject.Inject;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class RolePermissionServiceTest
    extends AbstractComponentTest
{
  @Inject
  private RolePermissionService rolePermissionService;

  @Test
  public void testGetAllPermissionsForRole() {
    RolePermissionDTO rolePermissions = rolePermissionService.getPermissionsForRole(Role.SYSTEM_ADMIN_ROLE_ID);
    RolePermissionDTO expected = getExpectedRolePermissions();
    assertThat(rolePermissions.permissionCategories.size(), is(expected.permissionCategories.size()));
    for (int i = 0; i < rolePermissions.permissionCategories.size(); i++) {
      PermissionCategoryDTO actualCategory = rolePermissions.permissionCategories.get(i);
      PermissionCategoryDTO expectedCategory = rolePermissions.permissionCategories.get(i);
      assertThat(actualCategory.displayName, is(expectedCategory.displayName));
      assertThat(actualCategory.permissions, hasSize(expectedCategory.permissions.size()));
      for (int j = 0; j < actualCategory.permissions.size(); j++) {
        assertThat(actualCategory.permissions.get(j).displayName, is(expectedCategory.permissions.get(j).displayName));
        assertThat(actualCategory.permissions.get(j).description, is(expectedCategory.permissions.get(j).description));
        assertThat(actualCategory.permissions.get(j).allowed, is(expectedCategory.permissions.get(j).allowed));
      }
    }
  }

  @Test
  public void testGetAllPermissionsForNewRole() {
    RolePermissionDTO rolePermissions = rolePermissionService.getPermissionsForNewRole();
    RolePermissionDTO expected = getExpectedRolePermissions();
    assertThat(rolePermissions.permissionCategories.size(), is(expected.permissionCategories.size()));
    for (int i = 0; i < rolePermissions.permissionCategories.size(); i++) {
      PermissionCategoryDTO actualCategory = rolePermissions.permissionCategories.get(i);
      PermissionCategoryDTO expectedCategory = rolePermissions.permissionCategories.get(i);
      assertThat(actualCategory.displayName, is(expectedCategory.displayName));
      assertThat(actualCategory.permissions, hasSize(expectedCategory.permissions.size()));
      for (int j = 0; j < actualCategory.permissions.size(); j++) {
        assertThat(actualCategory.permissions.get(j).displayName, is(expectedCategory.permissions.get(j).displayName));
        assertThat(actualCategory.permissions.get(j).description, is(expectedCategory.permissions.get(j).description));
      }
    }
  }

  private RolePermissionDTO getExpectedRolePermissions() {
    ListMultimap<String, PermissionDTO> permissionsByCategoryMap = ArrayListMultimap.create();
    for (Permission perm : EnumSet.allOf(Permission.class)) {
      permissionsByCategoryMap.put(perm.getCategory(), new PermissionDTO(perm, true));
    }

    RolePermissionDTO rolePermissionDTO = new RolePermissionDTO(Role.SYSTEM_ADMIN_ROLE_ID);
    for (String category : permissionsByCategoryMap.keySet()) {
      List<PermissionDTO> permissions = permissionsByCategoryMap.get(category);
      PermissionCategoryDTO permissionCategoryDTO = new PermissionCategoryDTO(category);
      permissionCategoryDTO.permissions = permissions;
      rolePermissionDTO.permissionCategories.add(permissionCategoryDTO);
    }
    Collections.sort(rolePermissionDTO.permissionCategories, RolePermissionService.PERMISSION_CATEGORY_COMPARATOR);
    return rolePermissionDTO;
  }
}
