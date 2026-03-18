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
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.policy.PolicyResource.NOTIFICATIONS_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class PolicyResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  private PolicyDAO policyDAO;

  @Before
  public void setUp() {
    policyDAO = lookup(PolicyDAO.class);
  }

  private Policy newPolicy() {
    Policy policy = new Policy(null, "Policy " + TemporaryEntity.uuid());
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    return policy;
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PolicyResource.RESOURCE_PATH);
  }

  @Test
  public void testGetPolicies_Org() throws Exception {
    grantReadPermission(org.getId());

    testAuthzGet(restRequest().parameter(OwnerType.ORGANIZATION, org.getId()));
  }

  @Test
  public void testGetPolicies_Application() throws Exception {
    grantReadPermission(app.getId());

    testAuthzGet(restRequest().parameter(OwnerType.APPLICATION, app.getPublicId()));
  }

  @Test
  public void testGetPolicies_RepoContainer() throws Exception {
    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    testAuthzGet(restRequest().parameter(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID));
  }

  @Test
  public void testGetPolicies_RepoManager() throws Exception {
    grantReadPermission(repositoryManager.getId());

    testAuthzGet(restRequest().parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId()));
  }

  @Test
  public void testGetPoliciesWithProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCode() throws Exception {
    HttpRequest request = restRequest().path(
        "withProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCode");

    testAuthzGet(request.parameter(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID), 403);

    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    testAuthzGet(request.parameter(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID));
  }

  @Test
  public void testGetPolicies_Repo() throws Exception {
    grantReadPermission(repo.getId());

    testAuthzGet(restRequest().parameter(OwnerType.REPOSITORY, repo.getId()));
  }

  @Test
  public void testGetApplicablePolicies_Org() throws Exception {
    HttpRequest request = restRequest().path("applicable");

    grantReadPermission(org.getId());

    testAuthzGet(request.parameter(OwnerType.ORGANIZATION, org.getId()));
  }

  @Test
  public void testGetApplicablePolicies_Application() throws Exception {
    HttpRequest request = restRequest().path("applicable");

    grantReadPermission(app.getId());

    testAuthzGet(request.parameter(OwnerType.APPLICATION, app.getPublicId()));
  }

  @Test
  public void testGetApplicablePolicies_RepoContainer() throws Exception {
    HttpRequest request = restRequest().path("applicable");

    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    testAuthzGet(request.parameter(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID));
  }

  @Test
  public void testGetApplicablePolicies_RepoManager() throws Exception {
    HttpRequest request = restRequest().path("applicable");

    grantReadPermission(repositoryManager.getId());

    testAuthzGet(request.parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId()));
  }

  @Test
  public void testGetApplicablePolicies_Repo() throws Exception {
    HttpRequest request = restRequest().path("applicable");

    grantReadPermission(repo.getId());

    testAuthzGet(request.parameter(OwnerType.REPOSITORY, repo.getId()));
  }

  @Test
  public void testAddPolicy() throws Exception {
    grantWritePermission(app.getId());

    testAuthzPost(restRequest().body(newPolicy()).parameter(OwnerType.APPLICATION, app.getPublicId()));

    grantWritePermission(org.getId());

    testAuthzPost(restRequest().body(newPolicy()).parameter(OwnerType.ORGANIZATION, org.getId()));

    grantWritePermission(repo.getId());

    HttpResponse response =
        testAuthzPost(restRequest().body(newPolicy()).parameter(OwnerType.REPOSITORY, repo.getId()));
    assertResponseStatus(200, response);

    grantWritePermission(repositoryManager.getId());

    response = testAuthzPost(
        restRequest().body(newPolicy()).parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId()));
    assertResponseStatus(200, response);

    grantWritePermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    response = testAuthzPost(restRequest().body(newPolicy())
        .parameter(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID));
    assertResponseStatus(200, response);
  }

  @Test
  public void testUpdatePolicy() throws Exception {
    grantWritePermission(app.getId());

    Policy policy = tempEntity.newPolicy(app);
    testAuthzPut(restRequest().body(policy).parameter(OwnerType.APPLICATION, app.getPublicId()));

    grantWritePermission(org.getId());

    policy = tempEntity.newPolicy(org);
    testAuthzPut(restRequest().body(policy).parameter(OwnerType.ORGANIZATION, org.getId()));

    grantWritePermission(repo.getId());

    policy = tempEntity.newPolicy(repo);
    testAuthzPut(restRequest().body(policy).parameter(OwnerType.REPOSITORY, repo.getId()));

    grantWritePermission(repositoryManager.getId());

    policy = tempEntity.newPolicy(repositoryManager);
    testAuthzPut(restRequest().body(policy).parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId()));

    grantWritePermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    policy = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    testAuthzPut(restRequest().body(policy)
        .parameter(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID));
  }

  @Test
  public void testUpdatePolicyNotifications() throws Exception {
    Policy policy = tempEntity.newPolicy(app);

    setMissingFeature(LicensedFeature.POLICY_READ_ONLY);
    HttpResponse response = restRequest().path(NOTIFICATIONS_PATH)
        .auth(authorized)
        .body(policy)
        .parameter(OwnerType.APPLICATION, app.getPublicId())
        .put();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_PAYMENT_REQUIRED);
    setFeatures(LicensedFeature.POLICY_READ_ONLY);

    testAuthzPut(
        restRequest().path(NOTIFICATIONS_PATH).body(policy).parameter(OwnerType.APPLICATION, app.getPublicId()),
        HttpStatus.SC_FORBIDDEN);
    grantWritePermission(app.getId());
    testAuthzPut(
        restRequest().path(NOTIFICATIONS_PATH).body(policy).parameter(OwnerType.APPLICATION, app.getPublicId()));

    policy = tempEntity.newPolicy(org);

    testAuthzPut(restRequest().path(NOTIFICATIONS_PATH).body(policy).parameter(OwnerType.ORGANIZATION, org.getId()),
        HttpStatus.SC_FORBIDDEN);
    grantWritePermission(org.getId());
    testAuthzPut(restRequest().path(NOTIFICATIONS_PATH).body(policy).parameter(OwnerType.ORGANIZATION, org.getId()));

    policy = tempEntity.newPolicy(repo);

    testAuthzPut(restRequest().path(NOTIFICATIONS_PATH).body(policy).parameter(OwnerType.REPOSITORY, repo.getId()),
        HttpStatus.SC_FORBIDDEN);
    grantWritePermission(repo.getId());
    testAuthzPut(restRequest().path(NOTIFICATIONS_PATH).body(policy).parameter(OwnerType.REPOSITORY, repo.getId()));

    policy = tempEntity.newPolicy(repositoryManager);

    testAuthzPut(restRequest().path(NOTIFICATIONS_PATH)
        .body(policy)
        .parameter(OwnerType.REPOSITORY_MANAGER,
            repositoryManager.getId()),
        HttpStatus.SC_FORBIDDEN);
    grantWritePermission(repositoryManager.getId());
    testAuthzPut(restRequest().path(NOTIFICATIONS_PATH)
        .body(policy)
        .parameter(OwnerType.REPOSITORY_MANAGER,
            repositoryManager.getId()));

    policy = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    testAuthzPut(restRequest().path(NOTIFICATIONS_PATH)
        .body(policy)
        .parameter(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID),
        HttpStatus.SC_FORBIDDEN);
    grantWritePermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    testAuthzPut(restRequest().path(NOTIFICATIONS_PATH)
        .body(policy)
        .parameter(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID));
  }

  @Test
  public void testDeletePolicy() throws Exception {
    HttpRequest request = restRequest().path("{policyId}");

    grantWritePermission(app.getId());

    Policy policy = tempEntity.newPolicy(app);
    testAuthzDelete(request.parameter(OwnerType.APPLICATION, app.getPublicId(), policy.getId()));

    grantWritePermission(org.getId());

    policy = tempEntity.newPolicy(org);
    testAuthzDelete(request.parameter(OwnerType.ORGANIZATION, org.getId(), policy.getId()));

    grantWritePermission(repo.getId());

    policy = tempEntity.newPolicy(repo);
    testAuthzDelete(
        request.parameter(OwnerType.REPOSITORY, repo.getId(), policy.getId()));

    grantWritePermission(repositoryManager.getId());

    policy = tempEntity.newPolicy(repositoryManager);
    testAuthzDelete(
        request.parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), policy.getId()));

    grantWritePermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    policy = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    testAuthzDelete(
        request.parameter(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID, policy.getId()));
  }

  @Test
  public void testUpdateOverrides() throws Exception {
    HttpRequest request =
        restRequest().path("{policyId}/overrides").body(new PolicyOverridesDTO(new LinkedHashMap<>()));

    grantWritePermission(app.getId());

    Policy policy = tempEntity.newPolicy(app);
    policy.setPolicyActionsOverrideAllowed(true);
    policyDAO.update(policy);
    testAuthzPut(request.parameter(OwnerType.APPLICATION, app.getPublicId(), policy.getId()));

    grantWritePermission(org.getId());

    policy = tempEntity.newPolicy(org);
    policy.setPolicyActionsOverrideAllowed(true);
    policyDAO.update(policy);
    testAuthzPut(request.parameter(OwnerType.ORGANIZATION, org.getId(), policy.getId()));

    grantWritePermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    policy = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    policy.setPolicyActionsOverrideAllowed(true);
    policyDAO.update(policy);
    testAuthzPut(
        request.parameter(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID, policy.getId()));
  }
}
