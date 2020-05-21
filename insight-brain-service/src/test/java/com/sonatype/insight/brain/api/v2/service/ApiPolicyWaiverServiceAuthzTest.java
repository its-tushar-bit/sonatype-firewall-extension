/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.OwnerType.REPOSITORY_CONTAINER;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;

public class ApiPolicyWaiverServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiPolicyWaiverService apiPolicyWaiverService;

  private PolicyViolation policyViolation;

  @Before
  public void setUpPolicyViolation() {
    Policy policy = tempEntity.newPolicy(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");
  }

  @Test
  public void testAddPolicyWaiver_Application_Authorized() {
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.APPLICATION, "waiver comment");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddPolicyWaiver_Application_Unauthenticated() {
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.APPLICATION, "waiver comment");
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddPolicyWaiver_Application_UnauthorizedButAuthenticated() {
    login();
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.APPLICATION, "waiver comment");
  }

  @Test
  public void testAddPolicyWaiver_Organization_Authorized() {
    grantPermission(org.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.ORGANIZATION, "waiver comment");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddPolicyWaiver_Organization_Unauthenticated() {
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.ORGANIZATION, "waiver comment");
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddWaiver_Organization_UnauthorizedButAuthenticated() {
    login();
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.ORGANIZATION, "waiver comment");
  }

  @Test
  public void testDeletePolicyWaiver_Application_Authorized() {
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);

    PolicyWaiver waiver = tempEntity.newWaiver(tempEntity.newPolicy(app).getId(), app.getId());
    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.APPLICATION, app.getId(), waiver.getId());
  }

  @Test
  public void testDeletePolicyWaiver_Application_Authorized_PublicId() {
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);

    PolicyWaiver waiver = tempEntity.newWaiver(tempEntity.newPolicy(app).getId(), app.getId());
    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.APPLICATION, app.getPublicId(), waiver.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeletePolicyWaiver_Application_Unauthorized() {
    login();
    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.APPLICATION, app.getId(), "policy-waiver-id");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeletePolicyWaiver_Application__Unauthenticated() {
    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.APPLICATION, app.getId(), "policy-waiver-id");
  }

  @Test
  public void testDeletePolicyWaiver_Organization_Authorized() {
    grantPermission(org.getId(), Permission.WAIVE_POLICY_VIOLATIONS);

    PolicyWaiver waiver = tempEntity.newWaiver(tempEntity.newPolicy(org).getId(), org.getId());
    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.ORGANIZATION, org.getId(), waiver.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeletePolicyWaiver_Organization_Unauthorized() {
    login();

    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.ORGANIZATION, org.getId(), "policy-waiver-id");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeletePolicyWaiver_Organization__Unauthenticated() {
    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.ORGANIZATION, org.getId(), "policy-waiver-id");
  }

  @Test
  public void testDeletePolicyWaiver_Repository_Authorized() {
    grantPermission(repository.getId(), Permission.WAIVE_POLICY_VIOLATIONS);

    PolicyWaiver waiver = tempEntity.newWaiver(tempEntity.newPolicy().getId(), repository.getId());
    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.REPOSITORY, repository.getId(), waiver.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeletePolicyWaiver_Repository_Unauthorized() {
    login();

    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.REPOSITORY, repository.getId(), "policy-waiver-id");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeletePolicyWaiver_Repository_Unauthenticated() {
    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.REPOSITORY, repository.getId(), "policy-waiver-id");
  }

  @Test
  public void testDeletePolicyWaiver_RepositoryContainer_Authorized() {
    grantPermission(REPOSITORY_CONTAINER_ID, Permission.WAIVE_POLICY_VIOLATIONS);

    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), REPOSITORY_CONTAINER_ID);

    apiPolicyWaiverService.deletePolicyWaiver(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, policyWaiver.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeletePolicyWaiver_RepositoryContainer_Unauthorized() {
    login();
    apiPolicyWaiverService.deletePolicyWaiver(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, "policy-waiver-id");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeletePolicyWaiver_RepositoryContainer_Unauthenticated() {
    apiPolicyWaiverService.deletePolicyWaiver(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, "policy-waiver-id");
  }
}
