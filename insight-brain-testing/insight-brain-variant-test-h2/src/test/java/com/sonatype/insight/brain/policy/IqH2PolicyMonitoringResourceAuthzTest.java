/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in the {@code com.sonatype.insight.brain.policy} package; reproduces the {@code AbstractResourceAuthzTest}
 * fixture (org/app/repo + authorized/unauthorized users) and its {@code testAuthzGet}/{@code testAuthzPut}/
 * {@code testAuthzDelete} helpers that the legacy {@code PolicyMonitoringResourceAuthzTest} inherited from its base
 * class, driving the reused {@code TestCLMServer} through the injected {@link IqTestContext}.
 */
@IqH2Test
class IqH2PolicyMonitoringResourceAuthzTest
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
    return ctx.restRequest().anon().path(PolicyMonitoringResource.RESOURCE_PATH);
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

  private HttpResponse testAuthzPut(HttpRequest request) throws Exception {
    return testAuthzPut(request, null);
  }

  private HttpResponse testAuthzPut(HttpRequest request, Integer expectedSuccessStatus) throws Exception {
    HttpResponse response = request.auth(unauthorized).put();
    assertStatus(response, 403);

    response = request.auth(authorized).put();
    assertStatus(response, expectedSuccessStatus);
    return response;
  }

  private HttpResponse testAuthzDelete(HttpRequest request) throws Exception {
    HttpResponse response = request.auth(unauthorized).delete();
    assertStatus(response, 403);

    response = request.auth(authorized).delete();
    assertStatus(response, null);

    return response;
  }

  @Test
  void testSet() throws Exception {
    HttpRequest request = restRequest().body(new PolicyMonitoring(null /* ownerId */, Stage.ID_RELEASE));

    grantWritePermission(app.getId());
    testAuthzPut(request.parameter(OwnerType.APPLICATION, app.getPublicId()));

    grantWritePermission(org.getId());
    testAuthzPut(request.parameter(OwnerType.ORGANIZATION, org.getId()));
  }

  @Test
  void testDelete() throws Exception {
    grantWritePermission(app.getId());
    PolicyMonitoring policyMonitoring = createPolicyMonitoring(app.getId());
    testAuthzDelete(restRequest().parameter(OwnerType.APPLICATION, app.getPublicId())
        .query("stageTypeId", policyMonitoring.getStageTypeId()));

    grantWritePermission(org.getId());
    policyMonitoring = createPolicyMonitoring(org.getId());
    testAuthzDelete(restRequest().parameter(OwnerType.ORGANIZATION, org.getId())
        .query("stageTypeId", policyMonitoring.getStageTypeId()));
  }

  @Test
  void testGet() throws Exception {
    grantReadPermission(app.getId());
    createPolicyMonitoring(app.getId());
    testAuthzGet(restRequest().parameter(OwnerType.APPLICATION, app.getPublicId()));

    grantReadPermission(org.getId());
    createPolicyMonitoring(org.getId());
    testAuthzGet(restRequest().parameter(OwnerType.ORGANIZATION, org.getId()));
  }

  @Test
  void testGetApplicable() throws Exception {
    HttpRequest request = restRequest().path("applicable");

    grantReadPermission(app.getId());
    createPolicyMonitoring(app.getId());
    testAuthzGet(request.parameter(OwnerType.APPLICATION, app.getPublicId()));

    grantReadPermission(org.getId());
    createPolicyMonitoring(org.getId());
    testAuthzGet(request.parameter(OwnerType.ORGANIZATION, org.getId()));
  }

  private PolicyMonitoring createPolicyMonitoring(String ownerid) {
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(ownerid, Stage.ID_RELEASE);
    return ctx.tempEntity().newPolicyMonitoring(policyMonitoring);
  }
}
