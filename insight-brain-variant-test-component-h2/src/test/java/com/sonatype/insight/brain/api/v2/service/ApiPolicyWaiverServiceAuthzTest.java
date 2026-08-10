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
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.OwnerType.REPOSITORY_CONTAINER;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.DEFAULT;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiPolicyWaiverServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  private static final String POLICY_WAIVER_ID = "policy-waiver-id";

  @Inject
  private ApiPolicyWaiverService apiPolicyWaiverService;

  private PolicyViolation policyViolation;

  private ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation;

  private String setUpParameterizePolicyViolation(String ownerId) {
    Policy policy = tempEntity.newPolicy(ownerId);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation violation = tempEntity.newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");
    return violation.getId();
  }

  @BeforeEach
  public void setUpPolicyViolation() {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");
    proxyRepositoryPolicyViolation =
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
  @Test
  public void testAddPolicyWaiver_Application_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.APPLICATION,
            "waiver comment"));
  }

  /**
   * @deprecated The tested method is deprecated
   */
  @Deprecated
  @Test
  public void testAddPolicyWaiver_Application_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.APPLICATION,
            "waiver comment"));
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
  @Test
  public void testAddPolicyWaiver_Organization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.ORGANIZATION,
            "waiver comment"));
  }

  /**
   * @deprecated The tested method is deprecated
   */
  @Deprecated
  @Test
  public void testAddWaiver_Organization_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.ORGANIZATION,
            "waiver comment"));
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Application_Authorized() {
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    addPolicyWaiverWithDefaultOptions(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Application_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> addPolicyWaiverWithDefaultOptions(OwnerType.APPLICATION, app.getId(), policyViolation.getId()));
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Application_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> addPolicyWaiverWithDefaultOptions(OwnerType.APPLICATION, app.getId(), policyViolation.getId()));
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Organization_Authorized() {
    grantPermission(org.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    addPolicyWaiverWithDefaultOptions(OwnerType.ORGANIZATION, org.getId(),
        policyViolation.getId());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Organization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> addPolicyWaiverWithDefaultOptions(OwnerType.ORGANIZATION, org.getId(), policyViolation.getId()));
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Organization_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> addPolicyWaiverWithDefaultOptions(OwnerType.ORGANIZATION, org.getId(), policyViolation.getId()));
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_RootOrganization_Authorized() {
    grantPermission(Organization.ROOT_ORGANIZATION_ID, Permission.WAIVE_POLICY_VIOLATIONS);
    addPolicyWaiverWithDefaultOptions(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        policyViolation.getId());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_RootOrganization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> addPolicyWaiverWithDefaultOptions(OwnerType.ORGANIZATION,
        Organization.ROOT_ORGANIZATION_ID, policyViolation.getId()));
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_RootOrganization_Unauthorizedd() {
    login();
    assertThrows(UnauthorizedException.class, () -> addPolicyWaiverWithDefaultOptions(OwnerType.ORGANIZATION,
        Organization.ROOT_ORGANIZATION_ID, policyViolation.getId()));
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

  @Test
  public void testDeletePolicyWaiver_Application_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.deletePolicyWaiver(OwnerType.APPLICATION, app.getId(), POLICY_WAIVER_ID));
  }

  @Test
  public void testDeletePolicyWaiver_Application__Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.deletePolicyWaiver(OwnerType.APPLICATION, app.getId(), POLICY_WAIVER_ID));
  }

  @Test
  public void testDeletePolicyWaiver_Organization_Authorized() {
    grantPermission(org.getId(), Permission.WAIVE_POLICY_VIOLATIONS);

    PolicyWaiver waiver = tempEntity.newWaiver(tempEntity.newPolicy(org).getId(), org.getId());
    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.ORGANIZATION, org.getId(), waiver.getId());
  }

  @Test
  public void testDeletePolicyWaiver_Organization_Unauthorized() {
    login();

    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.deletePolicyWaiver(OwnerType.ORGANIZATION, org.getId(), POLICY_WAIVER_ID));
  }

  @Test
  public void testDeletePolicyWaiver_Organization__Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.deletePolicyWaiver(OwnerType.ORGANIZATION, org.getId(), POLICY_WAIVER_ID));
  }

  @Test
  public void testDeletePolicyWaiver_Repository_Authorized() {
    grantPermission(repository.getId(), Permission.WAIVE_POLICY_VIOLATIONS);

    PolicyWaiver waiver = tempEntity.newWaiver(tempEntity.newPolicy().getId(), repository.getId());
    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.REPOSITORY, repository.getId(), waiver.getId());
  }

  @Test
  public void testDeletePolicyWaiver_Repository_Unauthorized() {
    login();

    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.deletePolicyWaiver(OwnerType.REPOSITORY, repository.getId(), POLICY_WAIVER_ID));
  }

  @Test
  public void testDeletePolicyWaiver_Repository_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.deletePolicyWaiver(OwnerType.REPOSITORY, repository.getId(), POLICY_WAIVER_ID));
  }

  @Test
  public void testDeletePolicyWaiver_RepositoryContainer_Authorized() {
    grantPermission(REPOSITORY_CONTAINER_ID, Permission.WAIVE_POLICY_VIOLATIONS);

    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), REPOSITORY_CONTAINER_ID);

    apiPolicyWaiverService.deletePolicyWaiver(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, policyWaiver.getId());
  }

  @Test
  public void testDeletePolicyWaiver_RepositoryContainer_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> apiPolicyWaiverService.deletePolicyWaiver(REPOSITORY_CONTAINER,
        REPOSITORY_CONTAINER_ID, POLICY_WAIVER_ID));
  }

  @Test
  public void testDeletePolicyWaiver_RepositoryContainer_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> apiPolicyWaiverService.deletePolicyWaiver(
        REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, POLICY_WAIVER_ID));
  }

  @Test
  public void testGetPolicyWaivers_Application_Authorized() {
    grantPermission(app.getId(), Permission.READ);
    apiPolicyWaiverService.getPolicyWaivers(OwnerType.APPLICATION, app.getId());
  }

  @Test
  public void testGetPolicyWaivers_Application_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.getPolicyWaivers(OwnerType.APPLICATION, app.getId()));
  }

  @Test
  public void testGetPolicyWaivers_Application_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.getPolicyWaivers(OwnerType.APPLICATION, app.getId()));
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

  @Test
  public void testGetPolicyWaivers_Organization_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.getPolicyWaivers(OwnerType.ORGANIZATION, org.getId()));
  }

  @Test
  public void testGetPolicyWaivers_Organization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.getPolicyWaivers(OwnerType.ORGANIZATION, org.getId()));
  }

  @Test
  public void testGetPolicyWaivers_Repository_Authorized() {
    grantPermission(repository.getId(), Permission.READ);
    apiPolicyWaiverService.getPolicyWaivers(OwnerType.REPOSITORY, repository.getId());
  }

  @Test
  public void testGetPolicyWaivers_Repository_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.getPolicyWaivers(OwnerType.REPOSITORY, repository.getId()));
  }

  @Test
  public void testGetPolicyWaivers_Repository_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.getPolicyWaivers(OwnerType.REPOSITORY, repository.getId()));
  }

  @Test
  public void testGetPolicyWaivers_RepositoryContainer_Authorized() {
    grantPermission(REPOSITORY_CONTAINER_ID, Permission.READ);
    apiPolicyWaiverService.getPolicyWaivers(REPOSITORY_CONTAINER, repository.getId());
  }

  @Test
  public void testGetPolicyWaivers_RepositoryContainer_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.getPolicyWaivers(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID));
  }

  @Test
  public void testGetPolicyWaivers_RepositoryContainer_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.getPolicyWaivers(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID));
  }

  @Test
  public void testGetApplicableWaivers_RootOrganization_Unauthenticated() {
    String policyViolationId = setUpParameterizePolicyViolation(Organization.ROOT_ORGANIZATION_ID);
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.getApplicableWaivers(policyViolationId));
  }

  @Test
  public void testGetApplicableWaivers_RootOrganization_Unauthorized() {
    String policyViolationId = setUpParameterizePolicyViolation(Organization.ROOT_ORGANIZATION_ID);
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.getApplicableWaivers(policyViolationId));
  }

  @Test
  public void testGetApplicableWaivers_RootOrganization_Authorized() {
    grantPermission(Organization.ROOT_ORGANIZATION_ID, Permission.READ);
    String policyViolationId = setUpParameterizePolicyViolation(Organization.ROOT_ORGANIZATION_ID);
    apiPolicyWaiverService.getApplicableWaivers(policyViolationId);
  }

  @Test
  public void testGetApplicableWaivers_Organization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.getApplicableWaivers(policyViolation.getId()));
  }

  @Test
  public void testGetApplicableWaivers_Organization_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.getApplicableWaivers(policyViolation.getId()));
  }

  @Test
  public void testGetApplicableWaivers_Organization_Authorized() {
    grantPermission(org.getId(), Permission.READ);
    apiPolicyWaiverService.getApplicableWaivers(policyViolation.getId());
  }

  @Test
  public void testGetApplicableWaivers_Application_Unauthenticated() {
    String policyViolationId = setUpParameterizePolicyViolation(app.getId());
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.getApplicableWaivers(policyViolationId));
  }

  @Test
  public void testGetApplicableWaivers_Application_Unauthorized() {
    String policyViolationId = setUpParameterizePolicyViolation(app.getId());
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.getApplicableWaivers(policyViolationId));
  }

  @Test
  public void testGetApplicableWaivers_Application_Authorized() {
    grantPermission(app.getId(), Permission.READ);
    String policyViolationId = setUpParameterizePolicyViolation(app.getId());
    apiPolicyWaiverService.getApplicableWaivers(policyViolationId);
  }

  @Test
  public void testGetApplicableWaivers_Repository_Unauthenticated() {
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId());
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.getApplicableWaivers(proxyRepositoryPolicyViolation.getId()));
  }

  @Test
  public void testGetApplicableWaivers_Repository_Unauthorized() {
    login();
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId());
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.getApplicableWaivers(proxyRepositoryPolicyViolation.getId()));
  }

  @Test
  public void testGetApplicableWaivers_Repository_Authorized() {
    grantPermission(repository.getId(), Permission.READ);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId());
    apiPolicyWaiverService.getApplicableWaivers(proxyRepositoryPolicyViolation.getId());
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByAppScanComponent_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByAppScanComponent(OwnerType.APPLICATION,
            app.getPublicId(), "scanId", null, null, null, null));
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByAppScanComponent_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByAppScanComponent(OwnerType.APPLICATION,
            app.getPublicId(), "scanId", null, null, null, null));
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByAppScanComponent_Authorized() {
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    String scanId = "scanId";
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");

    assertThrows(NotFoundException.class,
        () -> apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByAppScanComponent(OwnerType.APPLICATION,
            app.getPublicId(), scanId, componentIdentifier, null, null, null));
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_Application_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(OwnerType.APPLICATION,
            app.getPublicId(), BuildStageType.ID, null, null, null, null));
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_Application_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(OwnerType.APPLICATION,
            app.getPublicId(), BuildStageType.ID, null, null, null, null));
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_Application_Authorized() {
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    assertThrows(BadRequestException.class,
        () -> apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(OwnerType.APPLICATION,
            app.getPublicId(), BuildStageType.ID, null, null, null, null));
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_Organization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(
            OwnerType.ORGANIZATION, org.getPublicId(), BuildStageType.ID, null, null, null, null));
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_Organization_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(
            OwnerType.ORGANIZATION, org.getPublicId(), BuildStageType.ID, null, null, null, null));
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_Organization_Authorized() {
    grantPermission(org.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    assertThrows(BadRequestException.class,
        () -> apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(
            OwnerType.ORGANIZATION, org.getPublicId(), BuildStageType.ID, null, null, null, null));
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_RootOrganization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(
            OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, BuildStageType.ID, null, null, null, null));
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_RootOrganization_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(
            OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, BuildStageType.ID, null, null, null, null));
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_RootOrganization_Authorized() {
    grantPermission(Organization.ROOT_ORGANIZATION_ID, Permission.WAIVE_POLICY_VIOLATIONS);
    assertThrows(BadRequestException.class,
        () -> apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(
            OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, BuildStageType.ID, null, null, null, null));
  }

  private void addPolicyWaiverWithDefaultOptions(OwnerType ownerType, String ownerId, String violationId) {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(
        ownerType,
        ownerId,
        violationId,
        new ApiWaiverOptionsDTO(null, DEFAULT, null, null, false));
  }

  @Test
  public void testGetTransitivePolicyWaiversByAppScanComponent_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.getTransitivePolicyWaiversByAppScanComponent(OwnerType.APPLICATION, app.getId(),
            null, null, null, "hash"));
  }

  @Test
  public void testGetTransitivePolicyWaiversByAppScanComponent_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.getTransitivePolicyWaiversByAppScanComponent(OwnerType.APPLICATION, app.getId(),
            null, null, null, "hash"));
  }

  @Test
  public void testGetTransitivePolicyWaiversByAppScanComponent_Authorized() {
    grantReadPermission(app.getId());
    assertThrows(NotFoundException.class,
        () -> apiPolicyWaiverService.getTransitivePolicyWaiversByAppScanComponent(OwnerType.APPLICATION, app.getId(),
            null, null, null, "hash"));
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Repository_Authorized() {
    grantPermission(repository.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    addPolicyWaiverWithDefaultOptions(OwnerType.REPOSITORY, repository.getId(), proxyRepositoryPolicyViolation.getId());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Repository_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> addPolicyWaiverWithDefaultOptions(OwnerType.REPOSITORY,
        repository.getId(), proxyRepositoryPolicyViolation.getId()));
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Repository_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> addPolicyWaiverWithDefaultOptions(OwnerType.REPOSITORY,
        repository.getId(), proxyRepositoryPolicyViolation.getId()));
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_RepositoryContainer_Authorized() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.WAIVE_POLICY_VIOLATIONS);
    addPolicyWaiverWithDefaultOptions(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
        proxyRepositoryPolicyViolation.getId());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_RepositoryContainer_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> addPolicyWaiverWithDefaultOptions(
        OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
        proxyRepositoryPolicyViolation.getId()));
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_RepositoryContainer_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> addPolicyWaiverWithDefaultOptions(
        OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
        proxyRepositoryPolicyViolation.getId()));
  }

  @Test
  public void testGetPolicyWaiver_Application_Authorized() {
    grantPermission(app.getId(), Permission.READ);
    assertThrows(NotFoundException.class,
        () -> apiPolicyWaiverService.getPolicyWaiver(OwnerType.APPLICATION, app.getId(), POLICY_WAIVER_ID));
  }

  @Test
  public void testGetPolicyWaiver_Application_Authorized_PublicId() {
    grantPermission(app.getId(), Permission.READ);
    assertThrows(NotFoundException.class,
        () -> apiPolicyWaiverService.getPolicyWaiver(OwnerType.APPLICATION, app.getPublicId(), POLICY_WAIVER_ID));
  }

  @Test
  public void testGetPolicyWaiver_Application_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.getPolicyWaiver(OwnerType.APPLICATION, app.getId(), POLICY_WAIVER_ID));
  }

  @Test
  public void testGetPolicyWaiver_Application_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.getPolicyWaiver(OwnerType.APPLICATION, app.getId(), POLICY_WAIVER_ID));
  }

  @Test
  public void testGetPolicyWaiver_Organization_Authorized() {
    grantPermission(org.getId(), Permission.READ);
    assertThrows(NotFoundException.class,
        () -> apiPolicyWaiverService.getPolicyWaiver(OwnerType.ORGANIZATION, org.getId(), POLICY_WAIVER_ID));
  }

  @Test
  public void testGetPolicyWaiver_Organization_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.getPolicyWaiver(OwnerType.ORGANIZATION, org.getId(), POLICY_WAIVER_ID));
  }

  @Test
  public void testGetPolicyWaiver_Organization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.getPolicyWaiver(OwnerType.ORGANIZATION, org.getId(), POLICY_WAIVER_ID));
  }

  @Test
  public void testGetPolicyWaiver_Repository_Authorized() {
    grantPermission(repository.getId(), Permission.READ);
    assertThrows(NotFoundException.class,
        () -> apiPolicyWaiverService.getPolicyWaiver(OwnerType.REPOSITORY, repository.getId(), POLICY_WAIVER_ID));
  }

  @Test
  public void testGetPolicyWaiver_Repository_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.getPolicyWaiver(OwnerType.REPOSITORY, repository.getId(), POLICY_WAIVER_ID));
  }

  @Test
  public void testGetPolicyWaiver_Repository_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.getPolicyWaiver(OwnerType.REPOSITORY, repository.getId(), POLICY_WAIVER_ID));
  }

  @Test
  public void testGetPolicyWaiver_RepositoryContainer_Authorized() {
    grantPermission(REPOSITORY_CONTAINER_ID, Permission.READ);
    assertThrows(NotFoundException.class,
        () -> apiPolicyWaiverService.getPolicyWaiver(REPOSITORY_CONTAINER, repository.getId(), POLICY_WAIVER_ID));
  }

  @Test
  public void testGetPolicyWaiver_RepositoryContainer_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.getPolicyWaiver(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID,
            POLICY_WAIVER_ID));
  }

  @Test
  public void testGetPolicyWaiver_RepositoryContainer_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.getPolicyWaiver(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID,
            POLICY_WAIVER_ID));
  }

  @Test
  public void testUpdatePolicyWaiver_Application_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.updatePolicyWaiver(OwnerType.APPLICATION, app.getId(), POLICY_WAIVER_ID, null));
  }

  @Test
  public void testUpdatePolicyWaiver_Application_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.updatePolicyWaiver(OwnerType.APPLICATION, app.getId(), POLICY_WAIVER_ID, null));
  }

  @Test
  public void testUpdatePolicyWaiver_Application_Authorized() {
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    assertThrows(NotFoundException.class,
        () -> apiPolicyWaiverService.updatePolicyWaiver(OwnerType.APPLICATION, app.getId(), POLICY_WAIVER_ID, null));
  }

  @Test
  public void testUpdatePolicyWaiver_Organization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.updatePolicyWaiver(OwnerType.ORGANIZATION, org.getId(), POLICY_WAIVER_ID, null));
  }

  @Test
  public void testUpdatePolicyWaiver_Organization_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.updatePolicyWaiver(OwnerType.ORGANIZATION, org.getId(), POLICY_WAIVER_ID, null));
  }

  @Test
  public void testUpdatePolicyWaiver_Organization_Authorized() {
    grantPermission(org.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    assertThrows(NotFoundException.class,
        () -> apiPolicyWaiverService.updatePolicyWaiver(OwnerType.ORGANIZATION, org.getId(), POLICY_WAIVER_ID, null));
  }

  @Test
  public void testUpdatePolicyWaiver_RootOrganization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.updatePolicyWaiver(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
            POLICY_WAIVER_ID, null));
  }

  @Test
  public void testUpdatePolicyWaiver_RootOrganization_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.updatePolicyWaiver(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
            POLICY_WAIVER_ID, null));
  }

  @Test
  public void testUpdatePolicyWaiver_RootOrganization_Authorized() {
    grantPermission(Organization.ROOT_ORGANIZATION_ID, Permission.WAIVE_POLICY_VIOLATIONS);
    assertThrows(NotFoundException.class,
        () -> apiPolicyWaiverService.updatePolicyWaiver(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
            POLICY_WAIVER_ID, null));
  }

  @Test
  public void testUpdatePolicyWaiver_Repository_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.updatePolicyWaiver(OwnerType.REPOSITORY, repository.getId(), POLICY_WAIVER_ID,
            null));
  }

  @Test
  public void testUpdatePolicyWaiver_Repository_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.updatePolicyWaiver(OwnerType.REPOSITORY, repository.getId(), POLICY_WAIVER_ID,
            null));
  }

  @Test
  public void testUpdatePolicyWaiver_Repository_Authorized() {
    grantPermission(repository.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    assertThrows(NotFoundException.class,
        () -> apiPolicyWaiverService.updatePolicyWaiver(OwnerType.REPOSITORY, repository.getId(), POLICY_WAIVER_ID,
            null));
  }

  @Test
  public void testUpdatePolicyWaiver_RepositoryManager_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.updatePolicyWaiver(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(),
            POLICY_WAIVER_ID, null));
  }

  @Test
  public void testUpdatePolicyWaiver_RepositoryManager_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.updatePolicyWaiver(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(),
            POLICY_WAIVER_ID, null));
  }

  @Test
  public void testUpdatePolicyWaiver_RepositoryManager_Authorized() {
    grantPermission(repositoryManager.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    assertThrows(NotFoundException.class,
        () -> apiPolicyWaiverService.updatePolicyWaiver(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(),
            POLICY_WAIVER_ID, null));
  }

  @Test
  public void testUpdatePolicyWaiver_RepositoryContainer_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.updatePolicyWaiver(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID,
            POLICY_WAIVER_ID, null));
  }

  @Test
  public void testUpdatePolicyWaiver_RepositoryContainer_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.updatePolicyWaiver(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID,
            POLICY_WAIVER_ID, null));
  }

  @Test
  public void testUpdatePolicyWaiver_RepositoryContainer_Authorized() {
    grantPermission(REPOSITORY_CONTAINER_ID, Permission.WAIVE_POLICY_VIOLATIONS);
    assertThrows(NotFoundException.class,
        () -> apiPolicyWaiverService.updatePolicyWaiver(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID,
            POLICY_WAIVER_ID, null));
  }

  @Test
  public void testAddContainerImageWaiver_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.addContainerImageWaiver(app.getId(), null));
  }

  @Test
  public void testAddContainerImageWaiver_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.addContainerImageWaiver(app.getId(), null));
  }

  @Test
  public void testAddContainerImageWaiver_Authorized() {
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    assertThrows(NotFoundException.class,
        () -> apiPolicyWaiverService.addContainerImageWaiver(app.getId(), null));
  }

  @Test
  public void testDeleteContainerImageWaivers_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.deleteContainerImageWaiver(app.getId()));
  }

  @Test
  public void testDeleteContainerImageWaivers_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.deleteContainerImageWaiver(app.getId()));
  }

  @Test
  public void testDeleteContainerImageWaivers_Authorized() {
    // Container READ grants full Firewall access (permittedRepositoryIds = null),
    // which lets the Firewall repo-scope gate pass through to the @Authorize WAIVE check.
    grantPermission(REPOSITORY_CONTAINER_ID, Permission.READ);
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    assertThrows(NotFoundException.class,
        () -> apiPolicyWaiverService.deleteContainerImageWaiver(app.getId()));
  }

  @Test
  public void getAllPolicyContainerWaivers_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiPolicyWaiverService.getAllPolicyContainerWaivers(1, 1));
  }

  @Test
  public void getAllPolicyContainerWaivers_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiPolicyWaiverService.getAllPolicyContainerWaivers(1, 1));
  }
}
