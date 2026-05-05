/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiWaiverOptionsDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.OwnerType.REPOSITORY_CONTAINER;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.DEFAULT;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ApiPolicyWaiverServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final String POLICY_WAIVER_ID = "policy-waiver-id";

  @Inject
  private ApiPolicyWaiverService apiPolicyWaiverService;

  private PolicyViolation policyViolation;

  private RepositoryPolicyViolation repositoryPolicyViolation;

  private String setUpParameterizePolicyViolation(String ownerId) {
    Policy policy = tempEntity.newPolicy(ownerId);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation violation = tempEntity.newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");
    return violation.getId();
  }

  @Before
  public void setUpPolicyViolation() {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");
    repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());
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
  public void testAddPolicyWaiver_Application_Unauthorized() {
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
  public void testAddWaiver_Organization_Unauthorized() {
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
  public void testAddPolicyWaiverByPolicyViolationId_Application_Unauthorized() {
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
  public void testAddPolicyWaiverByPolicyViolationId_Organization_Unauthorized() {
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
  public void testAddPolicyWaiverByPolicyViolationId_RootOrganization_Unauthorizedd() {
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
    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.APPLICATION, app.getId(), POLICY_WAIVER_ID);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeletePolicyWaiver_Application__Unauthenticated() {
    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.APPLICATION, app.getId(), POLICY_WAIVER_ID);
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

    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.ORGANIZATION, org.getId(), POLICY_WAIVER_ID);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeletePolicyWaiver_Organization__Unauthenticated() {
    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.ORGANIZATION, org.getId(), POLICY_WAIVER_ID);
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

    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.REPOSITORY, repository.getId(), POLICY_WAIVER_ID);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeletePolicyWaiver_Repository_Unauthenticated() {
    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.REPOSITORY, repository.getId(), POLICY_WAIVER_ID);
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
    apiPolicyWaiverService.deletePolicyWaiver(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, POLICY_WAIVER_ID);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeletePolicyWaiver_RepositoryContainer_Unauthenticated() {
    apiPolicyWaiverService.deletePolicyWaiver(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, POLICY_WAIVER_ID);
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
  public void testGetApplicableWaivers_Application_Unauthorized() {
    String policyViolationId = setUpParameterizePolicyViolation(app.getId());
    login();
    apiPolicyWaiverService.getApplicableWaivers(policyViolationId);
  }

  @Test
  public void testGetApplicableWaivers_Application_Authorized() {
    grantPermission(app.getId(), Permission.READ);
    String policyViolationId = setUpParameterizePolicyViolation(app.getId());
    apiPolicyWaiverService.getApplicableWaivers(policyViolationId);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableWaivers_Repository_Unauthenticated() {
    RepositoryPolicyViolation repositoryPolicyViolation = tempEntity.newRepositoryPolicyViolation(repository.getId());
    apiPolicyWaiverService.getApplicableWaivers(repositoryPolicyViolation.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableWaivers_Repository_Unauthorized() {
    login();
    RepositoryPolicyViolation repositoryPolicyViolation = tempEntity.newRepositoryPolicyViolation(repository.getId());
    apiPolicyWaiverService.getApplicableWaivers(repositoryPolicyViolation.getId());
  }

  @Test
  public void testGetApplicableWaivers_Repository_Authorized() {
    grantPermission(repository.getId(), Permission.READ);
    RepositoryPolicyViolation repositoryPolicyViolation = tempEntity.newRepositoryPolicyViolation(repository.getId());
    apiPolicyWaiverService.getApplicableWaivers(repositoryPolicyViolation.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddWaiverToTransitivePolicyViolationsByAppScanComponent_Unauthenticated() {
    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByAppScanComponent(OwnerType.APPLICATION,
        app.getPublicId(), "scanId", null, null, null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddWaiverToTransitivePolicyViolationsByAppScanComponent_Unauthorized() {
    login();
    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByAppScanComponent(OwnerType.APPLICATION,
        app.getPublicId(), "scanId", null, null, null, null);
  }

  @Test(expected = NotFoundException.class)
  public void testAddWaiverToTransitivePolicyViolationsByAppScanComponent_Authorized() {
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    String scanId = "scanId";
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");

    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByAppScanComponent(OwnerType.APPLICATION,
        app.getPublicId(), scanId, componentIdentifier, null, null, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_Application_Unauthenticated() {
    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(OwnerType.APPLICATION,
        app.getPublicId(), BuildStageType.ID, null, null, null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_Application_Unauthorized() {
    login();
    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(OwnerType.APPLICATION,
        app.getPublicId(), BuildStageType.ID, null, null, null, null);
  }

  @Test(expected = BadRequestException.class)
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_Application_Authorized() {
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(OwnerType.APPLICATION,
        app.getPublicId(), BuildStageType.ID, null, null, null, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_Organization_Unauthenticated() {
    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(OwnerType.ORGANIZATION,
        org.getPublicId(), BuildStageType.ID, null, null, null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_Organization_Unauthorized() {
    login();
    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(OwnerType.ORGANIZATION,
        org.getPublicId(), BuildStageType.ID, null, null, null, null);
  }

  @Test(expected = BadRequestException.class)
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_Organization_Authorized() {
    grantPermission(org.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(OwnerType.ORGANIZATION,
        org.getPublicId(), BuildStageType.ID, null, null, null, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_RootOrganization_Unauthenticated() {
    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(OwnerType.ORGANIZATION,
        Organization.ROOT_ORGANIZATION_ID, BuildStageType.ID, null, null, null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_RootOrganization_Unauthorized() {
    login();
    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(OwnerType.ORGANIZATION,
        Organization.ROOT_ORGANIZATION_ID, BuildStageType.ID, null, null, null, null);
  }

  @Test(expected = BadRequestException.class)
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_RootOrganization_Authorized() {
    grantPermission(Organization.ROOT_ORGANIZATION_ID, Permission.WAIVE_POLICY_VIOLATIONS);
    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(OwnerType.ORGANIZATION,
        Organization.ROOT_ORGANIZATION_ID, BuildStageType.ID, null, null, null, null);
  }

  private void addPolicyWaiverWithDefaultOptions(OwnerType ownerType, String ownerId, String violationId) {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(
        ownerType,
        ownerId,
        violationId,
        new ApiWaiverOptionsDTO(null, DEFAULT, null, null, false));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetTransitivePolicyWaiversByAppScanComponent_Unauthenticated() {
    apiPolicyWaiverService.getTransitivePolicyWaiversByAppScanComponent(OwnerType.APPLICATION, app.getId(), null, null,
        null, "hash");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetTransitivePolicyWaiversByAppScanComponent_Unauthorized() {
    login();
    apiPolicyWaiverService.getTransitivePolicyWaiversByAppScanComponent(OwnerType.APPLICATION, app.getId(), null, null,
        null, "hash");
  }

  @Test(expected = NotFoundException.class)
  public void testGetTransitivePolicyWaiversByAppScanComponent_Authorized() {
    grantReadPermission(app.getId());
    apiPolicyWaiverService.getTransitivePolicyWaiversByAppScanComponent(OwnerType.APPLICATION, app.getId(), null, null,
        null, "hash");
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Repository_Authorized() {
    grantPermission(repository.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    addPolicyWaiverWithDefaultOptions(OwnerType.REPOSITORY, repository.getId(), repositoryPolicyViolation.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddPolicyWaiverByPolicyViolationId_Repository_Unauthenticated() {
    addPolicyWaiverWithDefaultOptions(OwnerType.REPOSITORY, repository.getId(), repositoryPolicyViolation.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddPolicyWaiverByPolicyViolationId_Repository_Unauthorized() {
    login();
    addPolicyWaiverWithDefaultOptions(OwnerType.REPOSITORY, repository.getId(), repositoryPolicyViolation.getId());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_RepositoryContainer_Authorized() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.WAIVE_POLICY_VIOLATIONS);
    addPolicyWaiverWithDefaultOptions(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
        repositoryPolicyViolation.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddPolicyWaiverByPolicyViolationId_RepositoryContainer_Unauthenticated() {
    addPolicyWaiverWithDefaultOptions(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
        repositoryPolicyViolation.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddPolicyWaiverByPolicyViolationId_RepositoryContainer_Unauthorized() {
    login();
    addPolicyWaiverWithDefaultOptions(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
        repositoryPolicyViolation.getId());
  }

  @Test(expected = NotFoundException.class)
  public void testGetPolicyWaiver_Application_Authorized() {
    grantPermission(app.getId(), Permission.READ);
    apiPolicyWaiverService.getPolicyWaiver(OwnerType.APPLICATION, app.getId(), POLICY_WAIVER_ID);
  }

  @Test(expected = NotFoundException.class)
  public void testGetPolicyWaiver_Application_Authorized_PublicId() {
    grantPermission(app.getId(), Permission.READ);
    apiPolicyWaiverService.getPolicyWaiver(OwnerType.APPLICATION, app.getPublicId(), POLICY_WAIVER_ID);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyWaiver_Application_Unauthorized() {
    login();
    apiPolicyWaiverService.getPolicyWaiver(OwnerType.APPLICATION, app.getId(), POLICY_WAIVER_ID);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyWaiver_Application_Unauthenticated() {
    apiPolicyWaiverService.getPolicyWaiver(OwnerType.APPLICATION, app.getId(), POLICY_WAIVER_ID);
  }

  @Test(expected = NotFoundException.class)
  public void testGetPolicyWaiver_Organization_Authorized() {
    grantPermission(org.getId(), Permission.READ);
    apiPolicyWaiverService.getPolicyWaiver(OwnerType.ORGANIZATION, org.getId(), POLICY_WAIVER_ID);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyWaiver_Organization_Unauthorized() {
    login();
    apiPolicyWaiverService.getPolicyWaiver(OwnerType.ORGANIZATION, org.getId(), POLICY_WAIVER_ID);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyWaiver_Organization_Unauthenticated() {
    apiPolicyWaiverService.getPolicyWaiver(OwnerType.ORGANIZATION, org.getId(), POLICY_WAIVER_ID);
  }

  @Test(expected = NotFoundException.class)
  public void testGetPolicyWaiver_Repository_Authorized() {
    grantPermission(repository.getId(), Permission.READ);
    apiPolicyWaiverService.getPolicyWaiver(OwnerType.REPOSITORY, repository.getId(), POLICY_WAIVER_ID);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyWaiver_Repository_Unauthorized() {
    login();
    apiPolicyWaiverService.getPolicyWaiver(OwnerType.REPOSITORY, repository.getId(), POLICY_WAIVER_ID);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyWaiver_Repository_Unauthenticated() {
    apiPolicyWaiverService.getPolicyWaiver(OwnerType.REPOSITORY, repository.getId(), POLICY_WAIVER_ID);
  }

  @Test(expected = NotFoundException.class)
  public void testGetPolicyWaiver_RepositoryContainer_Authorized() {
    grantPermission(REPOSITORY_CONTAINER_ID, Permission.READ);
    apiPolicyWaiverService.getPolicyWaiver(REPOSITORY_CONTAINER, repository.getId(), POLICY_WAIVER_ID);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyWaiver_RepositoryContainer_Unauthorized() {
    login();
    apiPolicyWaiverService.getPolicyWaiver(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, POLICY_WAIVER_ID);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyWaiver_RepositoryContainer_Unauthenticated() {
    apiPolicyWaiverService.getPolicyWaiver(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, POLICY_WAIVER_ID);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdatePolicyWaiver_Application_Unauthenticated() {
    apiPolicyWaiverService.updatePolicyWaiver(OwnerType.APPLICATION, app.getId(), POLICY_WAIVER_ID, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdatePolicyWaiver_Application_Unauthorized() {
    login();
    apiPolicyWaiverService.updatePolicyWaiver(OwnerType.APPLICATION, app.getId(), POLICY_WAIVER_ID, null);
  }

  @Test(expected = NotFoundException.class)
  public void testUpdatePolicyWaiver_Application_Authorized() {
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    apiPolicyWaiverService.updatePolicyWaiver(OwnerType.APPLICATION, app.getId(), POLICY_WAIVER_ID, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdatePolicyWaiver_Organization_Unauthenticated() {
    apiPolicyWaiverService.updatePolicyWaiver(OwnerType.ORGANIZATION, org.getId(), POLICY_WAIVER_ID, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdatePolicyWaiver_Organization_Unauthorized() {
    login();
    apiPolicyWaiverService.updatePolicyWaiver(OwnerType.ORGANIZATION, org.getId(), POLICY_WAIVER_ID, null);
  }

  @Test(expected = NotFoundException.class)
  public void testUpdatePolicyWaiver_Organization_Authorized() {
    grantPermission(org.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    apiPolicyWaiverService.updatePolicyWaiver(OwnerType.ORGANIZATION, org.getId(), POLICY_WAIVER_ID, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdatePolicyWaiver_RootOrganization_Unauthenticated() {
    apiPolicyWaiverService.updatePolicyWaiver(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        POLICY_WAIVER_ID, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdatePolicyWaiver_RootOrganization_Unauthorized() {
    login();
    apiPolicyWaiverService.updatePolicyWaiver(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        POLICY_WAIVER_ID, null);
  }

  @Test(expected = NotFoundException.class)
  public void testUpdatePolicyWaiver_RootOrganization_Authorized() {
    grantPermission(Organization.ROOT_ORGANIZATION_ID, Permission.WAIVE_POLICY_VIOLATIONS);
    apiPolicyWaiverService.updatePolicyWaiver(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        POLICY_WAIVER_ID, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdatePolicyWaiver_Repository_Unauthenticated() {
    apiPolicyWaiverService.updatePolicyWaiver(OwnerType.REPOSITORY, repository.getId(), POLICY_WAIVER_ID, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdatePolicyWaiver_Repository_Unauthorized() {
    login();
    apiPolicyWaiverService.updatePolicyWaiver(OwnerType.REPOSITORY, repository.getId(), POLICY_WAIVER_ID, null);
  }

  @Test(expected = NotFoundException.class)
  public void testUpdatePolicyWaiver_Repository_Authorized() {
    grantPermission(repository.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    apiPolicyWaiverService.updatePolicyWaiver(OwnerType.REPOSITORY, repository.getId(), POLICY_WAIVER_ID, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdatePolicyWaiver_RepositoryManager_Unauthenticated() {
    apiPolicyWaiverService.updatePolicyWaiver(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), POLICY_WAIVER_ID,
        null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdatePolicyWaiver_RepositoryManager_Unauthorized() {
    login();
    apiPolicyWaiverService.updatePolicyWaiver(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), POLICY_WAIVER_ID,
        null);
  }

  @Test(expected = NotFoundException.class)
  public void testUpdatePolicyWaiver_RepositoryManager_Authorized() {
    grantPermission(repositoryManager.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    apiPolicyWaiverService.updatePolicyWaiver(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), POLICY_WAIVER_ID,
        null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdatePolicyWaiver_RepositoryContainer_Unauthenticated() {
    apiPolicyWaiverService.updatePolicyWaiver(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, POLICY_WAIVER_ID, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdatePolicyWaiver_RepositoryContainer_Unauthorized() {
    login();
    apiPolicyWaiverService.updatePolicyWaiver(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, POLICY_WAIVER_ID, null);
  }

  @Test(expected = NotFoundException.class)
  public void testUpdatePolicyWaiver_RepositoryContainer_Authorized() {
    grantPermission(REPOSITORY_CONTAINER_ID, Permission.WAIVE_POLICY_VIOLATIONS);
    apiPolicyWaiverService.updatePolicyWaiver(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, POLICY_WAIVER_ID, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddContainerImageWaiver_Unauthenticated() {
    apiPolicyWaiverService.addContainerImageWaiver(app.getId(), null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddContainerImageWaiver_Unauthorized() {
    login();
    apiPolicyWaiverService.addContainerImageWaiver(app.getId(), null);
  }

  @Test(expected = NotFoundException.class)
  public void testAddContainerImageWaiver_Authorized() {
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    apiPolicyWaiverService.addContainerImageWaiver(app.getId(), null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteContainerImageWaivers_Unauthorized() {
    login();
    apiPolicyWaiverService.deleteContainerImageWaiver(app.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteContainerImageWaivers_Unauthenticated() {
    apiPolicyWaiverService.deleteContainerImageWaiver(app.getId());
  }

  @Test(expected = NotFoundException.class)
  public void testDeleteContainerImageWaivers_Authorized() {
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    apiPolicyWaiverService.deleteContainerImageWaiver(app.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void getAllPolicyContainerWaivers_Unauthenticated() {
    apiPolicyWaiverService.getAllPolicyContainerWaivers(1, 1);
  }

  @Test(expected = UnauthorizedException.class)
  public void getAllPolicyContainerWaivers_Unauthorized() {
    login();
    apiPolicyWaiverService.getAllPolicyContainerWaivers(1, 1);
  }
}
