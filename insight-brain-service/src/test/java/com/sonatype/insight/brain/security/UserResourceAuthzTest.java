/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import com.ning.http.client.Response;
import org.junit.Test;

public class UserResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testGetAll() throws Exception {
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.ADMIN_ROLE_ID, authorized.getUsername());

    String url = getRestUrl(UserResource.SERVICE_PATH);
    testAuthzGet(url);
  }

  @Test
  public void testAddUser() throws Exception {
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.ADMIN_ROLE_ID, authorized.getUsername());

    User user = new User("testAddUser", "testAddUser", "testAddUser", "testAddUser", "testAddUser@sonatype.com");
    String url = getRestUrl(UserResource.SERVICE_PATH);
    Response response = testAuthzPost(url, toJson(user));
    user = fromJson(response, User.class);
    new UserDAO().delete(user);
  }

  @Test
  public void testUpdateUser() throws Exception {
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.ADMIN_ROLE_ID, authorized.getUsername());

    User user = tempEntity.newUser("testUpdateUser");
    String url = getRestUrl(UserResource.SERVICE_PATH);
    testAuthzPut(url, toJson(user));
  }

  @Test
  public void testDeleteUser() throws Exception {
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.ADMIN_ROLE_ID, authorized.getUsername());

    User user = tempEntity.newUser("testDeleteUser");
    String url = getRestUrl(UserResource.SERVICE_PATH + "/{userId}", user.getId());
    testAuthzDelete(url);
  }
}
