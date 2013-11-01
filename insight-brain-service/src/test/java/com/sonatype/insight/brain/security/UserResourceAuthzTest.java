/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;

import com.ning.http.client.Response;
import org.junit.Test;

public class UserResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testGetAll() throws Exception {
    grantAdminPermission();

    String url = getRestUrl(UserResource.SERVICE_PATH);
    testAuthzGet(url);
  }

  @Test
  public void testFindUsers() throws Exception {
    grantWritePermission(app.getId());
    String url = getRestUrl(
        UserResource.SERVICE_PATH + "/{ownerType: global|application|organization}/{ownerId}/query",
        IdUtils.TYPE_APPLICATION, app.getPublicId())
        + "?q=name";
    testAuthzGet(url);

    grantWritePermission(org.getId());
    url = getRestUrl(UserResource.SERVICE_PATH + "/{ownerType: global|application|organization}/{ownerId}/query",
        IdUtils.TYPE_ORGANIZATION, org.getId()) + "?q=name";
    testAuthzGet(url);

    grantWritePermission();
    url = getRestUrl(UserResource.SERVICE_PATH + "/{ownerType: global|application|organization}/{ownerId}/query",
        IdUtils.TYPE_GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID) + "?q=name";
    testAuthzGet(url);
  }

  @Test
  public void testAddUser() throws Exception {
    grantAdminPermission();

    User user = new User("testAddUser", "testAddUser", "testAddUser", "testAddUser", "testAddUser@sonatype.com");
    String url = getRestUrl(UserResource.SERVICE_PATH);
    Response response = testAuthzPost(url, toJson(user));
    user = fromJson(response, User.class);
    new UserDAO().delete(user);
  }

  @Test
  public void testUpdateUser() throws Exception {
    grantAdminPermission();

    User user = tempEntity.newUser("testUpdateUser");
    String url = getRestUrl(UserResource.SERVICE_PATH);
    testAuthzPut(url, toJson(user));
  }

  @Test
  public void testDeleteUser() throws Exception {
    grantAdminPermission();

    User user = tempEntity.newUser("testDeleteUser");
    String url = getRestUrl(UserResource.SERVICE_PATH + "/{userId}", user.getId());
    testAuthzDelete(url);
  }
}
