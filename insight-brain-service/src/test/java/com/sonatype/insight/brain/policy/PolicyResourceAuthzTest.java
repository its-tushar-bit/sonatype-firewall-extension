/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.LinkedHashMap;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

public class PolicyResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  private Policy newPolicy() {
    Policy policy = new Policy(null, "Policy " + tempEntity.uuid());
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
  public void testGetPolicies() throws Exception {
    grantReadPermission(app.getId());

    testAuthzGet(restRequest().parameter(OwnerType.APPLICATION, app.getPublicId()));

    grantReadPermission(org.getId());

    testAuthzGet(restRequest().parameter(OwnerType.ORGANIZATION, org.getId()));

    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    testAuthzGet(restRequest().parameter(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID));
  }

  @Test
  public void testGetApplicablePolicies() throws Exception {
    HttpRequest request = restRequest().path("applicable");

    grantReadPermission(app.getId());

    testAuthzGet(request.parameter(OwnerType.APPLICATION, app.getPublicId()));

    grantReadPermission(org.getId());

    testAuthzGet(request.parameter(OwnerType.ORGANIZATION, org.getId()));

    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    testAuthzGet(request.parameter(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID));
  }

  @Test
  public void testAddPolicy() throws Exception {
    grantWritePermission(app.getId());

    testAuthzPost(restRequest().body(newPolicy()).parameter(OwnerType.APPLICATION, app.getPublicId()));

    grantWritePermission(org.getId());

    testAuthzPost(restRequest().body(newPolicy()).parameter(OwnerType.ORGANIZATION, org.getId()));

    grantWritePermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    HttpResponse response = testAuthzPost(restRequest().body(newPolicy()).parameter(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID));
    assertResponseStatus(200, response);
    Policy policy = response.getBody(Policy.class);
    tempEntity.register(policy);
  }

  @Test
  public void testUpdatePolicy() throws Exception {
    grantWritePermission(app.getId());

    Policy policy = tempEntity.newPolicy(app);
    testAuthzPut(restRequest().body(policy).parameter(OwnerType.APPLICATION, app.getPublicId()));

    grantWritePermission(org.getId());

    policy = tempEntity.newPolicy(org);
    testAuthzPut(restRequest().body(policy).parameter(OwnerType.ORGANIZATION, org.getId()));

    grantWritePermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    policy = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    testAuthzPut(restRequest().body(policy).parameter(OwnerType.REPOSITORY_CONTAINER,
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
    new PolicyDAO().update(policy);
    testAuthzPut(request.parameter(OwnerType.APPLICATION, app.getPublicId(), policy.getId()));

    grantWritePermission(org.getId());

    policy = tempEntity.newPolicy(org);
    policy.setPolicyActionsOverrideAllowed(true);
    new PolicyDAO().update(policy);
    testAuthzPut(request.parameter(OwnerType.ORGANIZATION, org.getId(), policy.getId()));

    grantWritePermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    policy = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    policy.setPolicyActionsOverrideAllowed(true);
    new PolicyDAO().update(policy);
    testAuthzPut(
        request.parameter(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID, policy.getId()));
  }
}
