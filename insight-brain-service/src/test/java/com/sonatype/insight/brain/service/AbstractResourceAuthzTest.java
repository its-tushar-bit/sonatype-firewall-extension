/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.junit.Before;

/**
 * Provides boilerplate fixture for authorization tests.
 */
public abstract class AbstractResourceAuthzTest
    extends AbstractResourceTest
{
  protected Organization org;

  protected Application app;

  protected User unauthorized;

  protected User authorized;

  @Before
  public void createEntities() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    unauthorized = tempEntity.newUser();
    authorized = tempEntity.newUser();
  }

  protected void grantAdminPermission() {
    Role role = tempEntity.newRole(true /* global */, Permission.ADMIN);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), authorized.getUsername());
  }

  protected void grantWritePermission() {
    Role role = tempEntity.newRole(true /* global */, Permission.WRITE);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), authorized.getUsername());
  }

  protected void grantWritePermission(String contextId) {
    Role role = tempEntity.newRole(false /* global */, Permission.WRITE);
    tempEntity.newMembershipMapping(contextId, role.getId(), authorized.getUsername());
  }

  protected void grantReadPermission(String contextId) {
    Role role = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(contextId, role.getId(), authorized.getUsername());
  }

  protected void testAuthzGet(String url) throws Exception {
    testAuthzGet(url, 200);
  }

  protected void testAuthzGet(String url, int expectedSuccessStatus) throws Exception {
    Response response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(expectedSuccessStatus, response);
  }

  // Sometimes, simply being able to log in, is all the authorization you need...
  protected void testAuthcGet(String url) throws Exception {
    Response response = RestAccess.get(url);
    assertResponseStatus(401, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
  }

  protected void testAuthzPut(String url, String body) throws Exception {
    testAuthzPut(url, body, 200);
  }

  protected void testAuthzPut(String url, String body, int expectedSuccessStatus) throws Exception {
    Response response = RestAccess.put(url, unauthorized.getUsername(), unauthorized.getPassword(), body);
    assertResponseStatus(403, response);

    response = RestAccess.put(url, authorized.getUsername(), authorized.getPassword(), body);
    assertResponseStatus(expectedSuccessStatus, response);
  }

  protected Response testAuthzPost(String url, String body) throws Exception {
    return testAuthzPost(url, body, 200);
  }

  protected Response testAuthzPost(String url, String body, int expectedSuccessStatus) throws Exception {
    Response response = RestAccess.post(url, unauthorized.getUsername(), unauthorized.getPassword(), body);
    assertResponseStatus(403, response);

    response = RestAccess.post(url, authorized.getUsername(), authorized.getPassword(), body);
    assertResponseStatus(expectedSuccessStatus, response);

    return response;
  }

  protected Response testAuthzDelete(String url) throws Exception {
    Response response = RestAccess.delete(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.delete(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(204, response);

    return response;
  }
}
