/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RolePermissionDAOTest
    extends AbstractDbDAOTest
{
  private RolePermissionDAO permDAO;

  private RoleDAO roleDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    permDAO = daoFactory.createRolePermissionDAO();
    roleDAO = daoFactory.createRoleDAO();
  }

  @Test
  public void testSystemAdminRoleHasConfigureSystemPermissions() {
    Role role = roleDAO.getById(Role.SYSTEM_ADMIN_ROLE_ID);
    assertThat(role).isNotNull();
    Set<Permission> perms = permDAO.getPermissionsForRole(role.getId());
    assertThat(perms).containsExactlyInAnyOrder(Permission.CONFIGURE_SYSTEM, Permission.VIEW_ROLES,
        Permission.ACCESS_AUDIT_LOG);
  }

  @Test
  public void testPolicyAdminRoleHasIqPermissions() {
    Role role = roleDAO.getById(Role.POLICY_ADMIN_ROLE_ID);
    assertThat(role).isNotNull();
    Set<Permission> perms = permDAO.getPermissionsForRole(role.getId());
    assertThat(perms).containsExactlyInAnyOrder(Permission.EDIT_ROLES, Permission.VIEW_ROLES,
        Permission.MANAGE_PROPRIETARY, Permission.WRITE, Permission.READ, Permission.EVALUATE_APPLICATION,
        Permission.EVALUATE_COMPONENT, Permission.CLAIM_COMPONENT, Permission.ADD_APPLICATION,
        Permission.MANAGE_AUTOMATIC_APPLICATION_CREATION, Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION,
        Permission.EDIT_ACCESS_CONTROL, Permission.WAIVE_POLICY_VIOLATIONS, Permission.CHANGE_LICENSES,
        Permission.CHANGE_SECURITY_VULNERABILITIES, Permission.LEGAL_REVIEWER, Permission.CREATE_PULL_REQUESTS);
  }

  @Test
  public void testOwnerRoleHasExpectedPermissions() {
    Role role = roleDAO.getByName("Owner");
    assertThat(role).isNotNull();
    Set<Permission> perms = permDAO.getPermissionsForRole(role.getId());
    assertThat(perms).containsExactlyInAnyOrder(Permission.WRITE, Permission.READ, Permission.EVALUATE_APPLICATION,
        Permission.EVALUATE_COMPONENT, Permission.VIEW_ROLES, Permission.ADD_APPLICATION,
        Permission.MANAGE_PROPRIETARY, Permission.EDIT_ACCESS_CONTROL, Permission.WAIVE_POLICY_VIOLATIONS,
        Permission.CHANGE_LICENSES, Permission.CHANGE_SECURITY_VULNERABILITIES, Permission.LEGAL_REVIEWER,
        Permission.CREATE_PULL_REQUESTS);
  }

  @Test
  public void testDeveloperRoleHasExpectedPermissions() {
    Role role = roleDAO.getByName("Developer");
    assertThat(role).isNotNull();
    Set<Permission> perms = permDAO.getPermissionsForRole(role.getId());
    assertThat(perms).containsExactlyInAnyOrder(Permission.READ, Permission.EVALUATE_COMPONENT,
        Permission.CREATE_PULL_REQUESTS);
  }

  @Test
  public void testLegalReviewerRoleHasExpectedPermissions() {
    Role role = roleDAO.getById(Role.LEGAL_REVIEWER_ROLE_ID);
    assertThat(role).isNotNull();
    Set<Permission> perms = permDAO.getPermissionsForRole(role.getId());
    assertThat(perms).containsExactlyInAnyOrder(Permission.READ, Permission.WRITE, Permission.LEGAL_REVIEWER,
        Permission.CHANGE_LICENSES);
  }

  @Test
  public void testGetRoleIdsByPermission() {
    String roleId = tempEntity.newRole("com.sonatype.insight.test.jaxrs.testing", false /* global */).getId();
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
    assertThatThrownBy(() -> permDAO.update(rolePerm)).isInstanceOf(UnsupportedOperationException.class);
  }
}
