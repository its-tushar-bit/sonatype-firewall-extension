/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.List;

import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Inject;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

public class RoleServiceTest
    extends AbstractComponentTest
{
  @Inject
  private RoleService roleService;

  @Test
  public void testGetAllRoles() {
    List<Role> roles = roleService.getAllRoles();
    assertThat(roles.size(), greaterThan(0));
  }
}
