/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestOptionsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestReviewDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.PolicyWaiverRequestBuilder;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.DEFAULT;

public class ApiPolicyWaiverRequestServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiPolicyWaiverRequestService apiPolicyWaiverRequestService;

  private PolicyViolation createApplicationPolicyViolation() {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    return tempEntity.newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");
  }

  private void addPolicyWaiverRequestWithDefaultOptions(OwnerType ownerType, String ownerId, String violationId) {
    apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(ownerType, ownerId, violationId,
        new ApiPolicyWaiverRequestOptionsDTO(null, DEFAULT, null, null, false));
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_Authorized() {
    grantPermission(app.getId(), Permission.READ);
    addPolicyWaiverRequestWithDefaultOptions(OwnerType.APPLICATION, app.getId(),
        createApplicationPolicyViolation().getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddPolicyWaiverRequestByPolicyViolationId_Unauthenticated() {
    addPolicyWaiverRequestWithDefaultOptions(OwnerType.APPLICATION, app.getId(), null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddPolicyWaiverRequestByPolicyViolationId_Unauthorized() {
    login();
    addPolicyWaiverRequestWithDefaultOptions(OwnerType.APPLICATION, app.getId(), null);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_AppPublicId_Authorized() {
    grantPermission(app.getId(), Permission.READ);
    addPolicyWaiverRequestWithDefaultOptions(OwnerType.APPLICATION, app.getPublicId(),
        createApplicationPolicyViolation().getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddPolicyWaiverRequestByPolicyViolationId_AppPublicId_Unauthenticated() {
    addPolicyWaiverRequestWithDefaultOptions(OwnerType.APPLICATION, app.getPublicId(), null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddPolicyWaiverRequestByPolicyViolationId_AppPublicId_Unauthorized() {
    login();
    addPolicyWaiverRequestWithDefaultOptions(OwnerType.APPLICATION, app.getPublicId(), null);
  }

  private void reviewPolicyWaiverRequestWithDefaultOptions(OwnerType ownerType, String ownerId) {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    PolicyWaiverRequest policyWaiverRequest = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequestBuilder()
        .setPolicyId(policy.getId()).setOwnerId(ownerId).setPolicyViolationId("policyViolationId").build());
    apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(ownerType, ownerId, policyWaiverRequest.getId(),
        new ApiPolicyWaiverRequestReviewDTO(null, DEFAULT, null, null, false,
            PolicyWaiverRequestStatus.REJECTED.name()));
  }

  @Test
  public void testReviewPolicyWaiverRequest_Authorized() {
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    reviewPolicyWaiverRequestWithDefaultOptions(OwnerType.APPLICATION, app.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testReviewPolicyWaiverRequest_Unauthenticated() {
    reviewPolicyWaiverRequestWithDefaultOptions(OwnerType.APPLICATION, app.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testReviewPolicyWaiverRequest_Unauthorized() {
    login();
    reviewPolicyWaiverRequestWithDefaultOptions(OwnerType.APPLICATION, app.getId());
  }

  @Test
  public void testReviewPolicyWaiverRequest_AppPublicId_Authorized() {
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    PolicyWaiverRequest policyWaiverRequest = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequestBuilder()
        .setPolicyId(policy.getId()).setOwnerId(app.getId()).setPolicyViolationId("policyViolationId").build());
    apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.APPLICATION, app.getPublicId(),
        policyWaiverRequest.getId(), new ApiPolicyWaiverRequestReviewDTO(null, DEFAULT, null, null, false,
            PolicyWaiverRequestStatus.REJECTED.name()));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testReviewPolicyWaiverRequest_AppPublicId_Unauthenticated() {
    reviewPolicyWaiverRequestWithDefaultOptions(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testReviewPolicyWaiverRequest_AppPublicId_Unauthorized() {
    login();
    reviewPolicyWaiverRequestWithDefaultOptions(OwnerType.APPLICATION, app.getPublicId());
  }
}
