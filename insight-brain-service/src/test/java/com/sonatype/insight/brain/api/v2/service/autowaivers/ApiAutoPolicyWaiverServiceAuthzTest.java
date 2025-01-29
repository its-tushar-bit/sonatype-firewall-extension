/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.autowaivers;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverAdapter;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiAutoPolicyWaiverServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiAutoPolicyWaiverService apiAutoPolicyWaiverService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetAutoPolicyWaiver_Unauthenticated() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    apiAutoPolicyWaiverService.getAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
        autoPolicyWaiver.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAutoPolicyWaiver_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    apiAutoPolicyWaiverService.getAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
        autoPolicyWaiver.getId());
  }

  @Test
  public void testGetAutoPolicyWaiver_Authorized() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    grantReadPermission(application.getId());
    apiAutoPolicyWaiverService.getAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
        autoPolicyWaiver.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAutoPolicyWaivers_Unauthenticated() {
    Application application = tempEntity.newApplicationWithParent();
    apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.APPLICATION, application.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAutoPolicyWaivers_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.APPLICATION, application.getId());
  }

  @Test
  public void testGetAutoPolicyWaivers_Authorized() {
    Application application = tempEntity.newApplicationWithParent();
    grantReadPermission(application.getId());
    tempEntity.newAutoPolicyWaiver(application.getId());
    apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.APPLICATION, application.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddAutoPolicyWaiver_Unauthenticated() {
    Application application = tempEntity.newApplicationWithParent();
    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(), dto);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddAutoPolicyWaiver_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(), dto);
  }

  @Test
  public void testAddAutoPolicyWaiver_Authorized() {
    Application application = tempEntity.newApplicationWithParent();
    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.threatLevel = 8;
    dto.reachable = true;
    grantPermission(application.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(), dto);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateAutoPolicyWaiver_Unauthenticated() {
    Application application = tempEntity.newApplicationWithParent();
    ApiAutoPolicyWaiverDTO dto =
        ApiAutoPolicyWaiverAdapter.convertToDTO(tempEntity.newAutoPolicyWaiver(application.getId()));
    apiAutoPolicyWaiverService.updateAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
        dto.autoPolicyWaiverId, dto);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateAutoPolicyWaiver_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    ApiAutoPolicyWaiverDTO dto =
        ApiAutoPolicyWaiverAdapter.convertToDTO(tempEntity.newAutoPolicyWaiver(application.getId()));
    apiAutoPolicyWaiverService.updateAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
        dto.autoPolicyWaiverId, dto);
  }

  @Test
  public void testUpdateAutoPolicyWaiver_Authorized() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.autoPolicyWaiverId = autoPolicyWaiver.getId();
    dto.reachable = true;
    dto.threatLevel = 10;
    grantWritePermission(application.getId());
    apiAutoPolicyWaiverService.updateAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
        dto.autoPolicyWaiverId, dto);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteAutoPolicyWaiver_Unauthenticated() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    apiAutoPolicyWaiverService.deleteAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
        autoPolicyWaiver.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteAutoPolicyWaiver_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    apiAutoPolicyWaiverService.deleteAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
        autoPolicyWaiver.getId());
  }

  @Test
  public void testDeleteAutoPolicyWaiver_Authorized() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    grantPermission(application.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    apiAutoPolicyWaiverService.deleteAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
        autoPolicyWaiver.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAutoPolicyWaiverStatus_Unauthenticated() {
    Application application = tempEntity.newApplicationWithParent();
    apiAutoPolicyWaiverService.getAutoPolicyWaiverStatus(OwnerType.APPLICATION, application.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAutoPolicyWaiverStatus_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    apiAutoPolicyWaiverService.getAutoPolicyWaiverStatus(OwnerType.APPLICATION, application.getId());
  }

  @Test
  public void testGetAutoPolicyWaiverStatus_Authorized() {
    Application application = tempEntity.newApplicationWithParent();
    grantReadPermission(application.getId());
    apiAutoPolicyWaiverService.getAutoPolicyWaiverStatus(OwnerType.APPLICATION, application.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableAutoPolicyWaivers_Unauthenticated() {
    Organization organization = tempEntity.newOrganization("test-org");
    Application application = tempEntity.newApplication(organization.getId());
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    apiAutoPolicyWaiverService.getApplicableAutoPolicyWaiverWithPermissionCheck(autoPolicyWaiver.getId(), application);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableAutoPolicyWaivers_Unauthorized() {
    login();
    Organization organization = tempEntity.newOrganization("test-org");
    Application application = tempEntity.newApplication(organization.getId());
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    apiAutoPolicyWaiverService.getApplicableAutoPolicyWaiverWithPermissionCheck(autoPolicyWaiver.getId(), application);

  }

  @Test
  public void testGetApplicableAutoPolicyWaivers_Authorized() {
    Organization organization = tempEntity.newOrganization("test-org");
    Application application = tempEntity.newApplication(organization.getId());
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    grantReadPermission(application.getId());

    apiAutoPolicyWaiverService.getApplicableAutoPolicyWaiverWithPermissionCheck(
        autoPolicyWaiver.getId(), application);
  }
}
