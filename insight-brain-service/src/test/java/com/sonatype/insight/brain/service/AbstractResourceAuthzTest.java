/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

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

  protected void grantConfigureSystemPermission() {
    Role role = tempEntity.newRole(true /* global */, Permission.CONFIGURE_SYSTEM);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), authorized.getUsername());
  }

  protected void grantManageProprietaryPermission() {
    Role role = tempEntity.newRole(true /* global */, Permission.MANAGE_PROPRIETARY);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), authorized.getUsername());
  }

  protected void grantWritePermission() {
    Role role = tempEntity.newRole(true /* global */, Permission.WRITE);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), authorized.getUsername());
  }

  protected void grantWritePermission(String contextId) {
    grantPermission(contextId, Permission.WRITE);
  }

  protected void grantReadPermission(String contextId) {
    grantPermission(contextId, Permission.READ);
  }

  protected void grantPermission(String contextId, Permission permission) {
    Role role = tempEntity.newRole(false /* global */, permission);
    tempEntity.newMembershipMapping(contextId, role.getId(), authorized.getUsername());
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().anon();
  }

  protected Response testAuthzGet(HttpRequest request) throws Exception {
    return testAuthzGet(request, 200);
  }

  protected Response testAuthzGet(HttpRequest request, int expectedSuccessStatus) throws Exception {
    Response response = request.auth(unauthorized.getUsername(), unauthorized.getPassword()).get();
    assertResponseStatus(403, response);

    response = request.auth(authorized.getUsername(), authorized.getPassword()).get();
    assertResponseStatus(expectedSuccessStatus, response);
    return response;
  }

  // Sometimes, simply being able to log in, is all the authorization you need...
  protected Response testAuthcGet(HttpRequest request) throws Exception {
    Response response = request.anon().get();
    assertResponseStatus(401, response);

    response = request.auth(authorized.getUsername(), authorized.getPassword()).get();
    assertResponseStatus(200, response);
    return response;
  }

  protected Response testAuthzPut(HttpRequest request) throws Exception {
    return testAuthzPut(request, 200);
  }

  protected Response testAuthzPut(HttpRequest request, int expectedSuccessStatus) throws Exception {
    Response response = request.auth(unauthorized.getUsername(), unauthorized.getPassword()).put();
    assertResponseStatus(403, response);

    response = request.auth(authorized.getUsername(), authorized.getPassword()).put();
    assertResponseStatus(expectedSuccessStatus, response);
    return response;
  }

  protected Response testAuthzPost(HttpRequest request) throws Exception {
    return testAuthzPost(request, 200);
  }

  protected Response testAuthzPost(HttpRequest request, int expectedSuccessStatus) throws Exception {
    Response response = request.auth(unauthorized.getUsername(), unauthorized.getPassword()).post();
    assertResponseStatus(403, response);

    response = request.auth(authorized.getUsername(), authorized.getPassword()).post();
    assertResponseStatus(expectedSuccessStatus, response);
    return response;
  }

  // Sometimes, simply being able to log in, is all the authorization you need...
  protected Response testAuthcPost(HttpRequest request) throws Exception {
    Response response = request.anon().post();
    assertResponseStatus(401, response);

    response = request.auth(authorized.getUsername(), authorized.getPassword()).post();
    assertResponseStatus(200, response);
    return response;
  }

  protected Response testAuthzDelete(HttpRequest request) throws Exception {
    Response response = request.auth(unauthorized.getUsername(), unauthorized.getPassword()).delete();
    assertResponseStatus(403, response);

    response = request.auth(authorized.getUsername(), authorized.getPassword()).delete();
    assertResponseStatus(204, response);

    return response;
  }
}
