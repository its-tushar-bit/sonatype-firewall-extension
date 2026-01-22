/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestOptionsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestReviewDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestsApplicableToViolationDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.DEFAULT;
import static org.assertj.core.api.Assertions.assertThat;

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

  private void updatePolicyWaiverRequestWithDefaultOptions(
      OwnerType ownerType,
      String ownerId,
      String policyWaiverRequestId)
  {
    apiPolicyWaiverRequestService.updatePolicyWaiverRequest(ownerType, ownerId, policyWaiverRequestId,
        new ApiPolicyWaiverRequestOptionsDTO(null, DEFAULT, null, null, false));
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_Authorized() {
    // Posting a RequestWaiverReviewEvent requires a base URL to be set
    // For this test there is no running IQ instance so we need to set a dummy URL
    setBaseUrl("http://localhost:1234");
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
    // Posting a RequestWaiverReviewEvent requires a base URL to be set
    // For this test there is no running IQ instance so we need to set a dummy URL
    setBaseUrl("http://localhost:1234");
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
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyWaiverRequest policyWaiverRequest = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setPolicyId(policy.getId()).setOwnerId(ownerId).setPolicyViolationId(policyViolation.getId()));
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
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyWaiverRequest policyWaiverRequest = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setPolicyId(policy.getId()).setOwnerId(app.getId()).setPolicyViolationId(policyViolation.getId()));
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

  @Test
  public void testGetPolicyWaiverRequest_Authorized() {
    grantPermission(app.getId(), Permission.READ);
    getPolicyWaiverRequest(OwnerType.APPLICATION, app.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyWaiverRequest_Unauthenticated() {
    getPolicyWaiverRequest(OwnerType.APPLICATION, app.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyWaiverRequest_Unauthorized() {
    login();
    getPolicyWaiverRequest(OwnerType.APPLICATION, app.getId());
  }

  @Test
  public void testGetPolicyWaiverRequest_AppPublicId_Authorized() {
    grantPermission(app.getId(), Permission.READ);
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    PolicyWaiverRequest policyWaiverRequest = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setPolicyId(policy.getId()).setOwnerId(app.getId()).setPolicyViolationId("policyViolationId"));
    apiPolicyWaiverRequestService.getPolicyWaiverRequest(OwnerType.APPLICATION, app.getPublicId(),
        policyWaiverRequest.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyWaiverRequest_AppPublicId_Unauthenticated() {
    getPolicyWaiverRequest(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyWaiverRequest_AppPublicId_Unauthorized() {
    login();
    getPolicyWaiverRequest(OwnerType.APPLICATION, app.getPublicId());
  }

  private void getPolicyWaiverRequest(OwnerType ownerType, String ownerId) {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    PolicyWaiverRequest policyWaiverRequest = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setPolicyId(policy.getId()).setOwnerId(ownerId).setPolicyViolationId("policyViolationId"));
    apiPolicyWaiverRequestService.getPolicyWaiverRequest(ownerType, ownerId, policyWaiverRequest.getId());
  }

  private ApiPolicyWaiverRequestsApplicableToViolationDTO getApplicableWaiverRequests(
      OwnerType ownerType,
      String ownerId)
  {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    AbstractPolicyViolation policyViolation;
    if (OwnerType.APPLICATION == ownerType || OwnerType.ORGANIZATION == ownerType) {
      PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId");
      policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    }
    else {
      policyViolation =
          tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());
    }
    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setPolicyId(policy.getId()).setOwnerId(ownerId)
        .setHash(policyViolation.getHash()).setConstraintFacts(policyViolation.getConstraintFacts())
        .setComponentMatchStrategy(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT)
        .setPolicyViolationId(policyViolation.getId()));
    return apiPolicyWaiverRequestService.getApplicableWaiverRequests(policyViolation.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableWaiverRequests_Organization_Unauthenticated() {
    getApplicableWaiverRequests(OwnerType.ORGANIZATION, org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableWaiverRequests_Organization_Unauthorized() {
    login();
    assertThat(getApplicableWaiverRequests(OwnerType.ORGANIZATION, org.getId()).activeWaiverRequests).isEmpty();
  }

  @Test
  public void testGetApplicableWaiverRequests_Organization_Authorized() {
    grantPermission(org.getId(), Permission.READ);
    assertThat(getApplicableWaiverRequests(OwnerType.ORGANIZATION, org.getId()).activeWaiverRequests).hasSize(1);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableWaiverRequests_Application_Unauthenticated() {
    getApplicableWaiverRequests(OwnerType.APPLICATION, app.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableWaiverRequests_Application_Unauthorized() {
    login();
    assertThat(getApplicableWaiverRequests(OwnerType.APPLICATION, app.getId()).activeWaiverRequests).isEmpty();
  }

  @Test
  public void testGetApplicableWaiverRequests_Application_Authorized() {
    grantPermission(app.getId(), Permission.READ);
    assertThat(getApplicableWaiverRequests(OwnerType.APPLICATION, app.getId()).activeWaiverRequests).hasSize(1);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableWaiverRequests_Repository_Unauthenticated() {
    getApplicableWaiverRequests(OwnerType.REPOSITORY, repository.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableWaiverRequests_Repository_Unauthorized() {
    login();
    assertThat(getApplicableWaiverRequests(OwnerType.REPOSITORY, repository.getId()).activeWaiverRequests).isEmpty();
  }

  @Test
  public void testGetApplicableWaiverRequests_Repository_Authorized() {
    grantPermission(repository.getId(), Permission.READ);
    assertThat(getApplicableWaiverRequests(OwnerType.REPOSITORY, repository.getId()).activeWaiverRequests).hasSize(1);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableWaiverRequests_RepositoryManager_Unauthenticated() {
    getApplicableWaiverRequests(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableWaiverRequests_RepositoryManager_Unauthorized() {
    login();
    assertThat(
        getApplicableWaiverRequests(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId()).activeWaiverRequests)
            .isEmpty();
  }

  @Test
  public void testGetApplicableWaiverRequests_RepositoryManager_Authorized() {
    grantPermission(repositoryManager.getId(), Permission.READ);
    assertThat(
        getApplicableWaiverRequests(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId()).activeWaiverRequests)
            .hasSize(1);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableWaiverRequests_RepositoryContainer_Unauthenticated() {
    getApplicableWaiverRequests(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableWaiverRequests_RepositoryContainer_Unauthorized() {
    login();
    assertThat(getApplicableWaiverRequests(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID).activeWaiverRequests).isEmpty();
  }

  @Test
  public void testGetApplicableWaiverRequests_RepositoryContainer_Authorized() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.READ);
    assertThat(getApplicableWaiverRequests(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID).activeWaiverRequests).hasSize(1);
  }

  @Test
  public void testUpdatePolicyWaiverRequest_Authorized() {
    PolicyViolation policyViolation = createApplicationPolicyViolation();
    PolicyWaiverRequest policyWaiverRequest = tempEntity.newPolicyWaiverRequest(
        new PolicyWaiverRequest().setPolicyId(tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID).getId())
            .setOwnerId(app.getId()).setPolicyViolationId(policyViolation.getId()));

    grantPermission(app.getId(), Permission.READ);
    updatePolicyWaiverRequestWithDefaultOptions(OwnerType.APPLICATION, app.getId(), policyWaiverRequest.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdatePolicyWaiverRequest_Unauthenticated() {
    updatePolicyWaiverRequestWithDefaultOptions(OwnerType.APPLICATION, app.getId(), null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdatePolicyWaiverRequest_Unauthorized() {
    login();
    updatePolicyWaiverRequestWithDefaultOptions(OwnerType.APPLICATION, app.getId(), null);
  }

  @Test
  public void testUpdatePolicyWaiverRequest_AppPublicId_Authorized() {
    PolicyViolation policyViolation = createApplicationPolicyViolation();
    PolicyWaiverRequest policyWaiverRequest = tempEntity.newPolicyWaiverRequest(
        new PolicyWaiverRequest().setPolicyId(tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID).getId())
            .setOwnerId(app.getId()).setPolicyViolationId(policyViolation.getId()));

    grantPermission(app.getId(), Permission.READ);
    updatePolicyWaiverRequestWithDefaultOptions(OwnerType.APPLICATION, app.getPublicId(), policyWaiverRequest.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdatePolicyWaiverRequest_AppPublicId_Unauthenticated() {
    updatePolicyWaiverRequestWithDefaultOptions(OwnerType.APPLICATION, app.getPublicId(), null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdatePolicyWaiverRequest_AppPublicId_Unauthorized() {
    login();
    updatePolicyWaiverRequestWithDefaultOptions(OwnerType.APPLICATION, app.getPublicId(), null);
  }
}
