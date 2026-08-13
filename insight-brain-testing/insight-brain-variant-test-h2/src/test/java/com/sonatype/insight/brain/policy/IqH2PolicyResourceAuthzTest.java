/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.LinkedHashMap;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in the original {@code com.sonatype.insight.brain.policy} package because
 * {@link PolicyResource#NOTIFICATIONS_PATH} is package-private.
 */
@IqH2Test
class IqH2PolicyResourceAuthzTest
{
  private IqTestContext ctx;

  private Organization org;

  private Application app;

  private Repository repo;

  private RepositoryManager repositoryManager;

  private User unauthorized;

  private User authorized;

  private PolicyDAO policyDAO;

  @BeforeEach
  void createEntities() {
    org = ctx.tempEntity().newOrganization();
    app = ctx.tempEntity().newApplication(org.getId());
    repositoryManager = ctx.tempEntity().newRepositoryManager();
    repo = ctx.tempEntity().newRepository(repositoryManager, "test");
    unauthorized = ctx.tempEntity().newUser();
    authorized = ctx.tempEntity().newUser();
    policyDAO = ctx.lookup(PolicyDAO.class);
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
    return ctx.restRequest().anon().path(PolicyResource.RESOURCE_PATH);
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

  private HttpResponse testAuthzPost(HttpRequest request) throws Exception {
    return testAuthzPost(request, null);
  }

  private HttpResponse testAuthzPost(HttpRequest request, Integer expectedSuccessStatus) throws Exception {
    HttpResponse response = request.auth(unauthorized).post();
    assertStatus(response, 403);

    response = request.auth(authorized).post();
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

  private Policy newPolicy() {
    Policy policy = new Policy(null, "Policy " + TemporaryEntity.uuid());
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    return policy;
  }

  @Test
  void testGetPolicies_Org() throws Exception {
    grantReadPermission(org.getId());

    testAuthzGet(restRequest().parameter(OwnerType.ORGANIZATION, org.getId()));
  }

  @Test
  void testGetPolicies_Application() throws Exception {
    grantReadPermission(app.getId());

    testAuthzGet(restRequest().parameter(OwnerType.APPLICATION, app.getPublicId()));
  }

  @Test
  void testGetPolicies_RepoContainer() throws Exception {
    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    testAuthzGet(restRequest().parameter(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID));
  }

  @Test
  void testGetPolicies_RepoManager() throws Exception {
    grantReadPermission(repositoryManager.getId());

    testAuthzGet(restRequest().parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId()));
  }

  @Test
  void testGetPoliciesWithProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCode() throws Exception {
    HttpRequest request = restRequest().path(
        "withProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCode");

    testAuthzGet(request.parameter(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID), 403);

    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    testAuthzGet(request.parameter(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID));
  }

  @Test
  void testGetPolicies_Repo() throws Exception {
    grantReadPermission(repo.getId());

    testAuthzGet(restRequest().parameter(OwnerType.REPOSITORY, repo.getId()));
  }

  @Test
  void testGetApplicablePolicies_Org() throws Exception {
    HttpRequest request = restRequest().path("applicable");

    grantReadPermission(org.getId());

    testAuthzGet(request.parameter(OwnerType.ORGANIZATION, org.getId()));
  }

  @Test
  void testGetApplicablePolicies_Application() throws Exception {
    HttpRequest request = restRequest().path("applicable");

    grantReadPermission(app.getId());

    testAuthzGet(request.parameter(OwnerType.APPLICATION, app.getPublicId()));
  }

  @Test
  void testGetApplicablePolicies_RepoContainer() throws Exception {
    HttpRequest request = restRequest().path("applicable");

    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    testAuthzGet(request.parameter(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID));
  }

  @Test
  void testGetApplicablePolicies_RepoManager() throws Exception {
    HttpRequest request = restRequest().path("applicable");

    grantReadPermission(repositoryManager.getId());

    testAuthzGet(request.parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId()));
  }

  @Test
  void testGetApplicablePolicies_Repo() throws Exception {
    HttpRequest request = restRequest().path("applicable");

    grantReadPermission(repo.getId());

    testAuthzGet(request.parameter(OwnerType.REPOSITORY, repo.getId()));
  }

  @Test
  void testAddPolicy() throws Exception {
    grantWritePermission(app.getId());

    testAuthzPost(restRequest().body(newPolicy()).parameter(OwnerType.APPLICATION, app.getPublicId()));

    grantWritePermission(org.getId());

    testAuthzPost(restRequest().body(newPolicy()).parameter(OwnerType.ORGANIZATION, org.getId()));

    grantWritePermission(repo.getId());

    HttpResponse response =
        testAuthzPost(restRequest().body(newPolicy()).parameter(OwnerType.REPOSITORY, repo.getId()));
    ctx.assertResponseStatus(200, response);

    grantWritePermission(repositoryManager.getId());

    response = testAuthzPost(
        restRequest().body(newPolicy()).parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId()));
    ctx.assertResponseStatus(200, response);

    grantWritePermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    response = testAuthzPost(restRequest().body(newPolicy())
        .parameter(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID));
    ctx.assertResponseStatus(200, response);
  }

  @Test
  void testUpdatePolicy() throws Exception {
    grantWritePermission(app.getId());

    Policy policy = ctx.tempEntity().newPolicy(app);
    testAuthzPut(restRequest().body(policy).parameter(OwnerType.APPLICATION, app.getPublicId()));

    grantWritePermission(org.getId());

    policy = ctx.tempEntity().newPolicy(org);
    testAuthzPut(restRequest().body(policy).parameter(OwnerType.ORGANIZATION, org.getId()));

    grantWritePermission(repo.getId());

    policy = ctx.tempEntity().newPolicy(repo);
    testAuthzPut(restRequest().body(policy).parameter(OwnerType.REPOSITORY, repo.getId()));

    grantWritePermission(repositoryManager.getId());

    policy = ctx.tempEntity().newPolicy(repositoryManager);
    testAuthzPut(restRequest().body(policy).parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId()));

    grantWritePermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    policy = ctx.tempEntity().newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    testAuthzPut(restRequest().body(policy)
        .parameter(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID));
  }

  @Test
  void testUpdatePolicyNotifications() throws Exception {
    Policy policy = ctx.tempEntity().newPolicy(app);

    testAuthzPut(
        restRequest().path(PolicyResource.NOTIFICATIONS_PATH)
            .body(policy)
            .parameter(OwnerType.APPLICATION, app.getPublicId()),
        HttpStatus.SC_FORBIDDEN);

    grantWritePermission(app.getId());

    ctx.setMissingFeature(LicensedFeature.POLICY_READ_ONLY);
    HttpResponse response = restRequest().path(PolicyResource.NOTIFICATIONS_PATH)
        .auth(authorized)
        .body(policy)
        .parameter(OwnerType.APPLICATION, app.getPublicId())
        .put();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_PAYMENT_REQUIRED);
    ctx.setFeatures(LicensedFeature.POLICY_READ_ONLY);

    testAuthzPut(
        restRequest().path(PolicyResource.NOTIFICATIONS_PATH)
            .body(policy)
            .parameter(OwnerType.APPLICATION, app.getPublicId()));

    policy = ctx.tempEntity().newPolicy(org);

    testAuthzPut(
        restRequest().path(PolicyResource.NOTIFICATIONS_PATH)
            .body(policy)
            .parameter(OwnerType.ORGANIZATION,
                org.getId()),
        HttpStatus.SC_FORBIDDEN);
    grantWritePermission(org.getId());
    testAuthzPut(restRequest().path(PolicyResource.NOTIFICATIONS_PATH)
        .body(policy)
        .parameter(OwnerType.ORGANIZATION, org.getId()));

    policy = ctx.tempEntity().newPolicy(repo);

    testAuthzPut(
        restRequest().path(PolicyResource.NOTIFICATIONS_PATH)
            .body(policy)
            .parameter(OwnerType.REPOSITORY,
                repo.getId()),
        HttpStatus.SC_FORBIDDEN);
    grantWritePermission(repo.getId());
    testAuthzPut(restRequest().path(PolicyResource.NOTIFICATIONS_PATH)
        .body(policy)
        .parameter(OwnerType.REPOSITORY, repo.getId()));

    policy = ctx.tempEntity().newPolicy(repositoryManager);

    testAuthzPut(restRequest().path(PolicyResource.NOTIFICATIONS_PATH)
        .body(policy)
        .parameter(OwnerType.REPOSITORY_MANAGER,
            repositoryManager.getId()),
        HttpStatus.SC_FORBIDDEN);
    grantWritePermission(repositoryManager.getId());
    testAuthzPut(restRequest().path(PolicyResource.NOTIFICATIONS_PATH)
        .body(policy)
        .parameter(OwnerType.REPOSITORY_MANAGER,
            repositoryManager.getId()));

    policy = ctx.tempEntity().newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    testAuthzPut(restRequest().path(PolicyResource.NOTIFICATIONS_PATH)
        .body(policy)
        .parameter(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID),
        HttpStatus.SC_FORBIDDEN);
    grantWritePermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    testAuthzPut(restRequest().path(PolicyResource.NOTIFICATIONS_PATH)
        .body(policy)
        .parameter(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID));
  }

  @Test
  void testDeletePolicy() throws Exception {
    HttpRequest request = restRequest().path("{policyId}");

    grantWritePermission(app.getId());

    Policy policy = ctx.tempEntity().newPolicy(app);
    testAuthzDelete(request.parameter(OwnerType.APPLICATION, app.getPublicId(), policy.getId()));

    grantWritePermission(org.getId());

    policy = ctx.tempEntity().newPolicy(org);
    testAuthzDelete(request.parameter(OwnerType.ORGANIZATION, org.getId(), policy.getId()));

    grantWritePermission(repo.getId());

    policy = ctx.tempEntity().newPolicy(repo);
    testAuthzDelete(
        request.parameter(OwnerType.REPOSITORY, repo.getId(), policy.getId()));

    grantWritePermission(repositoryManager.getId());

    policy = ctx.tempEntity().newPolicy(repositoryManager);
    testAuthzDelete(
        request.parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), policy.getId()));

    grantWritePermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    policy = ctx.tempEntity().newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    testAuthzDelete(
        request.parameter(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID, policy.getId()));
  }

  @Test
  void testUpdateOverrides() throws Exception {
    HttpRequest request =
        restRequest().path("{policyId}/overrides").body(new PolicyOverridesDTO(new LinkedHashMap<>()));

    grantWritePermission(app.getId());

    Policy policy = ctx.tempEntity().newPolicy(app);
    policy.setPolicyActionsOverrideAllowed(true);
    policyDAO.update(policy);
    testAuthzPut(request.parameter(OwnerType.APPLICATION, app.getPublicId(), policy.getId()));

    grantWritePermission(org.getId());

    policy = ctx.tempEntity().newPolicy(org);
    policy.setPolicyActionsOverrideAllowed(true);
    policyDAO.update(policy);
    testAuthzPut(request.parameter(OwnerType.ORGANIZATION, org.getId(), policy.getId()));

    grantWritePermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    policy = ctx.tempEntity().newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    policy.setPolicyActionsOverrideAllowed(true);
    policyDAO.update(policy);
    testAuthzPut(
        request.parameter(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID, policy.getId()));
  }
}
