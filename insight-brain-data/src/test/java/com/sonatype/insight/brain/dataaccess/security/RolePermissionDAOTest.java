/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;

import org.junit.After;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class RolePermissionDAOTest
    extends AbstractDbDAOTest
{
  private RolePermissionDAO permDAO = new RolePermissionDAO();

  private RoleDAO roleDAO = new RoleDAO();

  private Collection<Role> rolesToDelete = new ArrayList<>();

  @After
  public void exit() {
    for (Role role : rolesToDelete) {
      roleDAO.delete(role);
    }
  }

  private Role newRole(String name) {
    Role role = new Role();
    role.setName(name);
    roleDAO.insert(role);
    rolesToDelete.add(role);
    return role;
  }

  @Test
  public void testSystemAdminRoleHasConfigureSystemPermissions() throws Exception {
    Role role = roleDAO.getById(Role.SYSTEM_ADMIN_ROLE_ID);
    assertThat(role, is(notNullValue()));
    Set<Permission> perms = permDAO.getPermissionsForRole(role.getId());
    assertThat(perms, contains(Permission.CONFIGURE_SYSTEM));
  }

  @Test
  public void testClmAdminRoleHasClmPermissions() throws Exception {
    Role role = roleDAO.getById(Role.CLM_ADMIN_ROLE_ID);
    assertThat(role, is(notNullValue()));
    Set<Permission> perms = permDAO.getPermissionsForRole(role.getId());
    assertThat(perms, contains(Permission.MANAGE_PROPRIETARY, Permission.WRITE, Permission.READ,
        Permission.EVALUATE_APPLICATION, Permission.EVALUATE_COMPONENT));
  }

  @Test
  public void testOwnerRoleHasExpectedPermissions() throws Exception {
    Role role = roleDAO.getByName("Owner");
    assertThat(role, is(notNullValue()));
    Set<Permission> perms = permDAO.getPermissionsForRole(role.getId());
    assertThat(perms, is(notNullValue()));
    assertThat(
        perms,
        containsInAnyOrder(Permission.WRITE, Permission.READ, Permission.EVALUATE_APPLICATION,
            Permission.EVALUATE_COMPONENT));
  }

  @Test
  public void testDeveloperRoleHasExpectedPermissions() throws Exception {
    Role role = roleDAO.getByName("Developer");
    assertThat(role, is(notNullValue()));
    Set<Permission> perms = permDAO.getPermissionsForRole(role.getId());
    assertThat(perms, is(notNullValue()));
    assertThat(perms, containsInAnyOrder(Permission.READ, Permission.EVALUATE_COMPONENT));
  }

  @Test
  public void testGetRoleIdsByPermission() throws Exception {
    String roleId = newRole("testing").getId();
    for (Permission perm : Permission.values()) {
      assertThat(permDAO.getRoleIdsByPermission(perm), not(hasItem(roleId)));
    }

    RolePermission rolePerm = new RolePermission(roleId, Permission.values()[0]);
    permDAO.insert(rolePerm);
    for (Permission perm : Permission.values()) {
      if (Permission.values()[0].equals(perm)) {
        assertThat(permDAO.getRoleIdsByPermission(perm), hasItem(roleId));
      }
      else {
        assertThat(permDAO.getRoleIdsByPermission(perm), not(hasItem(roleId)));
      }
    }

    permDAO.delete(rolePerm);
    for (Permission perm : Permission.values()) {
      assertThat(permDAO.getRoleIdsByPermission(perm), not(hasItem(roleId)));
    }
  }
}
