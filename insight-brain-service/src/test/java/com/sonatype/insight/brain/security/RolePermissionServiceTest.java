/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.EnumSet;
import java.util.Set;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RolePermissionServiceTest
    extends AbstractComponentTest
{
  @Inject
  private RoleDAO roleDAO;

  @Inject
  private RolePermissionService rolePermissionService;

  @Test
  public void testSetPermissionsForRole_BuiltInRolesAreReadOnly() {
    Role role = roleDAO.getById(Role.SYSTEM_ADMIN_ROLE_ID);
    assertThatThrownBy(
        () -> rolePermissionService.setPermissionsForRole(role.getId(), EnumSet.of(Permission.CONFIGURE_SYSTEM)))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Cannot change permissions for built-in role '" + role.getName() + "'");
    assertThat(rolePermissionService.getPermissionsForRole(role.getId())).hasSize(3);
  }

  @Test
  public void testSetPermissionsForRole_CustomRolesCannotGetCertainPermissions() {
    Role role = tempEntity.newRole("Tester", false);
    assertThat(Permission.CONFIGURE_SYSTEM.isAllowedInCustomRoles()).isFalse();
    assertThatThrownBy(
        () -> rolePermissionService.setPermissionsForRole(role.getId(), EnumSet.of(Permission.CONFIGURE_SYSTEM)))
            .isInstanceOf(BadRequestException.class)
            .hasMessage(
                "Cannot assign permission '" + Permission.CONFIGURE_SYSTEM + "' to custom role '" + role.getName()
                    + "'");
    assertThat(rolePermissionService.getPermissionsForRole(role.getId())).isEmpty();
  }

  @Test
  public void testSetPermissionsForRole() {
    Role role = tempEntity.newRole("Tester", false);
    Set<Permission> permissions = EnumSet.of(Permission.WRITE, Permission.READ);
    rolePermissionService.setPermissionsForRole(role.getId(), permissions);
    assertThat(rolePermissionService.getPermissionsForRole(role.getId())).isEqualTo(permissions);

    permissions = EnumSet.of(Permission.WRITE, Permission.EVALUATE_APPLICATION);
    rolePermissionService.setPermissionsForRole(role.getId(), permissions);
    assertThat(rolePermissionService.getPermissionsForRole(role.getId())).isEqualTo(permissions);

    permissions.clear();
    rolePermissionService.setPermissionsForRole(role.getId(), permissions);
    assertThat(rolePermissionService.getPermissionsForRole(role.getId())).isEmpty();
  }
}
