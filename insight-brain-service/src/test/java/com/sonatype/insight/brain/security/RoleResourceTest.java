/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class RoleResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetAllRoles() throws Exception {
    Response response = AuthedRestAccess.get(getRestUrl((RoleResource.SERVICE_PATH)));
    assertResponseStatus(200, response);
    Role[] roles = fromJson(response, Role[].class);
    assertThat(roles, not(emptyArray()));
  }

  @Test
  public void testAddRole() throws Exception {
    Role role = new Role("New Name", "New Description");
    Response response = AuthedRestAccess.post(getRestUrl(RoleResource.SERVICE_PATH), toJson(role));
    assertResponseStatus(200, response);
    Role newRole = fromJson(response, Role.class);
    assertThat(newRole.getId(), is(notNullValue()));
    assertThat(newRole.getName(), is(role.getName()));
    assertThat(newRole.getDescription(), is(role.getDescription()));
  }

  @Test
  public void testUpdateRole() throws Exception {
    Role role = tempEntity.newRole(false);
    role.setName("Updated Name");
    role.setDescription("Updated Description");
    Response response = AuthedRestAccess.put(getRestUrl(RoleResource.SERVICE_PATH), toJson(role));
    assertResponseStatus(200, response);
    Role updatedRole = fromJson(response, Role.class);
    assertThat(updatedRole.getId(), is(role.getId()));
    assertThat(updatedRole.getName(), is(role.getName()));
    assertThat(updatedRole.getDescription(), is(role.getDescription()));
  }

  @Test
  public void testDeleteRole() throws Exception {
    Role role = tempEntity.newRole(false);
    Response response = AuthedRestAccess.delete(getRestUrl(RoleResource.SERVICE_PATH + "/{roleId}", role.getId()));
    assertResponseStatus(204, response);
    assertThat(new RoleDAO().getById(role.getId()), is(nullValue()));
  }
}
