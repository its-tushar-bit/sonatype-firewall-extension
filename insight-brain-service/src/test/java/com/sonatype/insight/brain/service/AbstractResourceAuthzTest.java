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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

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

  private void assertStatus(Response response, Integer status) {
    if (status == null) {
      assertThat(response.getStatusCode(), is(allOf(greaterThanOrEqualTo(200), lessThan(400))));
    }
    else {
      assertThat(response.getStatusCode(), is(status));
    }
  }

  protected Response testAuthzGet(HttpRequest request) throws Exception {
    return testAuthzGet(request, null);
  }

  protected Response testAuthzGet(HttpRequest request, Integer expectedSuccessStatus) throws Exception {
    Response response = request.auth(unauthorized.getUsername(), unauthorized.getPassword()).get();
    assertStatus(response, 403);

    response = request.auth(authorized.getUsername(), authorized.getPassword()).get();
    assertStatus(response, expectedSuccessStatus);
    return response;
  }

  // Sometimes, simply being able to log in, is all the authorization you need...
  protected Response testAuthcGet(HttpRequest request) throws Exception {
    Response response = request.anon().get();
    assertStatus(response, 401);

    response = request.auth(authorized.getUsername(), authorized.getPassword()).get();
    assertStatus(response, null);
    return response;
  }

  protected Response testAuthzPut(HttpRequest request) throws Exception {
    return testAuthzPut(request, null);
  }

  protected Response testAuthzPut(HttpRequest request, Integer expectedSuccessStatus) throws Exception {
    Response response = request.auth(unauthorized.getUsername(), unauthorized.getPassword()).put();
    assertStatus(response, 403);

    response = request.auth(authorized.getUsername(), authorized.getPassword()).put();
    assertStatus(response, expectedSuccessStatus);
    return response;
  }

  protected Response testAuthzPost(HttpRequest request) throws Exception {
    return testAuthzPost(request, null);
  }

  protected Response testAuthzPost(HttpRequest request, Integer expectedSuccessStatus) throws Exception {
    Response response = request.auth(unauthorized.getUsername(), unauthorized.getPassword()).post();
    assertStatus(response, 403);

    response = request.auth(authorized.getUsername(), authorized.getPassword()).post();
    assertStatus(response, expectedSuccessStatus);
    return response;
  }

  // Sometimes, simply being able to log in, is all the authorization you need...
  protected Response testAuthcPost(HttpRequest request) throws Exception {
    Response response = request.anon().post();
    assertStatus(response, 401);

    response = request.auth(authorized.getUsername(), authorized.getPassword()).post();
    assertStatus(response, null);
    return response;
  }

  protected Response testAuthzDelete(HttpRequest request) throws Exception {
    Response response = request.auth(unauthorized.getUsername(), unauthorized.getPassword()).delete();
    assertStatus(response, 403);

    response = request.auth(authorized.getUsername(), authorized.getPassword()).delete();
    assertStatus(response, null);

    return response;
  }
}
