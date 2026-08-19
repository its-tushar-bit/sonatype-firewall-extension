/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.ide.IdeResource;
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
 * Reproduces the {@code AbstractResourceAuthzTest} scaffolding (org/app/repo/unauthorized/authorized
 * fixtures + testAuthzGet/testAuthzPost helpers) because {@code IdeResourceAuthzTest} no longer inherits
 * it under the {@code @IqH2Test} composition pattern.
 */
@IqH2Test
class IqH2IdeResourceAuthzTest
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

  private void grantPermission(String contextId, Permission permission) {
    Role role = ctx.tempEntity().newRole(false /* global */, permission);
    ctx.tempEntity().newMembershipMapping(contextId, role.getId(), authorized.getUsername());
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().anon().path(IdeResource.RESOURCE_PATH);
  }

  private void assertStatus(com.sonatype.insight.brain.HttpResponse response, Integer status) {
    if (status == null) {
      assertThat(response.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(400);
    }
    else {
      assertThat(response.getStatusCode()).isEqualTo(status);
    }
  }

  private com.sonatype.insight.brain.HttpResponse testAuthzGet(HttpRequest request) throws Exception {
    com.sonatype.insight.brain.HttpResponse response = request.auth(unauthorized).get();
    assertStatus(response, 403);

    response = request.auth(authorized).get();
    assertStatus(response, null);
    return response;
  }

  private com.sonatype.insight.brain.HttpResponse testAuthzPost(HttpRequest request) throws Exception {
    com.sonatype.insight.brain.HttpResponse response = request.auth(unauthorized).post();
    assertStatus(response, 403);

    response = request.auth(authorized).post();
    assertStatus(response, null);
    return response;
  }

  @Test
  void testDoScan() throws Exception {
    String hash = "0123456789";
    ctx.hdsRespondWith("{}").atUri("rest/ide/scan/simple/" + hash);
    grantPermission(app.getId(), Permission.EVALUATE_COMPONENT);

    HttpRequest request = restRequest().path("scan/simple/{appPublicId}/{hash}").parameter(app.getPublicId(), hash);
    testAuthzGet(request);
  }

  @Test
  void testPostScan() throws Exception {
    String hash = "0123456789";
    ctx.hdsRespondWith("{}").atUri("rest/ide/scan/enhanced/" + hash);
    grantPermission(app.getId(), Permission.EVALUATE_COMPONENT);

    HttpRequest request = restRequest().path("scan/enhanced/{appPublicId}/{hash}")
        .parameter(app.getPublicId(), hash)
        .body("{}");
    testAuthzPost(request);
  }

  @Test
  void testDoCoordinatesScan() throws Exception {
    ctx.hdsRespondWith("[]").atUri("rest/ide/scan/coordinates");
    grantPermission(app.getId(), Permission.EVALUATE_COMPONENT);

    HttpRequest request = restRequest().path(IdeResource.COORDINATES_SCAN_PATH).parameter(app.getPublicId());
    testAuthzGet(request);
  }
}
