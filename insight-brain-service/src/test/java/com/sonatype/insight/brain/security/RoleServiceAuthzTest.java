/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.List;

import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.inject.Inject;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

public class RoleServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private RoleService roleService;

  @Test
  public void testGetAllRoles_Authorized() {
    grantAdminPermission();
    List<Role> roles = roleService.getAllRoles();
    assertThat(roles, notNullValue());
    assertThat(roles, not(empty()));
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAllRoles_Unauthorized() {
    login();
    roleService.getAllRoles();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAllRoles_Unauthenticated() {
    roleService.getAllRoles();
  }
}
