/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.WaivedComponentUpgradeNotificationDTO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private OrganizationService organizationService;

  private final OrganizationDAO organizationDAO = new OrganizationDAO();

  private String waivedComponentUpgradeStageTypeId;

  @Before
  public void before() {
    // Capture the original root org waived component upgrade stage id, so we can restore it after the tests.
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    waivedComponentUpgradeStageTypeId = rootOrg.getWaivedComponentUpgradeStageTypeId();
  }

  @After
  public void restoreRootOrganizationState() {
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    rootOrg.setWaivedComponentUpgradeStageTypeId(waivedComponentUpgradeStageTypeId);
    organizationDAO.update(rootOrg);
  }

  @Test
  public void testGetAll_Authorized() {
    grantReadPermission(org.getId());

    final List<Organization> organizations = organizationService.getAll();
    assertThat(organizations).hasSize(1);
    final Organization organization = organizations.get(0);
    assertThat(organization.getId()).isEqualTo(org.getId());
    assertThat(organization.getName()).isEqualTo(org.getName());
  }

  @Test
  public void testGetAll_Unauthorized() {
    login();
    final List<Organization> organizations = organizationService.getAll();
    assertThat(organizations).isEmpty();
  }

  @Test
  public void testGetAll_Unauthenticated() {
    final List<Organization> organizations = organizationService.getAll();
    assertThat(organizations).isEmpty();
  }

  @Test
  public void testAddOrganization_Authorized() {
    grantWritePermission(Organization.ROOT_ORGANIZATION_ID);

    Organization orgToAdd = new Organization("MyOrg");
    final Organization addedOrg = organizationService.addOrganization(orgToAdd);
    tempEntity.register(addedOrg);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddOrganization_Unauthenticated() {
    Organization orgToAdd = new Organization("MyOrg");

    organizationService.addOrganization(orgToAdd);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddOrganization_Unauthorized() {
    login();
    Organization orgToAdd = new Organization("MyOrg");

    organizationService.addOrganization(orgToAdd);
  }

  @Test
  public void testUpdateOrganization_Authorized() {
    grantWritePermission(org.getId());

    organizationService.updateOrganization(org);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateOrganization_Unauthenticated() {
    organizationService.updateOrganization(org);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateOrganization_Unauthorized() {
    login();
    organizationService.updateOrganization(org);
  }

  @Test
  public void testDeleteOrganization_Authorized() throws Exception {
    grantWritePermission(org.getId());

    organizationService.deleteOrganization(org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteOrganization_Unauthenticated() throws Exception {
    organizationService.deleteOrganization(org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteOrganization_Unauthorized() throws Exception {
    login();
    organizationService.deleteOrganization(org.getId());
  }

  @Test
  public void testGetOrganization_Authorized() {
    grantReadPermission(org.getId());

    Organization organization = organizationService.getOrganization(org.getId());
    assertThat(organization).isNotNull();
    assertThat(organization.getId()).isEqualTo(org.getId());
    assertThat(organization.getName()).isEqualTo(org.getName());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetOrganization_Unauthorized() {
    login();
    organizationService.getOrganization(org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetOrganization_Unauthenticated() {
    organizationService.getOrganization(org.getId());
  }

  @Test
  public void testUpdatePolicyWaiverUpgradePathAvailableNotification_Authorized() {
    grantWritePermission(Organization.ROOT_ORGANIZATION_ID);
    WaivedComponentUpgradeNotificationDTO waivedComponentUpgradeNotificationDTO =
        new WaivedComponentUpgradeNotificationDTO();
    waivedComponentUpgradeNotificationDTO.setStage(Stage.ID_DEVELOP);
    organizationService.updateWaivedComponentUpgradeNotification(
        waivedComponentUpgradeNotificationDTO);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdatePolicyWaiverUpgradePathAvailableNotification_Unauthorized() {
    login();
    WaivedComponentUpgradeNotificationDTO waivedComponentUpgradeNotificationDTO =
        new WaivedComponentUpgradeNotificationDTO();
    waivedComponentUpgradeNotificationDTO.setStage(Stage.ID_DEVELOP);

    organizationService.updateWaivedComponentUpgradeNotification(
        waivedComponentUpgradeNotificationDTO);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdatePolicyWaiverUpgradePathAvailableNotification_Unauthenticated() {
    WaivedComponentUpgradeNotificationDTO waivedComponentUpgradeNotificationDTO =
        new WaivedComponentUpgradeNotificationDTO();
    waivedComponentUpgradeNotificationDTO.setStage(Stage.ID_DEVELOP);

    organizationService.updateWaivedComponentUpgradeNotification(
        waivedComponentUpgradeNotificationDTO);
  }

  @Test
  public void testGetPolicyWaiverUpgradePathAvailableNotification_Authorized() {
    grantReadPermission(Organization.ROOT_ORGANIZATION_ID);

    WaivedComponentUpgradeNotificationDTO waivedComponentUpgradeNotificationDTO =
        organizationService.getWaivedComponentUpgradeNotification();
    assertThat(waivedComponentUpgradeNotificationDTO).isNotNull();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyWaiverUpgradePathAvailableNotification_Unauthorized() {
    login();
    organizationService.getWaivedComponentUpgradeNotification();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyWaiverUpgradePathAvailableNotification_Unauthenticated() {
    organizationService.getWaivedComponentUpgradeNotification();
  }
}
