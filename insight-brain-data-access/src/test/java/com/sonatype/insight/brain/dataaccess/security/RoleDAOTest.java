/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.security.Role;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class RoleDAOTest
    extends AbstractDbDAOTest
{
  private RoleDAO roleDAO = new RoleDAO();

  @Test
  public void testGetGlobalRoles() throws Exception {
    List<Role> roles = roleDAO.getGlobalRoles();
    assertThat(roles, is(notNullValue()));
    assertThat(roles, hasSize(1));
    Role role = roles.get(0);
    assertThat(role.getName(), is("Administrator"));
    assertThat(role.isGlobal(), is(true));
  }

  @Test
  public void testGetApplicationRoles() throws Exception {
    List<Role> roles = roleDAO.getApplicationRoles();
    assertThat(roles, is(notNullValue()));
    assertThat(roles, hasSize(2));
    Role role = roles.get(0);
    assertThat(role.getName(), is("Developer"));
    assertThat(role.isGlobal(), is(false));
    role = roles.get(1);
    assertThat(role.getName(), is("Owner"));
    assertThat(role.isGlobal(), is(false));
  }
}
