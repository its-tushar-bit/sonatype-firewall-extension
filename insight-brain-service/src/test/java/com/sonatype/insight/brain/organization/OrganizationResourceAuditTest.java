/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationResourceAuditTest
    extends AbstractAuditTest
{
  private Organization organization;

  @Before
  public void before() {
    organization = tempEntity.newOrganization();
  }

  @Test
  public void testAddOrganization() throws Exception {
    Organization organization = new Organization("orgName");

    organization = organizationRequest().body(organization).post().getBody(Organization.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_ORGANIZATION, null);
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  public void testAddOrganization_Unauthorized() throws Exception {
    organizationRequest().body(new Organization()).with(unauthorizedUser()).post();

    assertAuditLog(AuditEvent.CREATE_ORGANIZATION, "unauthorized");
  }

  @Test
  public void testUpdateOrganization() throws Exception {
    organization.setName("updatedName");

    organizationRequest().body(organization).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_ORGANIZATION, null);
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  public void testUpdateOrganization_Unauthorized() throws Exception {
    organizationRequest().body(organization).with(unauthorizedUser()).put();

    assertAuditLog(AuditEvent.UPDATE_ORGANIZATION, "unauthorized");
  }

  @Test
  public void testDeleteOrganization_WithoutChildApplications() throws Exception {
    organizationRequest().path(OrganizationResource.DELETE_ORGANIZATION_PATH).parameter(organization.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_ORGANIZATION, null);
    assertOrganizationData(auditDTO, organization);

    assertThat(assertAuditLogs(AuditEvent.DELETE_APPLICATION, 0, null)).isEmpty();
  }

  @Test
  public void testDeleteOrganization_WithChildApplications() throws Exception {
    User contact = tempEntity.newUser();
    Application app1 = tempEntity
        .newApplication("appName1", "appPublicId1", organization.getId(), contact.getUsername());
    Application app2 = tempEntity
        .newApplication("appName2", "appPublicId2", organization.getId(), contact.getUsername());

    organizationRequest().path(OrganizationResource.DELETE_ORGANIZATION_PATH).parameter(organization.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_ORGANIZATION, null);
    assertOrganizationData(auditDTO, organization);

    List<AuditDTO> auditDTOs = assertAuditLogs(AuditEvent.DELETE_APPLICATION, 2, null);
    for (Application app : Arrays.asList(app1, app2)) {
      AuditDTO appAuditDTO = findFirstByDataKeyValue(auditDTOs, "applicationId", app.getId());
      assertApplicationData(appAuditDTO, app);
      assertCustomData(appAuditDTO, "contactUsername", contact.getUsername());
      assertParentOrganizationData(appAuditDTO, organization);
    }
  }

  @Test
  public void testDeleteOrganization_NLevel_WithoutChildApplications() throws Exception {
    List<Organization> testOrgs = tempEntity.newRelatedOrganizationsAsList(1, 7, 0);
    organizationRequest().path(OrganizationResource.DELETE_ORGANIZATION_PATH)
        .parameter(testOrgs.get(testOrgs.size() - 1).getId())
        .delete();

    List<AuditDTO> deletedOrgsAuditEvents = assertAuditLogs(AuditEvent.DELETE_ORGANIZATION, testOrgs.size(), null);
    for (Organization currentOrg : testOrgs) {
      AuditDTO orgAuditDTO = findFirstByDataKeyValue(deletedOrgsAuditEvents, "organizationId", currentOrg.getId());
      assertOrganizationData(orgAuditDTO, currentOrg);
      if (!Organization.ROOT_ORGANIZATION_ID.equals(currentOrg.getParentOrganizationId())) {
        Organization parentOrg =
            testOrgs.stream()
                .filter(org -> org.getId().equals(currentOrg.getParentOrganizationId()))
                .findFirst()
                .orElse(null);
        assertParentOrganizationData(orgAuditDTO, parentOrg);
      }
    }

    assertThat(assertAuditLogs(AuditEvent.DELETE_APPLICATION, 0, null)).isEmpty();
  }

  @Test
  public void testDeleteOrganization_NLevel_WithChildApplications() throws Exception {
    List<Organization> testOrgs = tempEntity.newRelatedOrganizationsAsList(organization, 1, 6, 0);
    List<Application> testApps = new LinkedList<>();
    testOrgs.add(organization);

    testOrgs.forEach(currentOrg -> testApps.add(tempEntity.newApplicationWithParent(currentOrg)));

    organizationRequest().path(OrganizationResource.DELETE_ORGANIZATION_PATH)
        .parameter(organization.getId())
        .delete();

    List<AuditDTO> deletedOrgsAuditEvents = assertAuditLogs(AuditEvent.DELETE_ORGANIZATION, testOrgs.size(), null);
    List<AuditDTO> deletedAppsAuditEvents = assertAuditLogs(AuditEvent.DELETE_APPLICATION, testApps.size(), null);
    for (int i = 0; i < testOrgs.size(); i++) {
      Organization currentOrg = testOrgs.get(i);
      Application currentApp = testApps.get(i);

      AuditDTO orgAuditDTO = findFirstByDataKeyValue(deletedOrgsAuditEvents, "organizationId", currentOrg.getId());
      assertOrganizationData(orgAuditDTO, currentOrg);
      if (!Organization.ROOT_ORGANIZATION_ID.equals(currentOrg.getParentOrganizationId())) {
        Organization parentOrg =
            testOrgs.stream()
                .filter(org -> org.getId().equals(currentOrg.getParentOrganizationId()))
                .findFirst()
                .orElse(null);
        assertParentOrganizationData(orgAuditDTO, parentOrg);
      }

      AuditDTO appAuditDTO = findFirstByDataKeyValue(deletedAppsAuditEvents, "applicationId", currentApp.getId());
      assertApplicationData(appAuditDTO, currentApp);
      assertParentOrganizationData(appAuditDTO, currentOrg);
    }
  }

  @Test
  public void testDeleteOrganization_Unauthorized() throws Exception {
    organizationRequest().path(OrganizationResource.DELETE_ORGANIZATION_PATH)
        .parameter(organization.getId())
        .with(unauthorizedUser())
        .delete();

    assertAuditLog(AuditEvent.DELETE_ORGANIZATION, "unauthorized");
  }

  @Test
  public void testSetIcon_Robot() throws Exception {
    organizationRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH)
        .parameter(organization.getId())
        .part("hasRobotSource", "true")
        .part("hashcode", "")
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ORGANIZATION_ICON, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "iconType", "robot");
  }

  @Test
  public void testSetIcon_File() throws Exception {
    String iconFilename = "defaulticon_application.png";

    organizationRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH)
        .parameter(organization.getId())
        .part("hasRobotSource", "false")
        .part("file", iconFilename, IconUtils.loadIconFromProductAssets("defaulticon_application.png"))
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ORGANIZATION_ICON, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "iconType", "file");
    assertCustomData(auditDTO, "iconFilename", iconFilename);
  }

  @Test
  public void testSetIcon_Default() throws Exception {
    organizationRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH)
        .parameter(organization.getId())
        .part("hasRobotSource", "false")
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ORGANIZATION_ICON, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "iconType", "default");
  }

  @Test
  public void testSetIcon_Unauthorized() throws Exception {
    organizationRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH)
        .parameter(organization.getId())
        .part("hasRobotSource", "false")
        .with(unauthorizedUser())
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ORGANIZATION_ICON, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  public void testMoveOrganizationErrorsExport() throws Exception {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();

    organizationRequest()
        .path(OrganizationResource.MOVE_ORGANIZATION_ERRORS_EXPORT_PATH)
        .parameter(organizations.get(0).getId(), organization.getId())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_MOVE_ORGANIZATION_ERRORS_LIST, null);
    assertOrganizationData(auditDTO, organizations.get(0));
  }

  @Test
  public void testMoveOrganizationErrorsExport_Unauthorized() throws Exception {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();

    organizationRequest()
        .path(OrganizationResource.MOVE_ORGANIZATION_ERRORS_EXPORT_PATH)
        .parameter(organizations.get(0).getId(), organization.getId())
        .with(unauthorizedUser())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_MOVE_ORGANIZATION_ERRORS_LIST, "unauthorized");
    assertOrganizationData(auditDTO, organizations.get(0));
  }

  private HttpRequest organizationRequest() {
    return restRequest().path(OrganizationResource.RESOURCE_PATH);
  }
}
