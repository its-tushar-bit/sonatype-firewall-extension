/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.EnumSet;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RolePermissionDAOTest
    extends AbstractDbDAOTest
{
  private RolePermissionDAO permDAO = new RolePermissionDAO();

  private RoleDAO roleDAO = new RoleDAO();

  @Test
  public void testSystemAdminRoleHasConfigureSystemPermissions() throws Exception {
    Role role = roleDAO.getById(Role.SYSTEM_ADMIN_ROLE_ID);
    assertThat(role).isNotNull();
    Set<Permission> perms = permDAO.getPermissionsForRole(role.getId());
    assertThat(perms).containsExactlyInAnyOrder(Permission.CONFIGURE_SYSTEM, Permission.VIEW_ROLES);
  }

  @Test
  public void testPolicyAdminRoleHasIqPermissions() throws Exception {
    Role role = roleDAO.getById(Role.POLICY_ADMIN_ROLE_ID);
    assertThat(role).isNotNull();
    Set<Permission> perms = permDAO.getPermissionsForRole(role.getId());
    assertThat(perms).containsExactlyInAnyOrder(Permission.EDIT_ROLES, Permission.VIEW_ROLES,
        Permission.MANAGE_PROPRIETARY, Permission.WRITE, Permission.READ, Permission.EVALUATE_APPLICATION,
        Permission.EVALUATE_COMPONENT, Permission.CLAIM_COMPONENT, Permission.ADD_APPLICATION,
        Permission.MANAGE_AUTOMATIC_APPLICATION_CREATION, Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION,
        Permission.EDIT_ACCESS_CONTROL);
  }

  @Test
  public void testOwnerRoleHasExpectedPermissions() throws Exception {
    Role role = roleDAO.getByName("Owner");
    assertThat(role).isNotNull();
    Set<Permission> perms = permDAO.getPermissionsForRole(role.getId());
    assertThat(perms).containsExactlyInAnyOrder(Permission.WRITE, Permission.READ, Permission.EVALUATE_APPLICATION,
        Permission.EVALUATE_COMPONENT, Permission.VIEW_ROLES, Permission.ADD_APPLICATION,
        Permission.MANAGE_PROPRIETARY, Permission.EDIT_ACCESS_CONTROL);
  }

  @Test
  public void testDeveloperRoleHasExpectedPermissions() throws Exception {
    Role role = roleDAO.getByName("Developer");
    assertThat(role).isNotNull();
    Set<Permission> perms = permDAO.getPermissionsForRole(role.getId());
    assertThat(perms).containsExactlyInAnyOrder(Permission.READ, Permission.EVALUATE_COMPONENT);
  }

  @Test
  public void testGetRoleIdsByPermission() throws Exception {
    String roleId = tempEntity.newRole("testing", false /* global */).getId();
    for (Permission perm : Permission.values()) {
      assertThat(permDAO.getRoleIdsByPermission(perm)).doesNotContain(roleId);
    }

    RolePermission rolePerm = new RolePermission(roleId, Permission.values()[0]);
    permDAO.insert(rolePerm);
    for (Permission perm : Permission.values()) {
      if (Permission.values()[0].equals(perm)) {
        assertThat(permDAO.getRoleIdsByPermission(perm)).contains(roleId);
      }
      else {
        assertThat(permDAO.getRoleIdsByPermission(perm)).doesNotContain(roleId);
      }
    }

    permDAO.delete(rolePerm);
    for (Permission perm : Permission.values()) {
      assertThat(permDAO.getRoleIdsByPermission(perm)).doesNotContain(roleId);
    }
  }

  @Test
  public void testUpdateNotSupported() {
    RolePermission rolePerm = permDAO.getByRoleId(tempEntity.newRole(false, Permission.WRITE).getId()).get(0);
    rolePerm.setPermission(Permission.READ);
    assertThatThrownBy(() -> {
      permDAO.update(rolePerm);
    }).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void testSetPermissionsForRole_BuiltInRolesAreReadOnly() {
    Role role = roleDAO.getById(Role.SYSTEM_ADMIN_ROLE_ID);
    assertThatThrownBy(() -> {
      permDAO.setPermissionsForRole(role.getId(), EnumSet.of(Permission.CONFIGURE_SYSTEM));
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("Cannot change permissions for built-in role '" + role.getName() + "'");
    assertThat(permDAO.getPermissionsForRole(role.getId())).hasSize(2);
  }

  @Test
  public void testSetPermissionsForRole_CustomRolesCannotGetCertainPermissions() {
    Role role = tempEntity.newRole("Tester", false);
    assertThat(Permission.CONFIGURE_SYSTEM.isAllowedInCustomRoles()).isFalse();
    assertThatThrownBy(() -> {
      permDAO.setPermissionsForRole(role.getId(), EnumSet.of(Permission.CONFIGURE_SYSTEM));
    }).isInstanceOf(BadRequestException.class).hasMessage(
        "Cannot assign permission '" + Permission.CONFIGURE_SYSTEM + "' to custom role '" + role.getName() + "'");
    assertThat(permDAO.getPermissionsForRole(role.getId())).isEmpty();
  }

  @Test
  public void testSetPermissionsForRole() {
    Role role = tempEntity.newRole("Tester", false);
    Set<Permission> permissions = EnumSet.of(Permission.WRITE, Permission.READ);
    permDAO.setPermissionsForRole(role.getId(), permissions);
    assertThat(permDAO.getPermissionsForRole(role.getId())).isEqualTo(permissions);

    permissions = EnumSet.of(Permission.WRITE, Permission.EVALUATE_APPLICATION);
    permDAO.setPermissionsForRole(role.getId(), permissions);
    assertThat(permDAO.getPermissionsForRole(role.getId())).isEqualTo(permissions);

    permissions.clear();
    permDAO.setPermissionsForRole(role.getId(), permissions);
    assertThat(permDAO.getPermissionsForRole(role.getId())).isEmpty();
  }
}
