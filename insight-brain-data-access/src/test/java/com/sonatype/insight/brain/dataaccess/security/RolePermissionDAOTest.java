/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class RolePermissionDAOTest
    extends AbstractDbDAOTest
{
  private RolePermissionDAO permDAO = new RolePermissionDAO();

  private RoleDAO roleDAO = new RoleDAO();

  @Test
  public void testAdminRoleHasAllPermissions() throws Exception {
    Role role = roleDAO.getByName("Administrator");
    assertThat(role, is(notNullValue()));
    Set<Permission> perms = permDAO.getPermissionsForRole(role.getId());
    assertThat(perms, is(notNullValue()));
    assertThat(perms, containsInAnyOrder(Permission.values()));
  }

  @Test
  public void testOwnerRoleHasReadWritePermission() throws Exception {
    Role role = roleDAO.getByName("Owner");
    assertThat(role, is(notNullValue()));
    Set<Permission> perms = permDAO.getPermissionsForRole(role.getId());
    assertThat(perms, is(notNullValue()));
    assertThat(perms, containsInAnyOrder(Permission.WRITE, Permission.READ));
  }

  @Test
  public void testDeveloperRoleHasOnlyReadPermission() throws Exception {
    Role role = roleDAO.getByName("Developer");
    assertThat(role, is(notNullValue()));
    Set<Permission> perms = permDAO.getPermissionsForRole(role.getId());
    assertThat(perms, is(notNullValue()));
    assertThat(perms, containsInAnyOrder(Permission.READ));
  }
}
