/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.inject.Inject;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

public class RolePermissionServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private RolePermissionService rolePermissionService;

  @Test
  public void testPermissionsForRole_Authorized() {
    grantConfigureSystemPermission();
    RolePermissionDTO rolePermission = rolePermissionService.getPermissionsForRole(Role.ADMIN_ROLE_ID);
    assertThat(rolePermission.permissionCategories, not(empty()));
    assertThat(rolePermission.permissionCategories.get(0).permissions, not(empty()));
  }

  @Test(expected = UnauthorizedException.class)
  public void testPermissionsForRole_Unauthorized() {
    login();
    rolePermissionService.getPermissionsForRole(Role.ADMIN_ROLE_ID);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testPermissionsForRole_Unauthenticated() {
    rolePermissionService.getPermissionsForRole(Role.ADMIN_ROLE_ID);
  }
}
