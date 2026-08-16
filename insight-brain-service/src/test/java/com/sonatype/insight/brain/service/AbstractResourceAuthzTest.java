/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Provides boilerplate fixture for authorization tests.
 */
public abstract class AbstractResourceAuthzTest
    extends AbstractResourceTest
{
  protected Organization org;

  protected Application app;

  protected Repository repo;

  protected RepositoryManager repositoryManager;

  protected User unauthorized;

  protected User authorized;

  @Before
  @BeforeEach
  public void createEntities() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    repositoryManager = tempEntity.newRepositoryManager();
    repo = tempEntity.newRepository(repositoryManager, "test");
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

  private void assertStatus(HttpResponse response, Integer status) {
    if (status == null) {
      assertThat(response.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(400);
    }
    else {
      assertThat(response.getStatusCode()).isEqualTo(status);
    }
  }

  protected HttpResponse testAuthzGet(HttpRequest request) throws Exception {
    return testAuthzGet(request, null);
  }

  protected HttpResponse testAuthzGet(HttpRequest request, Integer expectedSuccessStatus) throws Exception {
    HttpResponse response = request.auth(unauthorized).get();
    assertStatus(response, 403);

    response = request.auth(authorized).get();
    assertStatus(response, expectedSuccessStatus);
    return response;
  }

  // Sometimes, simply being able to log in, is all the authorization you need...
  protected HttpResponse testAuthcGet(HttpRequest request) throws Exception {
    HttpResponse response = request.anon().get();
    assertStatus(response, 401);

    response = request.auth(authorized).get();
    assertStatus(response, null);
    return response;
  }

  protected HttpResponse testAuthzPut(HttpRequest request) throws Exception {
    return testAuthzPut(request, null);
  }

  protected HttpResponse testAuthzPut(HttpRequest request, Integer expectedSuccessStatus) throws Exception {
    HttpResponse response = request.auth(unauthorized).put();
    assertStatus(response, 403);

    response = request.auth(authorized).put();
    assertStatus(response, expectedSuccessStatus);
    return response;
  }

  protected HttpResponse testAuthzPost(HttpRequest request) throws Exception {
    return testAuthzPost(request, null);
  }

  protected HttpResponse testAuthzPost(HttpRequest request, Integer expectedSuccessStatus) throws Exception {
    HttpResponse response = request.auth(unauthorized).post();
    assertStatus(response, 403);

    response = request.auth(authorized).post();
    assertStatus(response, expectedSuccessStatus);
    return response;
  }

  // Sometimes, simply being able to log in, is all the authorization you need...
  protected HttpResponse testAuthcPost(HttpRequest request) throws Exception {
    HttpResponse response = request.anon().post();
    assertStatus(response, 401);

    response = request.auth(authorized).post();
    assertStatus(response, null);
    return response;
  }

  protected HttpResponse testAuthzDelete(HttpRequest request) throws Exception {
    HttpResponse response = request.auth(unauthorized).delete();
    assertStatus(response, 403);

    response = request.auth(authorized).delete();
    assertStatus(response, null);

    return response;
  }
}
