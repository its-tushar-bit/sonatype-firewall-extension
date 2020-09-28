/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Organization;
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

  private String setUpParameterizePolicyViolation(String ownerId) {
    Policy policy = tempEntity.newPolicy(ownerId);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation violation = tempEntity.newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");
    return violation.getId();
  }

  @Before
  public void setUpPolicyViolation() {
    Policy policy = tempEntity.newPolicy(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");
  }

  /**
   * @deprecated The tested method is deprecated
   */
  @Deprecated
  @Test
  public void testAddPolicyWaiver_Application_Authorized() {
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.APPLICATION, "waiver comment");
  }

  /**
   * @deprecated The tested method is deprecated
   */
  @Deprecated
  @Test(expected = UnauthenticatedException.class)
  public void testAddPolicyWaiver_Application_Unauthenticated() {
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.APPLICATION, "waiver comment");
  }

  /**
   * @deprecated The tested method is deprecated
   */
  @Deprecated
  @Test(expected = UnauthorizedException.class)
  public void testAddPolicyWaiver_Application_UnauthorizedButAuthenticated() {
    login();
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.APPLICATION, "waiver comment");
  }

  /**
   * @deprecated The tested method is deprecated
   */
  @Deprecated
  @Test
  public void testAddPolicyWaiver_Organization_Authorized() {
    grantPermission(org.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.ORGANIZATION, "waiver comment");
  }

  /**
   * @deprecated The tested method is deprecated
   */
  @Deprecated
  @Test(expected = UnauthenticatedException.class)
  public void testAddPolicyWaiver_Organization_Unauthenticated() {
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.ORGANIZATION, "waiver comment");
  }

  /**
   * @deprecated The tested method is deprecated
   */
  @Deprecated
  @Test(expected = UnauthorizedException.class)
  public void testAddWaiver_Organization_UnauthorizedButAuthenticated() {
    login();
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.ORGANIZATION, "waiver comment");
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Application_Authorized() {
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    addPolicyWaiverWithDefaultOptions(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddPolicyWaiverByPolicyViolationId_Application_Unauthenticated() {
    addPolicyWaiverWithDefaultOptions(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddPolicyWaiverByPolicyViolationId_Application_UnauthorizedButAuthenticated() {
    login();
    addPolicyWaiverWithDefaultOptions(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Organization_Authorized() {
    grantPermission(org.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    addPolicyWaiverWithDefaultOptions(OwnerType.ORGANIZATION, org.getId(),
        policyViolation.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddPolicyWaiverByPolicyViolationId_Organization_Unauthenticated() {
    addPolicyWaiverWithDefaultOptions(OwnerType.ORGANIZATION, org.getId(),
        policyViolation.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddPolicyWaiverByPolicyViolationId_Organization_UnauthorizedButAuthenticated() {
    login();
    addPolicyWaiverWithDefaultOptions(OwnerType.ORGANIZATION, org.getId(),
        policyViolation.getId());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_RootOrganization_Authorized() {
    grantPermission(Organization.ROOT_ORGANIZATION_ID, Permission.WAIVE_POLICY_VIOLATIONS);
    addPolicyWaiverWithDefaultOptions(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        policyViolation.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddPolicyWaiverByPolicyViolationId_RootOrganization_Unauthenticated() {
    addPolicyWaiverWithDefaultOptions(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        policyViolation.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddPolicyWaiverByPolicyViolationId_RootOrganization_UnauthorizedButAuthenticated() {
    login();
    addPolicyWaiverWithDefaultOptions(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        policyViolation.getId());
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

  @Test
  public void testGetPolicyWaivers_Application_Authorized() {
    grantPermission(app.getId(), Permission.READ);
    apiPolicyWaiverService.getPolicyWaivers(OwnerType.APPLICATION, app.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyWaivers_Application_Unauthorized() {
    login();
    apiPolicyWaiverService.getPolicyWaivers(OwnerType.APPLICATION, app.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyWaivers_Application_Unauthenticated() {
    apiPolicyWaiverService.getPolicyWaivers(OwnerType.APPLICATION, app.getId());
  }

  @Test
  public void testGetPolicyWaivers_Application_Authorized_PublicId() {
    grantPermission(app.getId(), Permission.READ);
    apiPolicyWaiverService.getPolicyWaivers(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test
  public void testGetPolicyWaivers_Organization_Authorized() {
    grantPermission(org.getId(), Permission.READ);
    apiPolicyWaiverService.getPolicyWaivers(OwnerType.ORGANIZATION, org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyWaivers_Organization_Unauthorized() {
    login();
    apiPolicyWaiverService.getPolicyWaivers(OwnerType.ORGANIZATION, org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyWaivers_Organization_Unauthenticated() {
    apiPolicyWaiverService.getPolicyWaivers(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testGetPolicyWaivers_Repository_Authorized() {
    grantPermission(repository.getId(), Permission.READ);
    apiPolicyWaiverService.getPolicyWaivers(OwnerType.REPOSITORY, repository.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyWaivers_Repository_Unauthorized() {
    login();
    apiPolicyWaiverService.getPolicyWaivers(OwnerType.REPOSITORY, repository.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyWaivers_Repository_Unauthenticated() {
    apiPolicyWaiverService.getPolicyWaivers(OwnerType.REPOSITORY, repository.getId());
  }

  @Test
  public void testGetPolicyWaivers_RepositoryContainer_Authorized() {
    grantPermission(REPOSITORY_CONTAINER_ID, Permission.READ);
    apiPolicyWaiverService.getPolicyWaivers(REPOSITORY_CONTAINER, repository.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyWaivers_RepositoryContainer_Unauthorized() {
    login();
    apiPolicyWaiverService.getPolicyWaivers(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyWaivers_RepositoryContainer_Unauthenticated() {

    apiPolicyWaiverService.getPolicyWaivers(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableWaivers_RootOrganization_Unauthenticated() {
    String policyViolationId = setUpParameterizePolicyViolation(Organization.ROOT_ORGANIZATION_ID);
    apiPolicyWaiverService.getApplicableWaivers(policyViolationId);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableWaivers_RootOrganization_Unauthorized() {
    String policyViolationId = setUpParameterizePolicyViolation(Organization.ROOT_ORGANIZATION_ID);
    login();
    apiPolicyWaiverService.getApplicableWaivers(policyViolationId);
  }

  @Test
  public void testGetApplicableWaivers_RootOrganization_Authorized() {
    grantPermission(Organization.ROOT_ORGANIZATION_ID, Permission.READ);
    String policyViolationId = setUpParameterizePolicyViolation(Organization.ROOT_ORGANIZATION_ID);
    apiPolicyWaiverService.getApplicableWaivers(policyViolationId);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableWaivers_Organization_Unauthenticated() {
    apiPolicyWaiverService.getApplicableWaivers(policyViolation.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableWaivers_Organization_Unauthorized() {
    login();
    apiPolicyWaiverService.getApplicableWaivers(policyViolation.getId());
  }

  @Test
  public void testGetApplicableWaivers_Organization_Authorized() {
    grantPermission(org.getId(), Permission.READ);
    apiPolicyWaiverService.getApplicableWaivers(policyViolation.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableWaivers_Application_Unauthenticated() {
    String policyViolationId = setUpParameterizePolicyViolation(app.getId());
    apiPolicyWaiverService.getApplicableWaivers(policyViolationId);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableWaiver_Application_Unauthorized() {
    String policyViolationId = setUpParameterizePolicyViolation(app.getId());
    login();
    apiPolicyWaiverService.getApplicableWaivers(policyViolationId);
  }

  @Test
  public void testGetApplicableWaiver_Application_Authorized() {
    grantPermission(app.getId(), Permission.READ);
    String policyViolationId = setUpParameterizePolicyViolation(app.getId());
    apiPolicyWaiverService.getApplicableWaivers(policyViolationId);
  }

  private void addPolicyWaiverWithDefaultOptions(OwnerType ownerType, String ownerId, String violationId) {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(
        ownerType,
        ownerId,
        violationId,
        null,
        false,
        null
    );
  }
}
