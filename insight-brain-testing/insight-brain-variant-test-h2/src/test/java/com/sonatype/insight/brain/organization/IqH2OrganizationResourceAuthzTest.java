/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in the {@code com.sonatype.insight.brain.organization} package; reproduces the
 * {@code AbstractResourceAuthzTest} fixture (org/app/repo + authorized/unauthorized users) and its
 * {@code testAuthzGet}/{@code testAuthcGet}/{@code testAuthzPost} helpers that the legacy
 * {@code OrganizationResourceAuthzTest} inherited from its base class.
 */
@IqH2Test
class IqH2OrganizationResourceAuthzTest
{
  private IqTestContext ctx;

  private Organization org;

  private Application app;

  private Repository repo;

  private RepositoryManager repositoryManager;

  private User unauthorized;

  private User authorized;

  @BeforeEach
  void createEntities() {
    org = ctx.tempEntity().newOrganization();
    app = ctx.tempEntity().newApplication(org.getId());
    repositoryManager = ctx.tempEntity().newRepositoryManager();
    repo = ctx.tempEntity().newRepository(repositoryManager, "test");
    unauthorized = ctx.tempEntity().newUser();
    authorized = ctx.tempEntity().newUser();
  }

  private void grantWritePermission(String contextId) {
    grantPermission(contextId, Permission.WRITE);
  }

  private void grantReadPermission(String contextId) {
    grantPermission(contextId, Permission.READ);
  }

  private void grantPermission(String contextId, Permission permission) {
    Role role = ctx.tempEntity().newRole(false /* global */, permission);
    ctx.tempEntity().newMembershipMapping(contextId, role.getId(), authorized.getUsername());
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().anon().path(OrganizationResource.RESOURCE_PATH);
  }

  private void assertStatus(HttpResponse response, Integer status) {
    if (status == null) {
      assertThat(response.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(400);
    }
    else {
      assertThat(response.getStatusCode()).isEqualTo(status);
    }
  }

  private HttpResponse testAuthzGet(HttpRequest request) throws Exception {
    return testAuthzGet(request, null);
  }

  private HttpResponse testAuthzGet(HttpRequest request, Integer expectedSuccessStatus) throws Exception {
    HttpResponse response = request.auth(unauthorized).get();
    assertStatus(response, 403);

    response = request.auth(authorized).get();
    assertStatus(response, expectedSuccessStatus);
    return response;
  }

  // Sometimes, simply being able to log in, is all the authorization you need...
  private HttpResponse testAuthcGet(HttpRequest request) throws Exception {
    HttpResponse response = request.anon().get();
    assertStatus(response, 401);

    response = request.auth(authorized).get();
    assertStatus(response, null);
    return response;
  }

  private HttpResponse testAuthzPost(HttpRequest request) throws Exception {
    HttpResponse response = request.auth(unauthorized).post();
    assertStatus(response, 403);

    response = request.auth(authorized).post();
    assertStatus(response, null);
    return response;
  }

  @Test
  void testGenerateIcon() throws Exception {
    HttpRequest request = restRequest().path(OrganizationResource.GENERATE_ICON_PATH).parameter("hash");
    testAuthcGet(request);
  }

  @Test
  void testGetIcon() throws Exception {
    grantReadPermission(org.getId());

    HttpRequest request = restRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH).parameter(org.getId());
    testAuthzGet(request);
  }

  @Test
  void testSetIcon() throws Exception {
    grantWritePermission(org.getId());

    HttpRequest request = restRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH)
        .parameter(org.getId())
        .part("hasRobotSource", "false");
    testAuthzPost(request);
  }
}
