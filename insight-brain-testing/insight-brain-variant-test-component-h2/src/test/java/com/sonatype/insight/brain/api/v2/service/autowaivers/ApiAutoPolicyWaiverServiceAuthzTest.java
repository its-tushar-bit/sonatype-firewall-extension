/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.autowaivers;

import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverAdapter;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiAutoPolicyWaiverServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiAutoPolicyWaiverService apiAutoPolicyWaiverService;

  @Test
  public void testGetAutoPolicyWaiver_Unauthenticated() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    assertThrows(UnauthenticatedException.class, () -> apiAutoPolicyWaiverService.getAutoPolicyWaiver(
        OwnerType.APPLICATION, application.getId(),
        autoPolicyWaiver.getId()));
  }

  @Test
  public void testGetAutoPolicyWaiver_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    assertThrows(UnauthorizedException.class, () -> apiAutoPolicyWaiverService.getAutoPolicyWaiver(
        OwnerType.APPLICATION, application.getId(),
        autoPolicyWaiver.getId()));
  }

  @Test
  public void testGetAutoPolicyWaiver_Authorized() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    grantReadPermission(application.getId());
    apiAutoPolicyWaiverService.getAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
        autoPolicyWaiver.getId());
  }

  @Test
  public void testGetAutoPolicyWaivers_Unauthenticated() {
    Application application = tempEntity.newApplicationWithParent();
    assertThrows(UnauthenticatedException.class,
        () -> apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.APPLICATION, application.getId()));
  }

  @Test
  public void testGetAutoPolicyWaivers_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    assertThrows(UnauthorizedException.class,
        () -> apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.APPLICATION, application.getId()));
  }

  @Test
  public void testGetAutoPolicyWaivers_Authorized() {
    Application application = tempEntity.newApplicationWithParent();
    grantReadPermission(application.getId());
    tempEntity.newAutoPolicyWaiver(application.getId());
    apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.APPLICATION, application.getId());
  }

  @Test
  public void testAddAutoPolicyWaiver_Unauthenticated() {
    Application application = tempEntity.newApplicationWithParent();
    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    assertThrows(UnauthenticatedException.class,
        () -> apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(), dto));
  }

  @Test
  public void testAddAutoPolicyWaiver_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    assertThrows(UnauthorizedException.class,
        () -> apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(), dto));
  }

  @Test
  public void testAddAutoPolicyWaiver_Authorized() {
    Application application = tempEntity.newApplicationWithParent();
    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.threatLevel = 8;
    dto.reachability = true;
    grantPermission(application.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(), dto);
  }

  @Test
  public void testAddAutoPolicyWaivers_Unauthenticated() {
    Application application = tempEntity.newApplicationWithParent();
    assertThrows(UnauthenticatedException.class, () -> apiAutoPolicyWaiverService.addAutoPolicyWaivers(
        OwnerType.APPLICATION,
        application.getId(),
        List.of(new ApiAutoPolicyWaiverDTO(), new ApiAutoPolicyWaiverDTO())));
  }

  @Test
  public void testAddAutoPolicyWaivers_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    assertThrows(UnauthorizedException.class, () -> apiAutoPolicyWaiverService.addAutoPolicyWaivers(
        OwnerType.APPLICATION,
        application.getId(),
        List.of(new ApiAutoPolicyWaiverDTO(), new ApiAutoPolicyWaiverDTO())));
  }

  @Test
  public void testAddAutoPolicyWaivers_Authorized() {
    Application application = tempEntity.newApplicationWithParent();
    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver1 = new ApiAutoPolicyWaiverDTO();
    apiAutoPolicyWaiver1.threatLevel = 8;
    apiAutoPolicyWaiver1.reachability = true;

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver2 = new ApiAutoPolicyWaiverDTO();
    apiAutoPolicyWaiver2.threatLevel = 8;
    apiAutoPolicyWaiver2.pathForward = true;

    grantPermission(application.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    apiAutoPolicyWaiverService.addAutoPolicyWaivers(
        OwnerType.APPLICATION,
        application.getId(),
        List.of(apiAutoPolicyWaiver1, apiAutoPolicyWaiver2));
  }

  @Test
  public void testUpdateAutoPolicyWaiver_Unauthenticated() {
    Application application = tempEntity.newApplicationWithParent();
    ApiAutoPolicyWaiverDTO dto =
        ApiAutoPolicyWaiverAdapter.convertToDTO(tempEntity.newAutoPolicyWaiver(application.getId()));
    assertThrows(UnauthenticatedException.class, () -> apiAutoPolicyWaiverService.updateAutoPolicyWaiver(
        OwnerType.APPLICATION, application.getId(),
        dto.autoPolicyWaiverId, dto));
  }

  @Test
  public void testUpdateAutoPolicyWaiver_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    ApiAutoPolicyWaiverDTO dto =
        ApiAutoPolicyWaiverAdapter.convertToDTO(tempEntity.newAutoPolicyWaiver(application.getId()));
    assertThrows(UnauthorizedException.class, () -> apiAutoPolicyWaiverService.updateAutoPolicyWaiver(
        OwnerType.APPLICATION, application.getId(),
        dto.autoPolicyWaiverId, dto));
  }

  @Test
  public void testUpdateAutoPolicyWaiver_Authorized() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.autoPolicyWaiverId = autoPolicyWaiver.getId();
    dto.reachability = true;
    dto.threatLevel = 10;
    grantWritePermission(application.getId());
    apiAutoPolicyWaiverService.updateAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
        dto.autoPolicyWaiverId, dto);
  }

  @Test
  public void testDeleteAutoPolicyWaiver_Unauthenticated() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    assertThrows(UnauthenticatedException.class, () -> apiAutoPolicyWaiverService.deleteAutoPolicyWaiver(
        OwnerType.APPLICATION, application.getId(),
        autoPolicyWaiver.getId()));
  }

  @Test
  public void testDeleteAutoPolicyWaiver_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    assertThrows(UnauthorizedException.class, () -> apiAutoPolicyWaiverService.deleteAutoPolicyWaiver(
        OwnerType.APPLICATION, application.getId(),
        autoPolicyWaiver.getId()));
  }

  @Test
  public void testDeleteAutoPolicyWaiver_Authorized() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    grantPermission(application.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    apiAutoPolicyWaiverService.deleteAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
        autoPolicyWaiver.getId());
  }

  @Test
  public void testGetAutoPolicyWaiverStatus_Unauthenticated() {
    Application application = tempEntity.newApplicationWithParent();
    assertThrows(UnauthenticatedException.class,
        () -> apiAutoPolicyWaiverService.getAutoPolicyWaiverStatus(OwnerType.APPLICATION, application.getId()));
  }

  @Test
  public void testGetAutoPolicyWaiverStatus_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    assertThrows(UnauthorizedException.class,
        () -> apiAutoPolicyWaiverService.getAutoPolicyWaiverStatus(OwnerType.APPLICATION, application.getId()));
  }

  @Test
  public void testGetAutoPolicyWaiverStatus_Authorized() {
    Application application = tempEntity.newApplicationWithParent();
    grantReadPermission(application.getId());
    apiAutoPolicyWaiverService.getAutoPolicyWaiverStatus(OwnerType.APPLICATION, application.getId());
  }

  @Test
  public void testGetApplicableAutoPolicyWaiverWithPermissionCheck_Unauthenticated() {
    Organization organization = tempEntity.newOrganization("test-org");
    Application application = tempEntity.newApplication(organization.getId());
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    assertThrows(UnauthenticatedException.class, () -> apiAutoPolicyWaiverService
        .getApplicableAutoPolicyWaiverWithPermissionCheck(autoPolicyWaiver.getId(), application));
  }

  @Test
  public void testGetApplicableAutoPolicyWaiverWithPermissionCheck_Unauthorized() {
    login();
    Organization organization = tempEntity.newOrganization("test-org");
    Application application = tempEntity.newApplication(organization.getId());
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    assertThrows(UnauthorizedException.class, () -> apiAutoPolicyWaiverService
        .getApplicableAutoPolicyWaiverWithPermissionCheck(autoPolicyWaiver.getId(), application));
  }

  @Test
  public void testGetApplicableAutoPolicyWaiverWithPermissionCheck_Authorized() {
    Organization organization = tempEntity.newOrganization("test-org");
    Application application = tempEntity.newApplication(organization.getId());
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    grantReadPermission(application.getId());

    apiAutoPolicyWaiverService.getApplicableAutoPolicyWaiverWithPermissionCheck(
        autoPolicyWaiver.getId(), application);
  }

  @Test
  public void testGetApplicableAutoWaivers_Unauthenticated() {
    Application application = tempEntity.newApplicationWithParent();
    assertThrows(UnauthenticatedException.class,
        () -> apiAutoPolicyWaiverService.getApplicableAutoWaivers(OwnerType.APPLICATION, application.getId()));
  }

  @Test
  public void testGetApplicableAutoWaivers_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    assertThrows(UnauthorizedException.class,
        () -> apiAutoPolicyWaiverService.getApplicableAutoWaivers(OwnerType.APPLICATION, application.getId()));
  }

  @Test
  public void testGetApplicableAutoWaivers_Authorized() {
    Application application = tempEntity.newApplicationWithParent();
    grantReadPermission(application.getId());
    apiAutoPolicyWaiverService.getApplicableAutoWaivers(OwnerType.APPLICATION, application.getId());
  }

  @Test
  public void testGetApplicableAutoPolicyWaivers_Unauthenticated() {
    final Organization org = tempEntity.newOrganization("test-org");
    final Application app = tempEntity.newApplication(org.getId());
    final PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-ID");
    final Policy policy = tempEntity.newPolicy(app.getId(), evaluation.getId());
    final PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, policy);

    assertThrows(UnauthenticatedException.class,
        () -> apiAutoPolicyWaiverService.getApplicableAutoPolicyWaiver(policyViolation.getId()));
  }

  @Test
  public void testGetApplicableAutoPolicyWaivers_Unauthorized() {
    login();
    final Organization org = tempEntity.newOrganization("test-org");
    final Application app = tempEntity.newApplication(org.getId());
    final PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-ID");
    final Policy policy = tempEntity.newPolicy(app.getId(), evaluation.getId());
    final PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, policy);

    assertThrows(UnauthorizedException.class,
        () -> apiAutoPolicyWaiverService.getApplicableAutoPolicyWaiver(policyViolation.getId()));
  }

  @Test
  public void testGetApplicableAutoPolicyWaivers_Authorized() {
    final Organization org = tempEntity.newOrganization("test-org");
    final Application app = tempEntity.newApplication(org.getId());
    final PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-ID");
    final Policy policy = tempEntity.newPolicy(app.getId(), evaluation.getId());
    final PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, policy);

    grantReadPermission(app.getId());

    assertThatCode(() -> apiAutoPolicyWaiverService.getApplicableAutoPolicyWaiver(policyViolation.getId()))
        .doesNotThrowAnyException();
  }
}
